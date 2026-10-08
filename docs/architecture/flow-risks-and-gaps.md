# 业务流程断点与风险清单（可当回归清单用）

严重度：**P0 = 业务走不通/数据错**；**P1 = 会静默失败或半写**；**P2 = 可观测性/可维护性**。
"验证"列是给下一次回归的具体动作，不做完不许标完成。

| ID | 严重度 | 断点 | 证据（file:line / 实测） | 业务影响 | 验证方式 |
| --- | --- | --- | --- | --- | --- |
| B-01 | P0 | 订单种子/历史状态值 `1` 不在状态机键集（23000+），任何流转都失败 | `docs/sql/testdata.sql:84-90`；`OrderStatus.java:13-71`；`OrderServiceImpl.java:113-120`；线上 `pd_oms.pd_order.status=1 rows=3` | 存量单永远停在假状态，取派/调度/结算全部无从触发 | 用新建单跑完整链路，逐步 `select status`；或先把脏值修正为 23000 |
| B-02 | P0 | 状态流转失败被吞成"成功" | `OrderController.java:152-155` 返回 `null`，HTTP 仍 200 | 前端/Feign 调用方以为改成功，数据实际没动 | 拿脏状态单调 `PUT /oms/order/{id}`，断言应返回业务错误码 |
| B-03 | P0 | 签收触发的结算写入目标库无表 | `OrderServiceImpl.java:127-129`→`SettlementServiceImpl.java:58`；建表脚本 `docs/sql/财务结算域_建表.sql:26,44`；实测 `pd_oms` 无 `pd_settlement_order`/`pd_freight_detail` | 签收"成功"但永远不产生结算单，财务对账无源 | 建表后签收一单，查 `pd_settlement_order` 是否落行 |
| B-04 | P0 | 聚合层读影子副本，副本为空 | `CourierMapper.xml:6-9`、`WebManagerMapper.xml:32-35`；实测 `pd_aggregation.pd_order=0`（`pd_oms.pd_order=3`）、`pd_core_org=0`（`pd_auth=15`）；网关实测 `business-hall/courier/page → counts:0` | 管理端/网点列表"莫名空数据"，被误判成权限或前端问题 | 副本同步或改走 Feign 后，比对同一接口行数与主库一致 |
| B-05 | P0 | `pd-druid` 把 Kafka topic 当表查且列名过期 | `DruidServiceImpl.java:31,45,63,80`（`FROM tms_order_location`、`currentTime`）；实测该表在任何库都不存在，真实列为 `pd_oms.pd_truck_location.report_time` | 车辆位置/大屏接口必然 500 或空 | 改查 `pd_truck_location` 后调接口，应返回刚灌入的轨迹点 |
| B-06 | P1 | 签收/拒收多实体裸写无事务 | `CourierController.java:505`（订单+取派+运单三写）；对照 `@GlobalTransactional` 仅 5 处；`warehousing:392`、`handover:433`、`delivered:479` 同样裸写 | 中途失败即半写：订单已签收但任务未闭环 | 加事务后人为注入一次失败（改错 Feign 地址），断言三表要么全改要么全不改 |
| B-07 | P1 | 取消不回滚运单、已付不退款 | `MailingController.java:735-763`（仅订单+取派任务）；退款只有人工入口 `PayController.java:98-104` | 取消后运单仍待调度、资金挂账 | 造一条已付单取消，查运单状态与 `pd_payment_order` 流水 |
| B-08 | P1 | 调度只能靠 Quartz，事件触发入口被注释 | `DispatchTask.java:33`、`ScheduleJob:60`；`OrderEventListener.java:85`、`OrderEventMQListener.java:108` 注释 | 无按需调度；批次间隔决定业务时效，客户催单无法即时响应 | 决定保留哪种：留 cron 则文档写明周期；要即时则恢复事件触发并验证入队即调度 |
| B-09 | P1 | 订单↔货物按运单查询恒空 | 唯一写入点是置 null：`OrderController.java:97`；查询按它过滤：`OrderCargoServiceImpl.java:36-37` | 按运单看货物明细永远空，运单作业单缺货 | 建单/装车时真写 `tran_order_id`，再查接口应非空 |
| B-10 | P1 | 状态流转图两份定义 | `OrderServiceImpl.java:48,52-86` 注释自称"与 pd-work 保持一致"；另一份 `pd-work/state/StateTransitionValidator.java:28`（被 `TaskTransportServiceImpl`、`TransportOrderServiceImpl` 使用并有单测） | 只改一处即漂移；本次取证就因此误判过一次"死代码" | 收敛为单一来源（建议保留有单测那份），另一处删除或引用 |
| B-11 | P1 | 无软删/审计/租户隔离 | 线上 `pd_order` 列清单无 `deleted`/`update_time`/`org` 隔离列；全仓无 `@TableLogic`；`OIS:156-229` 查询不过滤 | 误删不可恢复；多网点数据互相可见；无法追溯改动 | 补 `deleted`+`@TableLogic` 与 `update_time`，列表接口加 org 条件后做越权用例 |
| B-12 | P2 | 23007 待派送无生产者；`TransportTaskStatus 3/5`、`DriverJobStatus 3/5`、`SchedulingStatus 2/3` 无生产者/无消费者 | `CourierController.java:447` 直接写 23008；枚举定义处无对应 set | 看板少一级、"已调度/改派/作废"筛选永远 0 行，运营口径失真 | 要么补生产路径，要么删状态并在文档标注不可用 |
| B-13 | P2 | 告警不可追溯 | `GpsAlertService.java:55` 仅 `log.error`；`bootstrap-dev.yml:57` webhook 默认为空；无告警表/接口/页面；今天压测触发 89 条告警全部只进日志 | 超速/停留/偏航事后无从追查（合规风险） | 告警落库 + 管理端页面，或至少接通 webhook 并验证收到 |
| B-14 | P2 | 归档表只写不读 | `pd_oms.pd_truck_location_archive`（今天建）存在，查询接口 `GpsTraceController:50,75,102,129` 只查主表；前端 `GpsTrace.js:8-10` 未接 byTasks | 超 30 天历史轨迹无法回看 | 加归档查询入口或明确标注"历史不可查" |
| B-15 | P2 | 迁移脚本引用不存在的文件 | `deploy/server/import-data.sh:36-42` 引 `docs/sql/V1.0…V1.6__*.sql`（实际目录无这些文件名）；`imp_opt` 语义为静默跳过 | 新环境按脚本导入会静默缺结构，故障复现困难 | 修引用为真实文件名后，在空库跑一次并断言表齐全 |
| B-16 | P2 | `pinda_tms` 遗留全量库（65 表）仍在线，含与业务库同名对象 | 实测 `information_schema`：`pinda_tms.pd_payment_order` 等；无任何服务数据源指向它 | 查错库/改错表的高风险源（本次审查就差点把支付表判成"已建"） | 确认无用后改名归档（如 `pinda_tms_retired`），不要直接删 |
| B-17 | P0 | Feign 写操作降级 **返回 null，调用方拿到"成功"** | `TruckFeignFallback.java:24,54` save/update `return null`；`OrderFeignFallback`、`TransportOrderFeignFallback:22,28`、`OrgApiFallback:26,31` 同类（后者返回空列表） | 车辆心跳/机构/订单写失败仍 HTTP 200；下拉与列表变空被当正常数据 | 让 fallback 返回明确失败码；单测断言降级路径不得返回 null |
| B-18 | P0 | 网关资源清单降级路径不安全：先 NPE、且存在判空即跳过的 fail-open 分支 | `ResourceApiFallback.list()` `return null` → `AccessFilter.java:73` 直接 `.getData()`（权限服务不可用时 500）；`AccessFilter.java:78` `if (resourceNeed2Auth != null)` 为假时**整段鉴权被跳过** | 鉴权行为随降级摇摆：要么大面积 500，要么放行未授权请求 | 断源测试：停 pd-auth-server 后调需鉴权接口，断言固定返回 401/503 而非 200 |
| B-19 | P0 | 下游服务信任 `userid` 头，但端口直接发布到宿主 | `pd-common/.../TokenAuthInterceptor.java:24,47-48` 仅判头是否存在（设计前提是网关已校验 JWT 并注入）；`deploy/apps/docker-compose.app.yml` 把 8161-8164/8191/8193 全部 `-p` 发布 | 任何能连 192.168.20.130 的人**伪造 `userid: 1` 即绕过全部业务鉴权**（数据隔离与权限体系整体失效） | 端口收进内网网段/不发布，或下游本地校验 JWT；验证：不带 token 直连 8162 应拒绝 |
| B-20 | P1 | 转发中心跨服务写 77 处、事务注解 0 个 | `pd-web-manager/.../TransforCenterBusinessController.java`：`Feign` 引用计数 77，`@Transactional`/`@GlobalTransactional` 计数 0 | 中转动作半成功即状态错乱，是本域最大的一片无保护写 | 关键动作补 `@GlobalTransactional`（8 库 undo_log 已就位），注入失败验证全回滚 |
| B-21 | P1 | 附件/POD 上传指向**未部署的服务** | `pd-web-courier/.../AttachmentClient.java:18` `name="${pinda.feign.authority-server:pd-file-server}"`；仓库无该属性覆写、compose 无 `pd-file-server` | 快递员 POD 电子签收（照片证据链）无法落附件，签收凭证缺失 | 部署文件服务或在 Nacos 显式覆写该属性，并跑一次上传落库验证 |
| B-22 | P2 | 死契约与越权信任 | `AppDriverFeign.java:12` 全仓零调用方；`pd-aggregation/.../AppCourierController.java:25` 直接信任请求体里的 `courierId`；`TxXidFilter.java:24` 允许客户端自带 `TX_XID` | 无鉴权的身份入参可冒充他人查询/操作；伪造事务号可能污染全局事务上下文 | 删死契约；身份一律从 `userid` 头取；`TX_XID` 只接受内网来源 |

