# 调度内部与客户·报表域体检（第三份）

配套 `business-flow-understanding.md`（主干流程）与 `support-domains-review.md`（四块地基）。
这份管两件前面只碰到边缘的事：**调度作业内部到底能不能产出可执行运单**，以及
**客户看到的金额/角标/看板是不是真数**。

方法同前：两个只读子代理分域提取，我逐条回代码复核 + 上服务器实测；
未采信的写在 §4，不混进结论。日期 2026-10-08。

---

## 1. 一句话结论

调度的**触发器是活的，产路是断的**：Quartz 配置在库里（`schedule_job` 实测 5 行），
但 `schedule_job_log` 实测 **0 行** —— 一轮都没成功跑过；同时异常重试任务因为缺表
每 10 分钟准点抛异常（我已建表，13:30 那一拍起不再报错，见 §3）。
客户端侧则是**数字全是假的**：客户单的运费恒等于写死的 23，角标把所有历史订单都算成"待处理"，
报表服务查的表在任何库里都不存在而异常被吞成空数组。

## 2. 调度内部（pd-dispatch）

| # | 结论 | 证据 | 等级 | 归属 |
| --- | --- | --- | --- | --- |
| S-1 | 异常重试任务**因缺表每 10 分钟抛一次异常**，整套"异常登记→自动重试"设计等于没上线 | `ScheduleExceptionRetryTask.java:44`（cron 默认 `0 */10 * * * ?`，Nacos 两个 profile 实测均无该键）、`:51` → `listPending()`（`ScheduleExceptionOrderServiceImpl.java:71-75`）；实测 `docker logs pd-dispatch` 12:50/13:00/13:10/13:20 四条 `[异常调度重试] 定时重试执行异常`，且 `pd_schedule_exception_order` 当时不存在 | **P0（已修）** | 仓库里 DDL 已跟踪但从未执行；本轮已建表 |
| S-2 | PENDING 异常单只能靠人工接口关闭，`markHandled()` **无任何调用方**（重复实现在控制器里）→ 只要有一条脏单解不掉，该机构**每 10 分钟重跑整轮调度**（含 6 次百度调用） | `IScheduleExceptionOrderService.java:37` 与 `ScheduleExceptionOrderServiceImpl.java:86` 定义 `markHandled`，全仓 grep 无调用点；实际收口在 `ScheduleExceptionOrderController.java:75-89`（`PUT /{id}/handle` 自己 setStatus/setHandleTime） | **P1** | 代码在仓，无人认领 |
| S-3 | 无并发保护：`@Scheduled` 无分布式锁、`ScheduleJob` 未加 `@DisallowConcurrentExecution` → 多实例或"人工点重试 + 定时轮"并发时，同一批 `OUTLETS_WAREHOUSE` 订单会被两轮同时查到并各建一套运输任务 | 全仓 grep `DisallowConcurrentExecution\|SchedulerLock\|ShedLock` 命中 0；状态回写发生在建任务之后（`TaskOrderClassifyServiceImpl.java:266` vs `BusinessOperationServiceImpl.java:83`） | **P1（当前单实例，属扩容即爆）** | — |
| S-4 | 一条坏订单炸掉整机构一轮：地址/坐标/区域类失败全部走 `RuntimeException`，而"异常登记→重试"只覆盖 ERROR 分组，接不住这类失败 | `TaskOrderClassifyServiceImpl.java:184,190,198,207,213,219,293-295,355-372`；登记仅发生在 `TaskRoutePlanningServiceImpl.java:52-64` | **P1** | 同文件我改过 :210/:330 的类型比较 |
| S-5 | 运单 id 被**加两遍**：先逐单 `findByOrderId` 加入（:79），随后又对同一批订单 `findByOrderIds` 整批加入（:90-93），无去重 → 运输任务的运单关联翻倍，列表与计数虚高，按单结算会重复计费 | `BusinessOperationServiceImpl.java:66,79,91,103,115` | **P1** | **该文件正被在编占用（`git status` 为 M），我未改** |
| S-6 | VRP 名不副实：全量路径暴力枚举 + 单指标排序，`depth` 字段存而不用，无容量/装载约束；"派车派司机"只取线路第一条车次（`get(0)`）按发车时间就近选，不校验车辆与司机占用 | `TaskRoutePlanningServiceImpl.java:195-214,297-323`；`AnalysisRoutePlanningDTO.java:26-70,89-133`；`TaskTripsSchedulingServiceImpl.java:75,132-174` | **P2** | — |
| S-7 | 事件路径是死的：`OrderEventMQListener` 里"触发智能调度"整段被注释；`OrderEventListener` 的两个 `@EventListener` 在全仓**没有任何 publish 点** → 调度只有 Quartz 一条产路，而将来误启用会双发 | `OrderEventMQListener.java:107-110`；`OrderEventListener.java:67,82,104,124`；全仓 grep `publishEvent`（排除监听器自身）为空 | **P2** | — |
| S-8 | 6 个服务的 prod 档把 MyBatis SQL 打成 `StdOutImpl`（Nacos 无任何 dataId 覆盖 log-impl）→ 上量后 stdout 会被 SQL 灌满 | `grep` 实测 pd-aggregation/pd-base/pd-dispatch/pd-oms/pd-user/pd-work 的 `bootstrap-prod.yml` 全部命中；Nacos 8 个 prod dataId 覆盖次数均为 0。当前体量还小（实测容器 json-log：pd-work 6.1M、pd-user 6.0M、其余 0.7~1.3M），因为没有流量 | **P2（暂不改）** | 会抽掉在编 agent 正在用的 SQL 调试输出，等他们收手再改 |

