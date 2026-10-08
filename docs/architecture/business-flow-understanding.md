# 品达TMS 业务流程体检报告（订单 → 运单 → 调度 → 作业 → 签收 → 轨迹 → 结算）

产出方式：`architecture-visualization:explore` 路由到 `flow-visualizer`（主干流程与状态机）+ `system-modeler`（服务边界与数据归属），图源用 `graphviz` DOT。
证据基线：本机仓库 `D:\MyCode\pinda-tms` @ `ecd6725` + 内网实测机 `192.168.20.130` 的 MySQL/Nacos 运行时读取（2026-10-08）。
阅读顺序：先看「一句话结论」和「主干能不能跑通」→ 再看三张断点表 → 需要图就看同目录 `business-flow.dot` / `order-lifecycle.dot` / `data-ownership.dot`。

## 一句话结论

**结构齐、血流不通。** 七段链路里只有「下单→揽收→入库→调度→运输→派送→签收」的**代码路径**基本齐全，但端到端**一次都没有真跑通过**（线上 3 条订单和 3 张运单全部停在 `status=1`，作业/取派/轨迹表 0 行）；而让它们跑不通的原因不是缺功能，是三类接缝坏了：**状态值与状态机对不上、跨服务用影子表副本代替调用、财务与告警写进去之后没人读也建不出表**。

## 运行时事实（决定后面所有结论的可信度）

| 事实 | 证据 |
| --- | --- |
| 2026-10-07 当时订单 3 条、运单 3 张，**全部停在 `status=1`**；**2026-10-08 复测已归零** | 10-07 实测 `pd_oms.pd_order`=3、`pd_work.pd_transport_order`=3（`status=1` 各 3）；10-08 复测 `pd_oms.pd_order`=0、`pd_work.pd_transport_order`=0、`pinda_tms.pd_order`=0。同期 `pd_auth_user`=13、`pd_truck_driver`=10、`pd_member`=3、`pd_area`=44703 **都没变**，只有订单侧空了。服务器 `@@log_bin=0`，没有可追溯的删除记录，**因此无法断定是谁在什么时候删的** |
| 取派任务、司机作业、轨迹、结算 全 0 行 | `pd_work.pd_task_pickup_dispatch`=0、`pd_driver_job`=0、`pd_oms.pd_truck_location`=0 |
| 快递员列表接口实测返回 0 行 | 网关 `GET /api/web-manager/business-hall/courier/page` → `{"counts":0,...}`，而 `会员分页` counts=3 |

含义：**任何"业务流程正常"的判断都不能只看有没有报错日志**——主干从未被数据走完过一次，很多问题只会在第一条真实业务数据上暴露。

## 断点一：订单状态机与数据/自身实现三处不一致（最高优先级）

`OrderStatus` 是 5 位码 `23000…23011`（`pd-service-api/pd-service-oms-domain/.../enums/OrderStatus.java:13-71`），而流转校验这样写：

```java
// pd-oms/.../OrderServiceImpl.java:113-120
Set<Integer> allowedTransitions = ORDER_STATUS_TRANSITIONS.get(existing.getStatus());
if (allowedTransitions == null || !allowedTransitions.contains(newStatus)) {
    log.error("订单[{}]状态流转非法…"); return false;      // 只记日志，返回 false
}
```

| # | 问题 | 证据 | 后果 |
| --- | --- | --- | --- |
| 1.1 | **种子数据的状态值 `1` 不在状态机键集内** | `docs/sql/testdata.sql:84-90` 插 `status` 为 `1`；`OrderStatus.java:13` 起为 `23000+` | 这 3 条单**永远流转不动**：任何改状态调用命中 `allowedTransitions == null → return false` |
| 1.2 | 失败被降级成"看起来没事" | `pd-oms/.../OrderController.java:152-155` 返回 `null`（HTTP 仍 200） | 调用方（多为 Feign）拿到空体，前端显示成功或 NPE，**没人知道状态没变** |
| 1.3 | 同一张流转图存了两份 | `pd-oms/.../OrderServiceImpl.java:52-86` 内联 map，注释第 48 行自称"与 pd-work 的 StateTransitionValidator 保持一致"；另一份在 `pd-work/.../state/StateTransitionValidator.java:28` | 两处靠人肉同步，必然漂移（取证员把 pd-work 那份判成死代码，实际它被 `TaskTransportServiceImpl`、`TransportOrderServiceImpl` 使用且有单测——**这种"两份定义"正是误判根源**） |
| 1.4 | 有进无出的状态：`23007 待派送` 无生产者 | 快递员接件直接写 `23008`（`pd-web-courier/.../CourierController.java:447`） | 「待派送」这一环节不可观测，看板/漏斗少一级 |
| 1.5 | `23002 网点自寄` 无揽收入口 | 只能靠 `CourierController.warehousing:401` 扫运单交件推进；运单在 `MailingController:242-246` 预生成但未判空 | 运单缺失时自寄单永久卡在 23002（推断，需真实单验证） |
| 1.6 | 终态之后无人消费 | `23010 拒收` 的回写 MQ 监听整段被注释：`pd-dispatch/.../OrderEventMQListener.java:149-164` | 拒收不影响任何下游，退款/回库不闭环 |

