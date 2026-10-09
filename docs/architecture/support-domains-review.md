# 支撑域体检报告：认证授权 / 主数据 / 计费支付 / 前端契约与公共层

配套《business-flow-understanding.md》（那份管主干业务流，这份管主干底下的四块地基）。

审查日期：2026-10-08。方法：4 个只读子代理分域提取候选结论 → **我逐条回代码复核 + 上服务器实测**，
未经确认的一律不进正文，单列在 §6。每条结论后面的 `证据:` 都是真实可打开的 `文件:行号`。

---

## 一句话结论

主干"结构齐、血流不通"，这四块地基层的问题是**同一句话的另一种说法：能跑的都是假的，真的都缺最后一环**。
收款链三条路全断（默认 mock 自付、回调验签可绕、真实回调被网关拦死）；运价规则表 0 行导致计价直接返回 null；
自动分单有两处 `String.equals(Long)` 恒 false；签名私钥躺在公开仓库；权限的唯一强制点在网关，而它 fail-open。

---

## 1. 认证与授权域（pd-auth-server / pd-tools-jwt / pd-user）

**四类身份其实只有一套。** 全仓唯一签发点是 `AuthManager.login` → RS256（`JwtHelper.java:119`），
claims 只有 `userId/account/name/orgId/stationId`（`AuthManager.java:92`）。司机、快递员、客户复用
`pd_auth_user` 与同一个 JWT，**没有身份类型字段**；`/anno/loginMobile` 在需求文档里仍标"待新增"
（`docs/需求文档/03-全局定案与待确认项清单.md:48`）。

| # | 结论 | 证据 | 等级 |
| --- | --- | --- | --- |
| A-1 | **JWT 签名私钥被 git 跟踪，且已在公开 Gitee 上**；compose 未注入外部密钥，代码保留 classpath 兜底 → 有源码即可伪造任意 userId 的合法 token | `git ls-files` 命中 `pd-auth-server/src/main/resources/client/pri.key` 与 `pd-tools-jwt/src/main/resources/pri.key`（实测 636 字节，DER 头 `30820278` = PKCS#8 RSA-2048）；`git ls-tree origin/master` 同样命中 2 个；`RsaKeyHelper.java:111-117` 兜底；`deploy/apps/docker-compose.app.yml:118-128` 只注入 jvm/nacos/redis | **P0** |
| A-2 | 数据/组织权限（DataScope）**只有结构没有行为**：拦截器全仓零装配点 | `DataScopeInterceptor.java:40` 是唯一命中；`UserServiceImpl.java:61-63` 造完 `new DataScope()` 却把参数位传 `null`；`StationServiceImpl.java:48` 传空对象 | **P0** |
| A-3 | 密码算法双轨：登录 BCrypt+MD5 兼容，**所有写入路径都是裸 MD5**，重置后固定弱口令 123456 | `UserServiceImpl.java:78`（改密比 md5）、`:154`（建号 md5）、`:175`（改号 md5）、`:164-165` + `BizConstant.java:15`（`e10adc3949ba59abbe56e057f20f883e` = MD5("123456")）；登录侧兼容见 `AuthManager.java:153-165` | **P1** |
| A-4 | 司机端把 JWT 的 userId 当**司机档案主键**直查，账号↔档案无外键，实测两边行数不同（司机 10 / 后台账号 13） | `pd-web/pd-web-driver/.../UserController.java:81`（`String driverId = RequestContext.getUserId()`）→ `pd-base/.../DriverController.java:69` 按主键 findOne；`PdTruckDriver.java:31` 有 `userId` 字段但查询链没用 | **P1** |
| A-5 | `/menu/router` 与 `/menu/my` 允许用**查询参数指定 userId，且参数优先**于网关注入的身份头 → 登录用户可枚举他人菜单 | `MenuController.java:181-195`（先 `isBlank(userId)` 再取 header）、`:136-141` 同款 | **P1** |
| A-6 | 登出链路两个静默失效面：`/anno/logout` 在免鉴权段（不验签即可把**别人的 token** 拉黑）；`LoginController.java:59` 用常量 `"token"` 读头，而头名是配置项 | `AuthManager.java:229-241`（空值/过期直接 return，接口恒返回 true）；实测网关 `pd-gateway-prod.yml:10` `header-name: token`、`:19-22` `ignore.auth.url: [/anno, /captcha]` | **P2** |
| A-7 | 会员（客户）是第二套账号，`pd_member.authId` **只是个字段，无任何绑定行为**；会员分页全表无条件返回含手机号/身份证 | `Member.java:22-24`、`MemberController.java:32-111`；实测 `pd_users.pd_member`=3 行，且 `pinda_tms` 里还有一份同名副本（影子表见主报告断点二） | **P2** |
| A-8 | 登录审计与验证码生命周期同样只有结构：成功/失败都不写 `LoginLog`，`passwordErrorNum` 不参与锁定，验证码 key 无 TTL | `UserServiceImpl.java:139-141`（`updateLoginTime` 无调用方）、`:134-136`；`ValidateCodeServiceImpl.java:57`；`caffeine.properties` 无 `captcha_*`/`login_fail` 区域 | **P2** |