## 3. 客户与报表域

| # | 结论 | 证据 | 等级 |
| --- | --- | --- | --- |
| C-1 | **客户单运费恒为 23 元**：先调 `orderMsg` 算价，而货物信息在几十行之后才 set；服务端对空货物直接返回空 map，于是 `getOrDefault("amount","23")` 永远命中兜底值 | `MailingController.java:136-137`（`orderFeign.getOrderMsg` + 兜底 `"23"`）对照 `:213-214`（`orderDTO.setOrderCargoDto(cargoDto)` 发生在校价之后）；`OrderController.java:118-120`（`orderCargoDto == null` → 返回空 map）。同一段在快递员端兜底是 20（`CourierController.java:744-747`），两个端两个假数 | **P0**；**`MailingController.java` 正在被在编占用，我未改** |
| C-2 | 客户端"待处理"角标其实是**该会员的全部订单数**：查询只带 memberId 或收件手机，不拼状态与时间条件 | `MailingController.java:855-872`；`OrderServiceImpl.java:222-228`（wrapper 只有 id/keyword/memberId/receiverPhone） | **P1** |
| C-3 | 地址簿行级越权（detail/PUT/DELETE 都只认 id，save/update 还采信请求体 userId）→ 可读他人姓名手机详址，甚至改走一行归属 | `pd-user/AddressBookController.java:64-72,109-125,133-141,40-56` | **P0（已修，见提交 92f624a）**；线上该表 0 行，收紧不影响存量 |
| C-4 | `isDefault` 是 Integer 却写 `1 == entity.getIsDefault()`，客户端不传该字段即拆箱 NPE 500 | `AddressBookController.java:45,117`；同类写法 `MailingController.java:860,895`（`0 == dto.getMailType()`，字段见 `MailingQueryDTO.java:10`） | **P1**；地址簿两处已修，客户端两处随 C-1 一起在编未动 |
| C-5 | `@Cache` 缓存键只含地址 id、不含调用方 → A 查过一次后 B 用同一 id **直接命中缓存拿到 A 的地址**，行级校验被整层旁路；而 save 里手工 `cacheChannel.set(region, entity.getId(), ...)` 用的是裸 id，与注解路径的 `ab:{id}` 记法不一致，写进去的项既不会被命中也不会被清除（内含 PII） | `AddressBookController.java:52`（手工写裸 id）vs `:65`（`key="ab", params="id"`）与 `:110,:134` 的 evict 记法 | **P1**；本轮去掉 detail 的缓存注解（见提交），手工写 key 的不一致留给在编流程一起收 |
| C-6 | 报表服务查的表根本不存在，异常还被吞成"空数据"：`tms_order_location` 在 8 个库 163 张表里全量比对**没有任何一库有它**（近似名 `pd_order_location` 的列是 send_location/status，完全对不上）；而 `BaseMapper` catch 后返回空 → 接口看起来 200、前端只是"没数" | `DruidServiceImpl.java:46,64,80,92,119,131`；实测 `POST http://172.21.0.7:8193/apache-druid/query/select` 返回 `code:0`，同时容器日志立刻出现 `Table 'pd_oms.tms_order_location' doesn't exist`（pd-druid 的 datasource 实测是 pd_oms） | **P0（需你拍板：改成查 `pd_oms.pd_truck_location` 还是建视图）** |
| C-7 | 会员域没有注册/改密/注销实现，且 `pd_member` 主键与后台 authId 混用：首次读 profile 时用 authId 当 `pd_member.id` 惰性建档，同一个 id 又被写进 `pd_order.member_id` → 会员维度报表会漏会串 | 全仓 grep 无 register/logout/password 映射（仅管理端 `UserController.java:232`）；`pd-web-customer/MemberServiceImpl.java:35-47`、`MailingController.java:186` | **P1（需你定会员账号模型）** |
| C-8 | 身份证/手机号/详址原样进 INFO 日志与响应体 | `CourierController.java:639,647,657`、`MailingController.java:113`、`MemberServiceImpl.java:29,33`、`MemberController.java:52-55`（直返 `id_card_no`/`phone`） | **P1** |
| C-9 | 聚合分页对 join 后的行集计数且 `SELECT *` 有重名列 → 一单多运单/一任务多司机作业时，列表与总数虚高、运单号可能取到 join 表的 id | `pd-aggregation/src/main/resources/mapper/WebManagerMapper.xml:30-33,70-71,114-116` + `MybatisPlusConfig.java:11` | **P2（需真实数据，当前 `pd_driver_job` 实测 0 行）** |
| C-10 | 演示/孤儿接口：`/cache/**`（**在 pd-user**）返回硬编码 "beijing/nanjing"，却已被注册成权限资源；`apache-druid/query/*` 无任何调用方；`/member/page`、`DELETE /member/{id}` 有 Feign 声明零引用；`/webManager/*` 4 个仅注册 | `pd-user/src/main/java/com/itheima/pinda/controller/J2cacheController.java:26,34-43,51-61,69-76`；`docs/sql/管理端接口资源注册_20261007.sql:239-242`（四条 cache 资源）；`MemberFeign.java` | **P2** |