作业层同类问题（同属"状态机不连贯"）：`TransportTaskStatus` 的 `3 待确认`、`5 已取消`，`DriverJobStatus` 的 `3 改派`、`5 作废`，`TransportOrderSchedulingStatus` 的 `2 未匹配线路`、`3 已调度` —— 均**无生产者**（枚举有值、代码从不写）；运单完成路径实际是 `2 → 4`，跳过 `3`。

## 断点二：服务边界靠"影子表"糊过去，数据归属三权分立

线上库实测：一个业务对象同时存在于 2-3 个库，且副本会空会漂：

| 对象 | 存在的库 | 实测漂移 |
| --- | --- | --- |
| `pd_order` | `pd_oms`（主）、`pd_aggregation`（副本）、`pinda_tms`（遗留全量） | 副本 **0 行** vs 主 3 行 |
| `pd_core_org` | `pd_auth`（主）、`pd_aggregation`（副本）、`pinda_tms` | 副本 **0 行** vs 主 15 行 |
| `pd_area` | `pd_auth`、`pd_aggregation` | 44703 = 44703（暂时一致，靠脚本灌） |
| `pd_transport_order` / `pd_task_pickup_dispatch` / `pd_auth_user` | 同上多库并存 | — |

| # | 问题 | 证据 | 后果 |
| --- | --- | --- | --- |
| 2.1 | 聚合层不查 owner 服务，而是 **JOIN 本库副本** | `pd-aggregation/src/main/resources/mapper/CourierMapper.xml:6-9`、`WebManagerMapper.xml:32-35` JOIN `pd_order`/`pd_transport_order`/`pd_task_pickup_dispatch`/`pd_auth_user`，**无库名前缀**（即读 `pd_aggregation` 自己的同名表） | 副本为空 → 列表接口稳定返回 0 行（已实测 `counts=0`）。这类"数据没有"会被当成业务问题查半天 |
| 2.2 | 写只走 Feign、读靠共享库：读写一致性规则不统一 | 全仓未见跨库 `UPDATE/INSERT` 前缀；跨服务写经 Feign（如 `TruckFeign` + `TruckFeignFallback`） | 读侧脏/空、写侧降级（fallback 静默返回空）叠加 → 出错时"看起来成功" |
| 2.3 | 轨迹归属漂移：`pd-netty` 连 `pd_oms`，`pd-truck_location` 落在订单库；`pd-druid` 也连 `pd_oms` | Nacos `pd-netty-prod.yml`、`pd-druid-prod.yml` 的 `jdbc:mysql://…/pd_oms`（实测） | 轨迹数据无独立归属；订单库同时承担 3 个域，容量/备份/权限都纠缠 |
| 2.4 | 遗留全量库 `pinda_tms`（65 表，含 `pd_payment_order`）仍在线上 | 实测 `information_schema` 查询 | 没有服务连它，但同名对象易误查误改（我今天差点把 `pd_payment_order` 判成"已建"） |
| 2.5 | 无软删/审计/租户隔离 | 线上 `pd_oms.pd_order` 列清单：无 `deleted`、无 `update_time`、无 `tenant`；全仓无 `@TableLogic` | 误删即物理删除，多 org 数据不隔离（`current_agency_id` 仅存不用） |

## 断点三：签收之后（轨迹、告警、结算）是"写了没人读、读了表不存在"