验证码本身没问题：本地生成（com.wf.captcha），无"跳过校验"分支；`GET /anno/token` 已默认关闭且 prod 拒绝（`LoginController.java:103-117`）。

## 2. 主数据域（pd-base / pd-aggregation / docs/sql）

| # | 结论 | 证据 | 等级 |
| --- | --- | --- | --- |
| B-1 | **区划导入脚本执行即清空 `pd_area`，回填语句是注释行**，而标题写着"可重复执行"，还要求三库都跑 | `docs/sql/区划数据/导入区划四级_可重复执行.sql:47`（`TRUNCATE TABLE pd_area;` 生效）、`:52`（`LOAD DATA LOCAL INFILE` 被注释）、`:9`（三库各自执行）。实测三库现各有 **44703 行**（pd_auth / pd_aggregation / pinda_tms），一次误执行 = 清掉 134109 行区域 | **P0** |
| B-2 | 区域 id 口径断裂 + **两处** `String.equals(Long)` 恒 false → 自动分单每单必抛"区域不一致" | `Order.java:65,70,75` 是 `String receiverXxxId`；`Area.java:32` 是 `extends Entity<Long>`；`TaskOrderClassifyServiceImpl.java:210`（寄件）与 `:330`（收件）。另 `:324` 依赖 `adcode+"000000"` 的编码拼接约定 | **P0** |
| B-3 | **运价真相不在主数据**：计费读 `rule` 表里的 DRL 文本，而线上 `rule` 表 **0 行** → `getKieContainer()` 为空 → 计价直接 `return null` | `ReloadDroolsRulesService.java:34-37`（`wrapper.eq("rule_key","tms")`）、`OrderServiceImpl.java:257-262`（container==null 则记 error 并返回 null）、`rules/orderAmountCalc.drl:29-31`（首重20/续重6/9/15 写死）；实测 `pd_oms.rule`=0 行、`pinda_tms.rule`=0 行，全仓无一条 `INSERT INTO rule` 脚本。`pd_transport_line.cost` 只被 `AnalysisRoutePlanningDTO.java:43` 消费，客户报价不读它 | **P0** |
| B-4 | 运价表三列全 UNIQUE + 规则装在单 JVM 内存 + 刷价靠无鉴权的手工 GET：无法分客户/线路定价，多实例只刷一台 | `docs/mysql/TMS项目建库脚本/pd_oms.sql:103-105`；`ReloadDroolsRulesService.java:27,43-49,54`；`RulesReloadController.java:13,28`；`CommandLineRunnerImpl.java:19`（启动刷一次） | **P1** |
| B-5 | 网点有三个"真相"，新表无人消费：owner 实为 `pd_auth.pd_core_org`（经 OrgApi），`pd_base` 的 agency_id 是 varchar 逻辑悬空，`docs/sql` 补建的 `pd_agency` 全仓 0 处实体/Mapper 引用 | `docs/sql/pd_agency_建表与初始化.sql:6-9,25`；`docs/sql/种子数据初始化_FR00.sql:58-60`（脚本自述"业务库无对应表"）；消费方 `ScheduleJobLogController.java:133` | **P0** |
| B-6 | 停用/删除零引用校验，主数据零外键、零二级索引；同一实体两套可见口径 | `PdFleetServiceImpl.java:75`、`PdTruckServiceImpl.java:108`（仅翻 status）；建库脚本 `pd_base.sql:195-207`（`pd_truck` 只有主键，车牌无 UNIQUE）；`PdGoodsTypeServiceImpl.java:45` 过滤 status 而 `:74-80` 不过滤且 ids 空时返回全表；`PdTruckDriverServiceImpl.java:64-70` 用 `getOne` 取 user_id + `docs/sql/修复_司机user_id冲突.sql:2-8` 人工修数据兜底 | **P1** |
| B-7 | 一键导数链路整条失效（路径不存在 + `set -e` 第 1 步就中断 + 增量步骤只打印"[跳过]"），且 `pd_area` 无任何缓存 | `deploy/server/import-data.sh:24-29`（写 `mysql/…`，实际在 `docs/mysql/…`）、`:36-42`（`docs/sql/V1.0~V1.6` 均不存在）、`:33`（把 0 代码引用的 `pd_goods_info` 灌进 pd_base）；`pd_aggregation.sql:23-43` 是 pd_area 唯一 DDL 且无二级索引；全仓无 pd_area 的 CacheMapping | **P1** |
| B-8 | 供应商/外协承运商在本域无主数据：只有常量与结算列，结算对象没有 owner 表 | `SettlementOrder.java:37-39`；`docs/sql/财务结算域_建表.sql:49`；建库脚本 grep 无 carrier/supplier 表 | **P2** |