## 修正取证员的两处误判（记录以免以讹传讹）

| 被推翻的说法 | 实测/证据 |
| --- | --- |
| "pd_aggregation 副本表都是空的，pd_area 0 行" | 实测 `pd_aggregation.pd_area=44703`；2026-10-08 复测三库 `pd_area` **各 44703 行**（`pd_auth` / `pd_aggregation` / `pinda_tms`，本文件旧版把 pd_auth 误记成 44474，已改正）。真正空的是 `pd_order`、`pd_core_org` 这类业务副本。**结论仍是无同步机制，但依据不同** |
| "pd_auth 库里没有 `pd_area` 表，`Area` 实体指向不存在的表" | 实测 `pd_auth.pd_area` 存在且有 44474 行 |
| "pd-work 的 `StateTransitionValidator` 是死代码" | 它被 `TaskTransportServiceImpl`、`TransportOrderServiceImpl` 调用且有 `StateTransitionValidatorTest`；真正的问题是**与 pd-oms 内联 map 重复定义两份** |


## 还需要人确认的未知项

| 未知 | 为什么无法从静态证据判定 | 验证动作 |
| --- | --- | --- |
| 真实业务单能否走完 23000→23009 | 线上只有 `status=1` 种子单 | 手工造 1 条新单走完全链路并逐步记录状态 |
| 设备/移动端是否已在上报轨迹 | 无真机；`netty.port=8194`（Nacos 实测）但容器无 netstat 未证实监听 | 宿主 `nc -zv 127.0.0.1 8194`，或用 HTTP `/netty/push` 先打通 |
| `pd_aggregation` 副本是否有隐式同步作业 | 未见同步任务/触发器 | 查 Quartz 表与 crontab、确认 `pd_area` 副本是谁灌的 |
| 自寄单（23002）实际是否卡单 | 需真实样本 | 按 `pickup_type=1` 统计状态分布 |
| `TruckFeign` fallback 静默降级是否掩盖过心跳失败 | fallback 返回空且异常被吞 | 临时把 pd-base 停 30 秒，看心跳缺失是否有告警 |