| # | 现象 | 证据 | 判定 |
| --- | --- | --- | --- |
| 3.1 | 上报入口其实齐（HTTP `POST /netty/push` + TCP `NettyServer` 8194），**生产端不缺** | `pd-netty/.../controller/NettyController.java:44,73`；`config/NettyServer.java:73`、`service/NettyServerHandler.java:97` → `KafkaSender.java:30` | 已证实（修正"生产端缺失"的猜测） |
| 3.2 | 消费→落库→归档链路本身是通的 | `listener/GpsTraceConsumer.java:248,294,296`；归档 `LocationRecordServiceImpl.java:55,57` 每 1h 清 30 天前 | 已证实；今天 200 条压测 200 条落库、0 丢弃 |
| 3.3 | **`pd-druid` 把 Kafka topic 当表查** | `pd-druid/.../DruidServiceImpl.java:45,63,80` `FROM tms_order_location`，列名还写 `currentTime`（真实列已改 `report_time`）；实测该表在任何库都不存在 | 车辆位置/大屏接口必坏（推断：500 或空数据） |
| 3.4 | 告警只进日志，不可追溯 | `service/GpsAlertService.java:55` 仅 `log.error`；webhook 取值 `bootstrap-dev.yml:57` = `${GPS_ALERT_WEBHOOK_URL:}` 默认为空；全仓无告警表/接口/页面 | 今天压测触发 89 条超速告警，全部只落日志 |
| 3.5 | 签收→结算有代码，但**表不在它连的库里** | `OrderServiceImpl.java:127-129` 签收即 `settlementService.settle(...)`；`SettlementServiceImpl.java:58` `REQUIRES_NEW` 写 `pd_settlement_order`+`pd_freight_detail`；实测 `pd_oms` 无这两张表（只在遗留 `pinda_tms` 有 `pd_payment_order`） | 结算必失败，且被 `REQUIRES_NEW`+吞异常设计成"不影响签收" → **静默不结算** |
| 3.6 | 结算只写不读 | 全仓无 Settlement/FreightDetail 的 Controller；账单/账期/发票/承运商仅在建表脚本或文档里 | 财务域是"半闭环 + 空壳" |
| 3.7 | 取消不回滚运单、不退款 | `MailingController.cancel:735-763` 只改订单+取派任务；退款只有人工入口 `PayController:98-104` | 已付订单取消后运单仍在、钱不退 |
| 3.8 | 调度只能定时触发 | `pd-dispatch/.../DispatchTask.java:33` 由 Quartz 反射驱动（`ScheduleJob:60`）；事件触发入口 `OrderEventListener:85`、`OrderEventMQListener:108` **整段注释** | 无按需调度，业务节奏受 cron 间隔支配 |
| 3.9 | 新环境迁移脚本引用不存在的文件 | `deploy/server/import-data.sh:36-42` 引 `docs/sql/V1.0…V1.6__*.sql`，实际目录里是另一套中文/业务命名文件 | 按 `imp_opt`（可选）静默跳过 → 表结构漂移 |

## 断点四：鉴权与降级的设计漏洞，会让"业务流程看起来跑通了"变成假象

这一段是复核 `flow-boundaries` 取证时**自己逐条验过**的（取证员另有两处误判，见文末）：

| # | 问题 | 证据 | 后果 |
| --- | --- | --- | --- |
| 4.1 | 下游只信任 `userid` 头，而端口全部发布到宿主 | `pd-common/.../TokenAuthInterceptor.java:24,47-48` 只判头存在（设计前提是网关已验 JWT）；`deploy/apps/docker-compose.app.yml` 发布 8161-8164/8191/8193 | 能连内网的人**伪造 `userid: 1` 即绕过全部业务鉴权**，多网点数据隔离形同不存在 |
| 4.2 | 网关资源清单降级路径不安全：先 NPE，再有一分支是放行 | `ResourceApiFallback.list()` `return null` → `AccessFilter.java:73` 直接 `.getData()`；`AccessFilter.java:78` 为 null 时**整段鉴权跳过** | 权限服务不可用时，行为在"大面积 500"和"放行未授权请求"之间摇摆 |
| 4.3 | 几乎所有 Feign 写操作降级返回 `null` | `TruckFeignFallback.java:24,54`、`OrderFeignFallback`、`TransportOrderFeignFallback:22,28`、`OrgApiFallback:26,31` | 跨服务写失败仍返回 HTTP 200，**流程"成功"但数据没落**，正是最难查的那类假成功 |
| 4.4 | 转发中心 77 处跨服务调用、0 个事务注解 | `pd-web-manager/.../TransforCenterBusinessController.java`（计数实测） | 中转动作半成功 → 订单/运单/任务三态互不一致 |
| 4.5 | POD 附件上传指向未部署的服务 | `pd-web-courier/.../AttachmentClient.java:18` 默认 `pd-file-server`；仓库无覆写、compose 无该服务 | 快递签收缺照片证据链（业务与合规都要它） |
| 4.6 | 身份取自请求体而非网关注入头，事务号可被客户端塞入 | `pd-aggregation/.../AppCourierController.java:25` 用 body 的 `courierId`；`TxXidFilter.java:24` 接受外部 `TX_XID` | 冒充他人查改；伪造全局事务上下文可能污染提交/回滚决策 |