Feign 契约侧我让子代理抽样核了 11 个（FleetFeign / TransportLineFeign / TransportTripsFeign / TruckFeign /
DriverFeign / GoodsTypeFeign / AgencyScopeFeign / CourierScopeFeign / AppCourierFeign / AppDriverFeign /
WebManagerFeign），**路径与参数逐一对齐，契约层没有缺口**——这条对得上我的复核，写业务时不用担心接口签名漂移。

| B-9 | **百度地图 AK 根本没注入**：代码用 `System.getProperty("baidu.map.ak", "")`，实测 `pd-oms` 容器 env 里 `baidu` 出现 0 次 → 地址转坐标恒失败 → `getDistance()` 提前返回、运费**根本没进入计价环节**。宿主出网是通的（`api.map.baidu.com` HTTP 200），属配置缺失不是网络问题 | `BaiduMapUtils.java:35`、`EntCoordSyncJob.java:29`；实测 `POST 172.21.0.6:8186/order/orderMsg` 返回 `"senderAddress":"sender error msg"` 且无 amount。缓解事实：`OrderController.java:77-81`（save）与 `:214-217`（reprice）都挡下这个分支，**不会产生 0 元订单**，只是单下不进去 | **P0** |

## 3. 费 → 收款 → 对账 与 通知/文件链路