## 4. 本轮被子代理说错、被我推翻的（别外传）

- **推翻**："全仓无 `DataSourceProxy`，所以 `@GlobalTransactional` 大概率保护不了跨服务写"。
  实测 Nacos `pd-work-prod.yml:55-59` 有 `seata.enabled: true` + **`enable-auto-data-source-proxy: true`**，
  seata-spring-boot-starter 会自动代理数据源；而我此前已用真实全局事务写入并核对过 `undo_log`。
  "代码里没有显式 DataSourceProxy" ≠ "没有代理"，这条若照抄会得出相反结论。
- **推翻**："schedule_job 空表 = 调度完全不启动"。实测 `pd_dispatch.schedule_job` 有 **5 行**；
  真正的事实是 `schedule_job_log` **0 行**（从未成功跑过一轮）——诊断方向不同。
- **降级**：prod `StdOutImpl` 判为高危刷屏。实测当前容器 json-log 只有 0.7~6.1M，
  因为系统没有流量；风险是真的但要等上量才成立，故记 P2 且暂不改（会抽掉在编同事的 SQL 调试输出）。
- **实测补强**：子代理把"重试任务可能重复触发"写成条件式，实测它在**每 10 分钟稳定抛异常**
  （12:50/13:00/13:10/13:20 四条 ERROR），原因比它猜的更靠前——表压根不存在。建表后 13:30 那一拍无报错。

| C-11 | **Feign 契约与响应封装对不上**（比 C-1 更底层的一条）：`AddressBookFeign.detail` 与 `MemberFeign.detail` 声明的返回类型是实体 `AddressBook` / `Member`，而服务端这两个方法返回的是 `Result`（`{code,msg,data}`）。全仓**没有任何自定义 Feign Decoder**（grep `implements Decoder\|JSONDecoder` 命中 0），同目录其余 6 个 Feign 方法都正确返回 `Result`，只有这两个用实体。契约错位是代码级确定的；具体表现取决于运行期 ObjectMapper 的 `FAIL_ON_UNKNOWN_PROPERTIES`（Spring Boot 默认关闭 → 调用方拿到字段全空但非 null 的对象，`if (x == null)` 挡不住；若被打开则解码直接抛异常）——**这一条的运行期表现我没测到**：探测时 pd-user 正被这次部署重建（Nacos 实例暂时 `hosts: []`），故不下结论 | `AddressBookFeign.java:58-59` vs `pd-user/AddressBookController.java:64-72`；`MemberFeign.java:58-59` vs `MemberController.java:47-48` | **P0** |