另：`AppDriverFeign.java:12` 全仓无调用方（死契约，可删）。

## 数据与事务边界小结

- `@GlobalTransactional` 只有 5 处：`DispatchTask.java:33`、`pd-web-driver/CargoController.java:408,531`、`pd-web-customer/MailingController.java:178`、`pd-web-courier/CourierController.java:278`。
- **多实体裸写**（无事务）：妥投 `delivered:479`、交件 `warehousing:392`、交接 `handover:433`、签收状态写 `CourierController:505` —— 半写风险最高的一段恰恰是签收。
- `undo_log` 在 8 个库都有（Seata 已铺好），所以补事务注解的成本低，只是没人用。
- `pd_order_cargo.tran_order_id` **只有置 null 的写入点**（`OrderController.java:97`），而查询以它过滤（`OrderCargoServiceImpl.java:36-37`）→ 按运单查货物恒空。
- 主键：`pd-oms/CustomIdGenerator.java:11` 机器位硬编码 `IdWorker(1,1)`（多实例会撞号，推断）。

## 未知与假设（不要当成事实）

| 未知项 | 为什么还不知道 | 怎么验 |
| --- | --- | --- |
| 真实业务单能否走完 23000→23009 | 线上只有 `status=1` 的种子单 | 用一条**新单**跑 `揽收→入库→调度→发车→派送→签收`，每步 `select status` |
| TCP 8194 是否真在监听 | 容器无 `netstat/ss`，日志不表态 | 从宿主 `nc -zv 127.0.0.1 8194`；或让设备真发一行 JSON |
| `23002 网点自寄` 是否卡过单 | 需真实自寄单样本 | 查 `pd_order` 里 `pickup_type=1` 的历史单状态分布 |
| 结算是否曾成功写入 | 目标库根本没表 | 建表后触发一次签收，看 `pd_settlement_order` 是否落行 |
| `pd_aggregation` 副本靠什么同步 | 未见同步任务/触发器 | 找 `pd_area` 灌库脚本与定时任务，确认是否有作业在同步 `pd_order` 副本 |

## 建议动作顺序（都带证据依据，不含猜测性重构）

1. **先修接缝，再加功能**：把种子/历史脏状态归一到 `23000+`（或让状态机兼容 1→23000 映射），并把 `return false` 改成显式业务错误（`OrderController.java:152`），否则后续一切验证都被假成功误导。
2. **状态流转图收成一份**（`pd-work` 那份已有单测，建议以它为准，pd-oms 改为引用），并清掉"无生产者"状态或补上生产路径。
3. **签收/交件/妥投三处补 `@GlobalTransactional`**（Seata 与 undo_log 已就位），避免半写。
4. **数据归属定案**：`pd_order`/`pd_core_org` 这类只允许 owner 服务读，聚合层改为 Feign 或显式视图；`pinda_tms` 与 `pd_aggregation` 影子副本要么有同步机制、要么删表（删前先确认无引用）。
5. **财务/告警要么闭环要么明确标注未实现**：结算表按 `docs/sql/财务结算域_建表.sql` 落到 `pd_oms` 并补查询接口；告警落库 + 前端可见；`pd-druid` 改成查 `pd_truck_location`（列名 `report_time`）。
6. 顺手修 `deploy/server/import-data.sh` 的 7 个失效引用，避免新环境静默缺表。
7. **降级不许返回 null**：`ResourceApiFallback`、`TruckFeignFallback`、`OrderFeignFallback`、`TransportOrderFeignFallback`、`OrgApiFallback` 统一改为返回明确失败码；`AccessFilter.java:73` 加判空，权限源不可用时的策略要显式（建议 fail-closed 返回 503），并写一条断源测试。
8. **堵住越权入口**：下游服务不再暴露宿主端口（或本地校验 JWT），`AppCourierController` 等改为从 `userid` 头取身份，`TX_XID` 只接受内网来源；补 `pd-file-server`（或显式覆写 `pinda.feign.authority-server`）让 POD 附件真正可落。