| # | 结论 | 证据 | 等级 |
| --- | --- | --- | --- |
| C-1 | **默认渠道就是 mock，而 mock 会服务端自行把单置已支付**；全部 prod dataId 里没有一行 `pay` 配置 → 线上运费"点即已付"，零收款 | `PayServiceImpl.java:53`（`@Value("${pay.channel:mock}")`）、`:120-132`（mock 单创建后立即自调 `handleCallback` 置 PAID）；实测 Nacos `pd-oms-prod.yml` 全文 71 行无任何 pay/channel 键，23 个 dataId 中无一命中 | **P0** |
| C-2 | 微信回调验签**实际不可达**：控制器只收 JSON body，而微信把签名放在 HTTP 头、body 还是加密的 → 验签分支永远进不去，落到兜底 `out_trade_no != null` 即放行 | `PayController.java:60-67`（`@RequestBody(required=false) Map`）；`WechatPayChannel.java:128-150`（要求 `wechatpay_signature/timestamp/nonce/rawBody` 四元组，缺任一即跳过校验，最后 `return params.get("out_trade_no") != null`）。未配置商户参数时是 fail-closed（`:129-132`），配了反而可绕 | **P0** |
| C-3 | **真实渠道的回调根本进不来**：回调 URL 不在网关免鉴权名单里，渠道方拿不到 token → 401。这条与 C-2 同时成立：伪造者（带任意 token）能过，正规渠道过不了 | 实测 `pd-gateway-prod.yml:19-22` `ignore.auth.url` 只有 `/anno`、`/captcha`；路由 `:42-47` `/oms/** → pd-oms` | **P0** |
| C-4 | **支付宝退款是桩**：配置齐全时把参数拼好、签好名，然后注释掉网关调用直接 `return true`；未配置时两渠道都"模拟退款成功"。上层据此把支付单和订单一起置 REFUNDED | `AlipayPayChannel.java:160-186`（`:183-184` "生产环境通过网关 POST" 后 `return true`）；`WechatPayChannel.java:186-190`；`PayServiceImpl.java:247-262`。且只有全额退、无退款流水表 | **P0** |
| C-5 | 无对账 / 无超时关单 / 无掉单补偿：三渠道 `queryPayment` 全部硬返 false，`STATUS_CLOSED` 无任何写入点，全仓 `@Scheduled` 只有 GPS 心跳与调度重试 | `WechatPayChannel.java:177-183`、`AlipayPayChannel.java:153-158`、`MockPayChannel.java:53-56`；`PaymentOrder.java:37`；`GpsTraceConsumer.java:494`、`ScheduleExceptionRetryTask.java:44` | **P1** |
| C-6 | 结算链"只生单不推进"：`STATUS_RECONCILED/SETTLED` 两个状态主代码零引用，运费明细固定 1 条"总价"无费用项拆分；且**结算表在库里根本不存在**（实测全库只有 `pinda_tms.pd_payment_order`，0 行） | `SettlementOrder.java:59,64`；`SettlementServiceImpl.java:105-113,132`；实测 `information_schema` 查 `pd_settlement_order`/`pd_settlement_item` = 无 | **P1** |
| C-7 | 计价无体积重：入参只有 `totalWeight + distance`（distance 取百度**直线**距离），`volume` 不参与，全仓无体积换算系数；续重 `ROUND_DOWN`（3.9kg 按 2kg 续重收）；端侧还有死价兜底 23/20 | `OrderServiceImpl.java:269-274`、`:323-327`；`orderAmountCalc.drl:10-71`；`DroolsRulesServiceImpl.java:30`；`MailingController.java:137`、`CourierController.java:747` | **P1** |
| C-8 | **POD/附件无处落地**：三端各自复制 `AttachmentClient` 默认指向 `pd-file-server`，而注册中心实测**只有 14 个服务、无 pd-file-server**，Feign 无 fallback → 上传必抛 No instances available；仓库内也无该模块与 compose 条目 | `pd-web-courier/.../feign/AttachmentClient.java:16-19`（另两端同构）；三端 `AttachmentController.java:24/26` 只有 `upload` 无 `page`；实测 Nacos 服务清单：pd-aggregation/pd-auth-server/pd-base/pd-dispatch/pd-druid/pd-gateway/pd-netty/pd-oms/pd-user/pd-web-{courier,customer,driver,manager}/pd-work | **P1** |
| C-9 | 短信默认 `http` 通道且 webhook URL 默认为空 → 静默跳过；失败只 warn 不落库不重试无频控；HTTP/Device 两个渠道**明文打印手机号** | `pd-dispatch/src/main/resources/bootstrap-dev.yml:94-100`；`GenericHttpSmsChannel.java:41-44`、`:55,58`；`DeviceSmsChannel.java:65,68`；`SmsNotificationService.java:69-71`；触发点只有揽收/交付两处（`OrderEventMQListener.java:113,172-176`），下单无通知 | **P2** |