C-11 的调用面（逐个 grep 过，不是猜的）：`memberFeign.detail` 有 **3 个真实调用方**——`CourierController.java:272`（取派件时取会员）、`:624`（身份证关联）、`MemberServiceImpl.java:28`（客户端 profile）；`addressBookFeign.detail` 有 3 处（地址详情 + 下单取寄/收地址）。
| C-12 | 由 C-11 连出来的下游：`pd-web-customer` 的地址详情与下单取址都走这两个 Feign，因此 `provinceId/cityId/countyId`、收件人姓名手机详址实际全是 null 进订单；而 `MemberServiceImpl` 走的是 `Map` + 手工 `user.get("data")` 解包，所以它没事——**同一个工程里两种解包约定并存**，改哪个都容易漏 | `pd-web-customer/AddressBookController.java:156-165`、`MailingController.java:105,109`；对照 `MemberServiceImpl.java:32-36` | **P1** |

说明我在 C-3 加的 401/403 会表现成什么：因为 C-11 已经存在，越权被拒时客户端拿到的仍然是"字段全空的地址"而不是错误提示——**这是 C-11 的既有缺陷，不是我这次改动引入的**。修好 C-11（两个 Feign 方法改成返回 `Result` 并在调用方解包）之后，403 才会以清晰报错的形式浮到界面上。这两条最好由正在改 `MailingController` 的人一起收，否则同一个文件会冲突两次。

## 5. 我这轮改了什么 / 什么没改

已改并验证：S-1（建 `pd_dispatch.pd_schedule_exception_order`，用仓库里已跟踪但从未执行的脚本）；
`pd_work.pd_status_transition_history`（新增脚本并执行，13 列与实体对齐，此前审计历史静默全丢）；
C-3/C-4/C-5 的 pd-user 地址簿部分（提交 `92f624a`）。

**没改以及原因**：
- C-1、C-4 客户端两处、S-5 双份关联——`MailingController.java` 与 `BusinessOperationServiceImpl.java`
  当前都在他的 agent 工作区里被改（`git status` 为 M），我不动别人的在编文件；结论与修法我都写明，
  改起来各只需几行，最好由正在写这段的人顺手收掉，避免同一文件两次冲突。
- `docs/sql/区划数据/导入区划四级_可重复执行.sql`（TRUNCATE 有、回填是注释）与
  `GpsTraceConsumer.java` 的轨迹 type 大小写——同样是未跟踪/在编文件。
- 告警建表脚本 `docs/sql/告警记录_建表.sql` 写的是 `USE pd_netty`，但线上没有 `pd_netty` 库、
  而 pd-netty 的 datasource 实测是 `pd_oms` → **这个脚本执行会失败或把表建到没人连的地方**。
  该脚本是他 agent 今天的未跟踪文件，我不改内容，只把错位报出来。

### 部署后在线验收（2026-10-08 13:38–13:44，deployTag=419334a）

| 项 | 实测结果 |
| --- | --- |
| 服务状态 | 15/15 healthy，`RestartCount=0`（是重建不是崩溃循环），中间件 6/6，网桥检查正常，内存 8896M/15946M |
| 注入是否落地 | pd-oms 容器 env 有 `JAVA_TOOL_OPTIONS=-Dbaidu.map.ak=`；pd-auth-server 有 2 个 `PINDA_JWT_*`、pd-gateway 有 1 个；`/data/pinda-jwt -> /data/pinda-jwt` 挂载已生效 |
| B-3 运价规则 | pd-oms 本次重启的**启动期**日志出现 `SELECT ... FROM rule WHERE (rule_key = ?)` + `KieModule was added`（13:41:31）→ 规则不再依赖手工 reload，冷启动就带着运价 |
| C-2 伪造回调 | `POST /pay/callback/wechat` 只带 `out_trade_no` → `{"msg":"支付回调处理失败","code":500}`，日志 `[支付] 回调验签失败: channel=wechat`。要说清楚：今天的拒绝来自 `isConfigured()` 那道闸（未配商户参数），我补的是**配了商户参数之后**那道原本会敞开的闸 |
| A-5 菜单越权 | 直连 auth 无身份头时，`/menu/router?userId=1` 与不带参数返回一致（`data:[]`）→ 查询参数已不起作用 |
| deploy-check 第 8 节 | 已装到 `/usr/local/bin`；输出"未轮换：pd-auth-server 仍从镜像内 classpath 读私钥"，全篇"结论: 全部通过"（未轮换属提示级，不判失败） |
| S-1 缺表 | 建表前后对照：12:50/13:00/13:10/13:20 每 10 分钟一条 `定时重试执行异常` → 13:30 那一拍起无报错，近 4 分钟 `doesn't exist` 计数 0 |