## 附：服务 → 库 → 拥有的对象（实测 + 代码）

| 服务 | 库 | 主要对象 | 备注 |
| --- | --- | --- | --- |
| pd-oms | `pd_oms` | `pd_order`、`pd_order_cargo`、`pd_order_location`、`rule`（+ 代码声明但**库里没有**：`pd_payment_order`/`pd_settlement_order`/`pd_freight_detail`） | 订单 owner |
| pd-work | `pd_work` | `pd_transport_order(_task)`、`pd_task_transport`、`pd_driver_job`、`pd_task_pickup_dispatch`、`pd_status_transition_history` | 运输作业 owner |
| pd-dispatch | `pd_dispatch` | `pd_order_classify*`、`pd_cache_line*`、`pd_schedule_job(_log)` + Quartz 表 | 调度 owner |
| pd-base | `pd_base` | `pd_truck(_license/_type/_driver)`、`pd_fleet`、`pd_transport_line*`、`pd_trips`、`pd_goods_type`、`pd_agency_scope` | 主数据 owner（还寄养 `d_tenant`/`d_global_user` 两张租户表） |
| pd-user | `pd_users` | `pd_member`、`pd_address_book` | 会员 owner |
| pd-auth-server | `pd_auth` | `pd_auth_user/role/resource/menu`、`pd_core_org`、`pd_core_station`、`pd_area`、登录/操作日志 | 权限与组织 owner（实测 `pd_auth_resource` 383 行） |
| pd-netty | **`pd_oms`** | `pd_truck_location(_archive)`；另经 HTTP 回写 `pd_base.pd_truck` 心跳 | 轨迹没有自己的库，落在订单库 |
| pd-aggregation | `pd_aggregation` | 12 张**只读副本**（`pd_order`/`pd_transport_order`/`pd_task_*`/`pd_auth_user`/`pd_core_org`/`pd_core_station`/`pd_area`/`pd_truck`/`pd_fleet`…），mapper 内 0 条 insert/update | 副本无应用写入方，靠脚本灌 → 已见 `pd_order`、`pd_core_org` 为 0 行 |
| pd-druid | 由 `${spring.datasource.url}` 注入（实测指向 `pd_oms`） | 无实体，裸 JDBC 查**不存在的** `tms_order_location` | 名字像监控，实际是车辆位置/大屏查询服务 |
| pd-web-manager / driver / courier / customer | 无数据源 | 纯 Feign 编排 + `TokenAuthInterceptor` | 接入层，边界正确 |
| pd-gateway | 无数据源 | 路由 + `AccessFilter` | 鉴权单点，见断点四 |

## 附：本次审查的证据边界

- 四路取证（订单域 / 调度作业域 / 轨迹财务 / 边界归属）+ 我自己的线上实测（MySQL `information_schema`、Nacos dataId、网关接口实调、容器配额）交叉；**取证员的三处结论被我推翻并已在 `flow-risks-and-gaps.md` 记录**（`pd_area` 副本行数、`pd_auth.pd_area` 是否存在、`StateTransitionValidator` 是否死代码）。
- 未做真实业务单的端到端走通（线上只有 `status=1` 的种子数据），因此"能不能跑通"的结论建立在代码路径 + 数据可达性上，不等于运行时验证通过。

## 附：图文件

- `business-flow.dot`：端到端业务流（角色/动作/结果，坏接缝与未知节点显式标注）
- `order-lifecycle.dot`：订单状态机（可流转边、孤儿状态、种子值落点）
- `data-ownership.dot`：服务 ↔ 库 ↔ 影子副本 ↔ 缺失表
- `flow-risks-and-gaps.md`：断点清单（严重度/证据/验证方式，可当回归清单用）