## 4. 前端契约与公共工程层

| # | 结论 | 证据 | 等级 |
| --- | --- | --- | --- |
| D-1 | **6 个前端 API 模块指向整库不存在的路由**：`/authority/dictionary`、`/authority/tenant`、`/authority/application`、`/authority/systemApi`、`/msgs/…`、`/file/attachment/page`。后端全部 `@RequestMapping` 顶层路径实测只有 22 个（`/anno /area /j /loginLog /menu /netty /optLog /orderLocus /org /pay /reload /resource /role /roleAuthority /rules /schedule /scheduleExceptionOrder /scheduleLog /station /trace /user` 等），逐个 grep 匹配数=0 | `pd-admin-ui/src/api/{Dictionary,Tenant,Application,SystemApi,Msgs,Attachment}.js`（如 `Dictionary.js:7`、`Msgs.js:6-18`） | **P1** |
| D-2 | **后端业务异常在前端表现为"空数据"而不是报错**：拦截器无条件 `resolve`，而它判错的键根本不存在——`Result` 是 HashMap，只有 `code/msg`（成功码 0），**没有 `isError`** | `pd-admin-ui/src/api/AxiosApi.js:43-58`（`if (res.data.isError)` … 末尾仍 `resolve(res)`）；`pd-common/.../Result.java:20,23-25`。`R.java:176-186` 才有 `isError/isSuccess`，两套契约并存 | **P1** |
| D-3 | 业务服务**没有全局异常处理**：`DefaultGlobalExceptionHandler` 是 `abstract`（abstract 不成 bean），子类只有 auth-server 与 gateway 两个 → pd-oms/pd-base/pd-work/pd-dispatch/pd-netty 等抛 BizException 直接落 Spring 默认 500 页 | `pd-tools-common/.../DefaultGlobalExceptionHandler.java:52`；子类仅 `pd-auth-server/.../ExceptionConfiguration.java:16`、`pd-gateway/.../ExceptionConfiguration.java:17` | **P1** |
| D-4 | 重置密码按钮**必失败**：前端 GET，后端只有 POST | `pd-admin-ui/src/api/User.js:24-27`（`method: 'GET'`）vs `UserController.java:239`（`@PostMapping("/reset")`） | **P1** |
| D-5 | 操作日志切面把**全部入参 JSON 明文落库**，改密接口恰好挂在它下面 → 旧密码/新密码/确认密码进 `pd_opt_log.params` | `SysLogAspect.java:102`（`JSONObject.toJSONString(args)`）、`:111`（`setParams`）；`UserController.java:230-235`（`@SysLog("修改密码")` + `UserUpdatePasswordDTO`）。同文件 `:228` 有一处被注释掉的同类写入，说明是搬移而非有意 | **P1** |
| D-6 | 菜单/权限是前端化的：静态常量菜单 + 守卫只判 TOKEN，后端 `/menu/router` 无人调用；`v-has-permission` 数据源来自 localStorage，改一下即全放行 | `pd-admin-ui/src/router/menu.js:1-5`（自述"静态菜单…不按角色过滤"）、`src/router/index.js:120-146` | **P1**（与 A-2 叠加后实际等于无权限层） |
| D-7 | 雪花 ID 三份 `IdWorker(1,1)` 硬编码 + 时钟回拨直接抛异常 | `pd-base/.../CustomIdGenerator.java:16`、`pd-oms/.../CustomIdGenerator.java:16`、`pd-work/.../CustomIdGenerator.java:17`；`IdWorker.java:33,86-89` | **P2** |
| D-8 | 轨迹 `type` 大小写口径不一致（**这条在我刚修的链路里**）：校验用 `equalsIgnoreCase`、入库保留原值、查询用精确 `eq`、前端固定小写 → 设备上报 `TRUCK` 的点落库后在页面上永远查不到，还被渲染成"快递员" | `GpsLocationValidator.java:118`、`GpsTraceConsumer.java:403`、`GpsTraceController.java:59,84`、`pd-admin-ui/src/views/pinda/trace/index.vue:12-13,82-83` | **P2** |
| D-9 | 分页两套语义并存（MyBatis-Plus `IPage(records/total)` vs 手拼 `{data:{items,total}}`）；`/enums` 只返回 5 个通用枚举、无任何业务状态枚举；`logging.level.root: debug` 出现在 prod profile | `StationController.java:61` vs `GpsTraceController.java:162-169`、`mixins/crud.js:105-111`；`AuthorityGeneralController.java:44-52`；`pd-gateway/src/main/resources/bootstrap.yml:33-35`、`pd-auth-server/.../bootstrap.yml:50` | **P2** |