## 续：支撑域体检（2026-10-08）

主干之外的四块地基（认证授权 / 主数据 / 计费支付通知 / 前端契约与公共层）另见
`support-domains-review.md`，编号 A-x / B-x / C-x / D-x，风险关系图 `support-domains-risk-map.dot`。
本轮被子代理说错、被我推翻或改级的 4 条单独记在该文件 §6，其中最重要的是：

- **推翻**"全仓无 refreshToken"：后端确实没有刷新接口，但前端 `utils/request.js:40-42,104,225-239` 有完整 refresh 流程 —— 真实缺陷是"前端有刷新、后端无接口"。
- **实测补强**：`pd_oms.rule` 与 `pinda_tms.rule` **各 0 行**（`rule_key='tms'` 查不到），配合 `OrderServiceImpl.java:257-262` → 当前运费计算返回 null，不是"运价口径不统一"这么轻。
- **实测补强**：三库 `pd_area` 各 44703 行，把 `导入区划四级_可重复执行.sql:47` 的 `TRUNCATE`（而 `:52` 回填是注释）从"脚本危险"抬成"一次误执行清掉 134109 行"。
- **改级**：`pd-file-server` 在注册中心实测不存在（14 个服务，无此项）且三端 `AttachmentClient` 无 fallback → 附件/POD 上传是运行时必抛，P2 → P1。