## 5. 建议动作顺序（地基版，接着主报告的 8 步之后）

1. **换 JWT 密钥并把密钥请出仓库**（A-1）：新密钥从环境变量/Nacos 注入，删掉 classpath 兜底，
   `.gitignore` + CI 增加"禁止 `*.key`/`*.pem` 入库"的守卫；历史里已公开的那把按已泄露处理。
2. **给 `rule` 表落一行 `rule_key='tms'` 的 DRL**（B-3），否则一切"下单算运费"的联调都在算 null。
3. **区划脚本改成"先备份、后灌数、再校验行数"的原子脚本**（B-1）：`TRUNCATE` 与 `LOAD DATA` 之间加行数守卫，
   缺 CSV 就直接退出，别让人误信"可重复执行"。
4. **修两处 `String.equals(Long)`**（B-2）：统一比较前先 `String.valueOf(area.getId())`，并把区域 id 口径定案。
5. **支付链三处收口**（C-1/C-2/C-3）：`pay.channel` 上线前必须显式配置且**默认值改为拒绝启动**；
   回调控制器接 rawBody + 请求头，验签不可降级为"字段非空"；把回调 URL 加进网关免鉴权并加渠道 IP 白名单。
6. **退款不许返回"模拟成功"**（C-4）：桩实现一律 `return false` 并落失败原因，避免 REFUNDED 假账。
7. **权限强制点补起来**（A-2/D-6/A-5）：注册 DataScope 拦截器；`/menu/*` 不再接受 userId 查询参数；
   菜单恢复后端资源表驱动（守卫加权限判断）。
8. **统一响应契约**（D-2/D-3/D-4）：业务服务补 `ExceptionConfiguration`；前端把 `isError` 判断改成按 `code` 判定；
   顺手把 reset 密码的方法改对。
9. **把敏感数据从日志里摘出去**（D-5/C-9/A-3）：切面对 password/mobile/idCard 字段做白名单脱敏；
   口令写入统一走 BCrypt，去掉 `DEF_PASSWORD` 常量。
10. **明确标注"未实现"而不是留着假接口**（C-5/C-6/C-8/B-5）：结算/对账/pd-file-server/网点表四条，
    要么补齐，要么在 `docs/需求文档` 里改标"未落地"，避免下一个 agent 继续按"已有"写代码。

## 6. 本轮被子代理说错、被我推翻或改级的（重要，别外传）

- **推翻**：「全仓无 refreshToken，所以没有刷新链路」。实测后端 Java 确实没有刷新接口，
  但前端 `pd-admin-ui/src/utils/request.js:4,40-42,104,225-239` **实现了完整的 refresh_token 流程**
  （`grant_type=refresh_token`）。真实结论是"**前端有刷新、后端无对应接口**"，方向相反。
- **降级**：上条里"`request.js` 是管理端在用的一套封装"。实测管理端 23 个 api 模块都走 `AxiosApi`
  （发 `token` 头，与网关 `header-name: token` **一致**），`utils/request.js` 只被 `main.js` 与
  `ImageCropper` 引用。所以这是"两套封装里有一套是半成品"，不是"主链路鉴权错"，P1 → P2。
- **改级**：「POD 附件缺文件服务」原判 P2。注册中心实测无 `pd-file-server` 实例、三端 Feign 无 fallback，
  上传是**运行时必抛**，不是"配置待补"，改 P1（C-8）。
- **实测补强**：子代理把「三库 `pd_area` 行数」列为待验证 → 实测三库各 44703 行，
  这把 B-1 从"脚本写得危险"抬成了"一次误执行清掉 13 万行"。
- **实测补强**：`rule` 表 0 行（待验证 → 已证）把 B-3 从"运价真相不在主数据"抬成"计价当前返回 null"。
- **新增**：C-3（真实渠道回调被网关 401 拦死）是我在实测 `ignore.auth.url` 后补的，子代理只提了"需验证白名单"。

## 7. 图

- `support-domains-risk-map.dot`：四域风险关系图（rankdir=TB，风险节点用菱形，未证实关系用虚线）。

## 8. 代码引用的表 vs 线上真实表（全量对账，2026-10-08）

对 8 个有独立数据源的服务，把它们实体上的 `@TableName` 与线上 `information_schema`
（实测 163 张表）逐一对齐。服务→库的映射取自各自 prod dataId 的 jdbc url 实测：
`pd-oms/pd-druid/pd-netty→pd_oms`、`pd-work→pd_work`、`pd-base→pd_base`、
`pd-user→pd_users`、`pd-dispatch→pd_dispatch`、`pd-aggregation→pd_aggregation`。

| 缺失的表 | 谁的代码在用它 | 仓库里有 DDL 吗 | 严重度与后果 |
| --- | --- | --- | --- |
| `pd_work.pd_status_transition_history` | `StatusTransitionHistory.java:24` + Mapper + ServiceImpl，`TaskTransportServiceImpl:259,303` 发车/到达/派送都调它 | 原本没有；本轮补 `docs/sql/状态流转历史_建表.sql` | **P1**：`StatusTransitionHistoryServiceImpl:53-55` 的 catch 只记 error 并 return false，调用方不看返回值 → 业务不失败，但**状态流转审计一条都落不下来**，合规留证是空的（已建表，13 列与实体对齐） |
| `pd_oms.pd_payment_order` | `PaymentOrder`（pd-oms 支付单） | 有，但脚本 `docs/sql/pd_payment_order_建表.sql` 是他 agent 今天的**未跟踪文件** | **P0**：pd-oms 连的是 pd_oms，而这张表只存在于历史副本 `pinda_tms` → 创建支付单必然报 table doesn't exist。**归属：他们正在做，我不重复建** |
| `pd_oms.pd_settlement_order`、`pd_oms.pd_freight_detail` | `SettlementOrder` / 运费明细 | 同上，`docs/sql/财务结算域_建表.sql`（未跟踪，今天新增） | **P1**：即主报告"静默不结算"的另一半。归属同上 |
| `pd_oms.pd_alarm_record` | `AlarmRecord` + `GpsAlertService` + `AlarmController`（pd-netty） | 有脚本 `docs/sql/告警记录_建表.sql`（未跟踪），**但它 `USE pd_netty`** | **P1 + 一处配置错位**：pd-netty 的 datasource 实测是 `pd_oms`，而线上根本没有 `pd_netty` 库 → 这个脚本执行会直接失败，或把表建到没人连的地方。需要他们把脚本目标库改成 pd_oms（或同步改 pd-netty 的数据源），我没动 |
| `pd_dispatch.pd_schedule_exception_order` | 调度异常重试任务 | 有，且已跟踪（`docs/sql/pd_schedule_exception_order_建表.sql`） | **P1**：脚本进了仓库却没执行过 → `ScheduleExceptionRetryTask` 一跑就报错。属"落地漏了一步"，不是缺设计 |

另外两条对账时发现的事实：

- **`tms_order_location` 在任何库里都不存在**（163 张表全量比对）。而 `pd-druid` 的
  `DruidServiceImpl` 有 6 处 SQL 打它（`:46,64,80,92,119,131`），且 pd-druid 的 datasource 是
  `pd_oms`。实测直接调 `POST http://172.21.0.7:8193/apache-druid/query/select`，
  服务日志立刻出现 `Table 'pd_oms.tms_order_location' doesn't exist` —— 这个"报表服务"从上线起就一条都查不出来。
  它不能简单改名成 `pd_truck_location`：SQL 里还取了 `name/phone/licensePlate`，那些字段在车辆/司机表上，
  需要决定是改建视图还是改 SQL，属业务口径，留给你拍板。
- `pd-aggregation` 与 `pd-druid` 的实体上 `@TableName` 命中数为 0，说明这两个服务的表全部写在
  SQL/XML 里 —— 这类"代码里找不到的表名"正是对账最容易漏的部分。

## 9. 本轮已修复与验证台账

| 项 | 改动 | 验证方式与结果 |
| --- | --- | --- |
| C-2 支付回调可伪造 | `PayController` 接 rawBody+签名头，`WechatPayChannel` 缺签名一律拒绝 | 本地 `mvn -o -pl pd-oms -am test`：PayChannelTest 5 条 + PayServiceImplTest 6 条 + 新测试 4 条全绿；在线伪造探针待部署后跑 |
| C-4 退款谎报 | 两处桩 `return true` 改 `return false`（MockPayChannel 保持可用） | 同上测试全绿；`PayServiceImpl.refund` 的失败分支不写状态，账不会被污染 |
| A-5 菜单越权 | `/menu/router`、`/menu/my` 去掉 userId 查询参数 | 前端确认无人调用（router/index.js 用静态菜单，Login.js 只声明未使用）；在线探针待部署后跑 |
| D-5 日志存明文口令 | `SysLogAspect.desensitize()` + 6 条单测 | `mvn -o -pl pd-tools-log -am test`：Tests run 6, Failures 0 |
| B-2 区域 id 恒 false | 两处改 `StringUtils.equals(..., String.valueOf(id))` | 编译通过；能否真正分单还需一条真实订单（`receiver_county_id` 存的到底是主键还是 adcode 未证实） |
| B-3 运价规则 0 行 | 种子 SQL + 生成器 + `OrderAmountCalcRuleTest` | 线上 `pd_oms.rule` 现为 1 行（content 3923 字节），`:8186/rules/reload` 返回 ok，日志 `KieModule was added`；4 条价格断言（20/44/29/35）本地全绿 |
| D-2/D-4 前端契约 | `isBizError()` 按 code 判错；reset 改 POST + `ids[]` | `npx eslint` 0 问题；`vue-cli-service build --mode docker` 构建通过 |
| A-1 私钥出仓（机制） | compose 注入 `PINDA_JWT_*_PATH`、CI 入库守卫、deploy-check 第 8 节、§2.11 手册 | `docker compose config` 校验通过；守卫在本地正反两次试跑（当前状态通过并提示 2 把历史私钥；临时塞入假私钥被正确拦下）。**首次推送我把公钥也误判成违规，流水线停在第 3 步，已修** |
| B-9 百度 AK 缺失 | `x-baidu-env` 锚点接 6 个服务的 `JAVA_TOOL_OPTIONS` | compose 渲染实测 `-Dbaidu.map.ak=`（与今天等价，不改行为）；实际启用需要你填 AK |
| 新增：审计历史表 | `docs/sql/状态流转历史_建表.sql` 并应用到 pd_work | 自检 `information_schema` 返回 13 列 |

