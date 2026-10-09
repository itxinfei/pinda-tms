# 需求文档「删旧码就绪度」审计 — 2026-10-09

> **审计问题**：`docs/需求文档`（52 份）能否作为**从零重构的唯一事实源**，删掉旧前后端代码后仍能重建。
> **审计方法**：不判文档"好不好看"，只判一件事——**哪些事实只存在于旧代码/旧配置里**。每条结论均给 `file:line` 证据；文档声称"不存在"时一律回代码复核（本项目历史约 1/3 审查结论被推翻）。
> **审计基线**：commit `89971c5`（2026-10-08 23:10）+ 工作区 **64 个未提交文档路径**，`01/08/26` 三份文档在审计期间（10:56–10:59）仍被并发修改。**本文是快照，非冻结态。**

---

## 一、结论：分三层，不能一句话回答

| 层 | 状态 | 判据 |
|---|---|---|
| **① 业务口径层**（做什么、什么优先级、谁拍板） | ✅ **基本收口** | 《03-全局定案》§二 拍板类 0 项 / 收口遗留 0 项 / 业务议题 0 项，`P-01~P-04` 已升格 `D-41/D-42`，Q 系列 17 项升格 `D-43~D-59`，§一 计数与 §1.1~§1.13 分组之和自洽 |
| **② 契约与资产层**（接口契约、权威 DDL、计费/算法口径、事件语义） | 🟠 **主干已固化，覆盖盲区未闭合**（审计当日由 🔴 升级至此，见 §4.1） | **已闭合**：7 类"只在旧代码里"的知识中 5 类已入文档 —— 计费(B-1→《06》§6.5+《52》§一)、权限 DDL(B-2→`docs/mysql/`)、字典 schema(B-3→《52》§4.2)、新 P0 端点(B-4→《08》《43》§二·B)、事件语义(B-6→《52》§二+《09》C6 硬指针)；冲突项 C-1(7 档)、C-2(告警落点)、C-7(资源 URL) 已订正。**仍未闭合**：**B-5/C-4**（`pd_auth_resource` 383 条无权威导出 + 管理端 82 / auth 除登录 84 / dispatch 19 接口**未被需求文档覆盖**，G-H 之外最大盲区。⚠️ 本行原写的 "83/82/18" 三个数**全是错的**，第四轮按 `count-apis.js` 复跑订正，见 §4.4）、**C-5**（建表三源并行）、**C-6**（undo_log 仓库态/运行态分叉）。~~C-3~~ 已于同轮闭合（§4.2）。**新增第四轮口径**：《08》本身已由"看起来权威"转为"逐条可核对"（25 处订正 + 27 表块可渲染），所以 C-4 的性质从"接口清单不可信"收窄为"**接口清单可信、但业务规格仍缺**"——重构能照《08》对接，仍不能照需求文档解释这些接口为什么存在 |
| **③ 基线冻结层** | 🔴 **未冻结**（本轮改动进一步扩大） | 原 64 个未提交文档路径 + 本轮 G-B~G-G 全部改写（含 14 份文档 + 2 个 `deploy/ci`/`docs/sql` 产物）；重构开工前必须先 commit 一版"重构基线"，**且提交须由用户授权、逐路径 `git add` 核对** |

**一句话（2026-10-09 第四轮更新）**：**业务口径与接口契约可按文档重构；但 ① 权限资源清单（`pd_auth_resource` 383 条）与管理端/鉴权/调度三处接口的业务规格仍无文档源，② 基线未冻结，③ 定案与现网之间还有 §4.5/§4.7 两串执行层缺口。** 因此**仍不能删旧代码**——差的是 G-H（须活体环境，见 §4.7 M-1/M-2）、G-A（一次人工提交）、BA-19 落地（§4.5），以及 8 项人工实测。

---

## 二、只在旧代码/旧配置里，删了就会丢（逐条证据）

> 🟢 **2026-10-09 闭合状态**：**B-1 ✅ / B-2 ✅ / B-4 ✅ / B-6 ✅ / B-7 ✅** 五类已整体固化；**B-3 🟠 半闭合**（两表 schema 已反推入《52》§4.2，但"真源表名"仍系 `Q6` 待裁决 —— 旧后端从未实现字典功能，P1-21 方向须人拍板）；**B-5 ⛔ 仍开放**（`pd_auth_resource` 383 条无权威导出，须 MySQL 访问，见 G-H）。以下原文为审计当日快照，保留作证据链。

### B-1 运价真值与进位口径 —— 最关键的一条
- **费率卡本体**在 `pd-oms/src/main/resources/rules/orderAmountCalc.drl`：首重 1kg 内 20 元；续重按距离三档 **≤200km→6 元 / ≤500km→9 元 / >500km→15 元**（salience 10 起 4 条规则）。
- **进位口径**在 `DroolsRulesServiceImpl.java:17-31`：`totalWeight.subtract(firstWeight)` 后 `setScale(0, ROUND_DOWN)` → **续重不足 1kg 直接截断不计费**。文档零处记载。
- **距离来源**在 `OrderServiceImpl.java:240-301`：`getDistance()` 调百度地图把地址解析成坐标再算里程；解析失败返回 `{amount:"0", errorMsg:"无法计算订单距离和订单价格，请输入真实地址"}`（`OrderServiceImpl.java:247`）；`orderCargoDto`/`distance` 缺失或 `KieContainer==null` → **返回 null**（前端拿到空响应，语义未定义）。
- `docs/sql/运价规则_种子_orderAmountCalc.sql` 保留了 .drl 全文，**但它自己声明"单一真源 = .drl"，再生成脚本 `deploy/ci/gen-drools-seed-sql.js` 也在待删代码树里** → 删码即断源。
- ⚠️ 附带纠正：《21-财务结算与调度运力IoT任务卡》line 38 判 `DroolsRulesServiceImpl#calcFee` 为"死代码（无调用方）"——**判错**，.drl 每条规则都在调它。

### B-2 权限域权威 DDL 锁在待删树内
- `pd-authority/pd_auth.sql` 定义 **11 张表**（`pd_auth_menu / pd_auth_resource / pd_auth_role / pd_auth_role_authority / pd_auth_role_org / pd_auth_user / pd_auth_user_role / pd_common_login_log / pd_common_opt_log / pd_core_org / pd_core_station`）。
- 《00-索引》声明的 `docs/mysql/TMS项目建库脚本/` 只有 6 个脚本、**无 pd_auth**、无 `create database`、**0 条 INSERT**。《45-数据库表结构设计》§1.5 只列 pd_auth 表名+关键字段（**清单，非 DDL**）。
- `pd-authority/pd_goods_info.sql` 同理。
- 另：`docs/mysql` 建库脚本内 **无 `undo_log` 表**，而 `@GlobalTransactional` 已散落在下单/调度链路（《08》§10-10）→ 删码后连"要建什么表"都无权威答案。

### B-3 字典表 schema 彻底无文档
- `c_common_dictionary` / `c_common_dictionary_item` 只存在于 `pd-authority/pd-apps/pd-auth/pd-auth-entity/.../common/Dictionary.java`、`DictionaryItem.java` 两个实体类；**全库 SQL 无 DDL**。
- 而《22-管理端缺失模块需求清单》提议**新建** `pd_auth_dictionary` / `pd_auth_dictionary_item`，字段表是**从 Vue2 页面 `pd-admin-ui/src/views/pinda/base/dict/*.vue` 反推**的（这些页面也在待删清单里）。
- 与 `BA-18` 的定案"**禁止新建第二套字典服务/字典表**，落点 `pd-auth`"正面冲突 → `P1-21` 的真源表名未定。

### B-4 新补的 P0 端点，接口文档没跟上
| 端点（实测代码） | 文档现状 |
|---|---|
| `POST /api/web-courier/courier/exception/report`（`CourierController.java:796`） | 仅《02-优先级总览》line 123 验收项提到；**《08-后端接口文档》《43-前端页面接口对接清单》无该行** |
| `POST /api/web-driver/business/cargo/exception/report`（`CargoController.java:792`，类映射 `business/cargo`） | **文档中该路径一次都没出现**；SQL 与 curl 用的是另一条路径（见 C-7） |
| `GET /api/netty-service/alarm` · `PUT /alarm/{id}/handle` · `POST /alarm/report`（`pd-netty/.../AlarmController.java:38/56/97/132`） | 《08》line 232 只有 `POST /alarm/report` 一行；**查询/处理两条未列** |
| `ExceptionType` 枚举（7 值：`GOODS_DAMAGED/REJECTED/ADDRESS_ERROR/VEHICLE_BREAKDOWN/CARGO_LOSS/DELAY/OTHER` + `isCourierScoped()` 归属判定） | 全库文档仅出现在 `docs/sql/三端异常上报接口_资源注册_20261008.sql`，**《07-数据字典与枚举口径》（自称跨端唯一真源）未收录** |
- 《51-移动三端建设规划》line 24/25/87/99/138 **仍写"🔴 API 不存在，须先补"**，而《00-索引》line 117 已写"已补齐（`89971c5`）"→ 同一条 P0 在两份文档里状态相反。

### B-5 网关资源清单（383 条）只有增量、没有权威导出
- 现路径：`docs/sql/阻断C_三端接口注册与账号闭环.sql`(65) + `管理端接口资源注册_20261007.sql`(335) + `前端缺失接口补齐.sql`(33) + `pd_auth_resource_注册模板_阻断C.sql`(12) + `三端异常上报接口_资源注册_20261008.sql`(4) —— **增量脚本堆叠**，无一份"当前 383 条全量 dump + 角色绑定"。
- 《08》§9 对内部 Feign 服务明确"**仅列代表**"（~153 接口未穷举）→ 重构时无法判定聚合层该暴露什么。

- → **"订单确认/揽收完成/派送完成 → 触发结算"这条业务事件契约只存在于待删代码里**，文档无一张事件表（谁生产、payload、幂等键、重试）。

### B-7 枚举与状态覆盖不全
- 《07》§十二自称"源码逐条提取 **16 个枚举类**"；代码实测约 **32 处枚举声明**，其中 `CoordSystem`、`LocationSource`、`ExceptionType`、`ScheduleParams` 等**未进《07》**。
- 《00-索引》§五仍挂 8 条"SRS 有规格、FR 无编号"（`DSP-04/TRK-03/DIS-04/DRV-04/CST-02/PER-01/PER-02/SEATA-01`）+ 6 条"有优先级、无抓手"（`FR-69/70/72/73/74/76`）→ 重构按抓手表施工会整域漏做。

---

## 三、文档内部冲突（会直接把重构带偏）

> 🟠 **2026-10-09 闭合状态**：**C-1 ✅**（7 档归一，三处真源互锁）、**C-2 🟢 执行口径已统一**（四份文档改到 pd-netty 事实；`D-05` 定案文本未动，标「⚠️ 复审中」待拍板 A/B）、**C-3 ✅**（《06》§5.1 已按代码订正动词，与《08》§6.1 一致；顺带扫出并修掉 6 组同类动词/漏项，见 §4.2）、**C-7 ✅**（脚本侧 URL 已订正 + 资源注册 SQL 就绪，执行仍须人工）、**C-5 🟠**（告警表 11 列 2 态 vs 19 字段 5 态这组例证已判负并作废，但三源并行的结构性问题未治）、**C-4 ⛔ 未处理**（但性质已收窄：第四轮把《08》本身逐条核对并订正 25 处，见 §4.4，剩下的缺口是"接口无业务规格 + 资源清单无权威导出"，不再是"接口清单不可信"）、**C-6 ⛔ 未处理**。

### C-1 🔴 客户端状态档位：4 档 / 6 档 / 7 档 三口径并存，且新表放错文档
| 文档 | 口径 |
|---|---|
| 《01-业务需求全景》line 146–160 | **7 档**，2026-10-09 拍板，**并给了完整映射表**（23000 / 23001-23004 / 23005-23006 / 23007-23008 / 23009 / 23010 / 23011） |
| 《03-全局定案》line 391 `D-48` | **6 档**（决策台账，未随 10-09 拍板更新） |
| 《06-开发规范》line 486–487、《51》line 76、《49》line 69/169 | **6 档** |
| 《07-数据字典》line 47–48、line 292（自称**跨端唯一真源**，§十二验收项写"客户视图 **4 档**由查表实现"）、《09》line 76/147/152/217（"3~4 档"，且把 23001 塞进"运输中"）、《27》line 128（断言"客户看到 **4 档**"） | **4 档** |
- 后果：CST-01（P0）的展示层无单一真源；**枚举真源文档《07》恰恰是最旧的那版**。
- 《09》line 207 已自证 4 档口径的缺陷（"客户看不到运输中档位…滞后率=100%"）。

### C-2 🔴 告警中心落点：台账定案 D-05 与代码相反
- 《03》line 32 `D-05`：告警 = `pd-work` 内部实现 + **`pd-web-manager` 新建 `AlarmController`** 暴露，网关 `/api/web-manager/alarm/*`。
- 代码事实：`AlarmController` **只在 `pd-netty`**，全库 `pd-web/` 下无 alarm Controller（实测 `find` 仅 1 处命中）。
- 《26-联调就绪检查清单》line 142 已于 2026-10-09 订正："`/api/web-manager/alarm/*` 下 7 条路径**全部系虚构，已删除**"，真实为 `GET/PUT/POST /api/netty-service/alarm...`。
- 但 **《15-管理端任务卡》line 123/125–132、《20-运营可视化任务卡》line 150–167、《27-链路测试》line 368–382、《25-子任务验收 CSV》T-051-4/5 仍按虚构契约写**（含 4/5/7 条接口数量三处不一，`EC-07` 早已提出）。
- 《02》line 129 同时记 `pd_transport_alert`（19 字段/5 态/7 接口）**已废弃，以代码 `pd_alarm_record` 为准**（10-09 拍板）→ 废弃方案在执行层文档里仍是活契约。

### C-3 🟠 `06` 与 `08` 的 HTTP 动词不一致
《06》line 224 写 `POST {id}/pay`、`POST {id}/cancel`；《08》§6.1 实为 `PUT /mailing/pay/{id}`、`PUT /mailing/cancel/{id}`。《08》有取代声明但《06》未回写。

### C-4 🟠 接口文档自身的覆盖缺口被写在文档里
《08》§11.1 自认：pd-web-manager 83 接口、pd-auth 除登录外 82 接口、`PayController`、pd-dispatch 18 接口 **未被需求文档覆盖**。管理端重构只能靠《08》，而《08》§9 又声明内部服务"仅列代表"。

> 🔎 **第四轮订正（2026-10-09，`count-apis.js` 复跑口径：方法级映射注解，剔除类级与 `/feign/`）**：三个数**都错了**——正确为 **manager 82（13 控制器）/ pd-auth-server 除登录 84（总 91 减 `LoginController` 7）/ pd-dispatch 内部 19**（`ScheduleJobController` 13 条 + 其余控制器 6 条，已逐条列举在《08》§九）。旧数来源无法回溯（写作时未留 `file:line`），这正是本审计要求"每条计数必须给可复跑入口"的原因。《08》§11.1 与 §八已同步订正，并给出 14 个控制器的加总等式，任何人可复跑核对。
> **本条性质因此收窄**：不再是"接口清单不可信"，而是"**清单已可信、业务规格仍缺**"——照《08》能对接，但照需求文档说不出这些接口为什么存在。补齐须为新写 SRS 章节，属**人工决策**（不在本文可自动闭合范围）。

### C-5 🟠 字典/报表建表口径三处并行
《20》《21》卡内嵌 DDL、《45-数据库表结构设计》、`docs/sql/*_建表.sql` 三源并存，同一张表字段数/状态数不同（告警表即例证：`pd_alarm_record` 11 列 2 态 vs `pd_transport_alert` 19 字段 5 态）。

### C-6 🟡 `undo_log` / Seata 仓库态与运行态口径分叉
《08》§10-10 记"未建 undo_log、`@GlobalTransactional` 是假分布式事务"，而《03》§三 `T-02` 记"已标 `@GlobalTransactional`，上线前二选一（部署 Seata 或显式降级）"。开发机上 Seata TC 已跑通（见项目记忆 `seata-tc-naming-group`）。**删码前须写清"仓库里是什么 / 运行环境里是什么"，否则重构会照搬错误基线。**

### C-7 🔴 司机在途上报的资源注册 URL 写错，网关必拒（可验证）
- 真实路径：`CargoController.java:68` `@RequestMapping("business/cargo")` + `:792` `@PostMapping("exception/report")` → 服务内 `/business/cargo/exception/report`。
- 注册 SQL：`docs/sql/三端异常上报接口_资源注册_20261008.sql:41` 写 `'/cargo/exception/report'`，line 79 的 curl 示例写 `/api/web-driver/cargo/exception/report`。
- 既有惯例：`pd_auth_resource_注册模板_阻断C.sql:136` 注册的是 `/business/cargo/wait`（含 `business` 段）。
- 匹配逻辑：`AccessFilter.java:145` `permission.equals(resource) || permission.startsWith(resource + "/")`，`permission = method + 去前缀后的 requestURI`。`POST/business/cargo/exception/report` 既不等于也不以 `POST/cargo/exception/report/` 开头 → `count==0` → **判"未知请求"拒绝**（fail-closed，`AccessFilter.java:76-80`）。
- 结论：**D-57 这条 P0 接口即使代码已补、SQL 已执行，也会在网关被拒**。快递员侧 `/courier/exception/report` 路径正确（类映射即 `courier`）。**是否已入库未实测**（需查 `pd_auth.pd_auth_resource`），但 URL 本身必须订正。

---

## 四、删旧码前必须完成的固化动作（执行状态见本节末「§4.1」）

> 依 CLAUDE.md："与权威文档冲突的优化默认无效，须先修订文档并经人工确认"。以下每条都改动 A/B 层权威文档。2026-10-09 用户指示"你继续"后按 Auto Mode 执行了可纯凭代码事实闭合的条目；**凡触及定案（`D-xx`）文本的，一律只标注冲突、不改动定案**（见 `D-05` 复审中）。

| # | 动作 | 落点 | 完成判据 |
|---|---|---|---|
| G-A | **冻结基线**：把 64 个未提交文档路径按逐路径 `git add` 核对后 commit 一版"重构基线" | git | `git status --porcelain -- docs` 为空 |
| G-B | **客户档位三源归一**：确认 7 档为最终口径 → 更新 `D-48`（《03》）、《06》§CST-01、《07》§一+§十二验收项、《09》§4.3+line 76/147/152/217、《27》line 128、《51》line 76 | 《03》《06》《07》《09》《27》《51》 | 全站 `grep "4 档\|6 档"` 只剩历史留痕；映射表只有一处真源 |
| G-C | **运价口径入文档**：首重 20 元 / 续重 6·9·15 三档 / `ROUND_DOWN` 截断 / 距离来自百度地图地址解析 / 失败返回 `amount=0` 与 null 两种语义 —— 抄《07》或新建运价规格节；.drl 全文随库入 `docs/`，声明新真源 | 《07》或《06》+ `docs/` 内 .drl | 不依赖 `pd-oms/` 与 `deploy/ci/` 即可重算同一笔运费 |
| G-D | **DDL 顶层化**：`pd-authority/pd_auth.sql`、`pd_goods_info.sql` 复制进 `docs/mysql/`；补 `create database`、`undo_log`；`c_common_dictionary(_item)` 从实体反推 DDL 入库并裁定 `P1-21` 真源表名 | `docs/mysql/` +《22》《45》 | 新库脚本可独立建库建表，含权限域 |
| G-E | **接口契约补全**：《08》§5/§4 增 `POST /courier/exception/report`、`POST /business/cargo/exception/report` 两行；§7 补齐 `GET /alarm`、`PUT /alarm/{id}/handle`；`ExceptionType` 7 值入《07》；《43》《51》同步页面映射 | 《08》《07》《43》《51》 | 移动端照《43》可写出调用代码，无需读 Java |
| G-F | **告警落点归一**：把 `D-05` 改为 pd-netty 事实（或按新决策重建 web-manager 层），同步《15》《20》《27》《25CSV》，删除 `pd_transport_alert` 活契约 | 《03》《15》《20》《26》《27》《25》 | 全站仅一套 `/alarm` 契约且与代码一致 |
| G-H | **资源注册权威化**：导出 `pd_auth.pd_auth_resource` 全量 + 角色绑定为一份 `docs/sql/` 权威脚本（含 Nacos 网关路由实测，解 `EC-12`/`T-03`）；同时订正 `/cargo/exception/report` → `/business/cargo/exception/report` | `docs/sql/` +《08》§2 | 网关放行清单可从文档重建 |
| G-I | **抓手盲区补齐**：8 条无 FR + 6 条无抓手，逐条补号或显式记"不做" | 《02》《04》《05》《06》 | 《04-需求编号对照表》无悬空项 |

### 4.1 执行状态（2026-10-09）

| # | 状态 | 实际落点与留痕 |
|---|---|---|
| G-A | ⛔ **未执行（须人工）** | 工作区有并发文档编辑者，且提交须由用户授权。基线仍未冻结——这是本文 §三 结论"③ 基线未闭环"仍然成立的唯一原因。 |
| G-B | ✅ 已执行 | 7 档归一：《03》`D-48` 行 + §7.4 对照行、《06》§6.8 ⑤/验收、《07》§一、《09》§4.3 整表重写 + line76/220、《49》两处加注；过程记录见《52》§5-Q5 |
| G-C | ✅ 已执行 | 算价口径进《06》§6.5（三入口/运价卡/`activation-group`/`ROUND_DOWN`/返回 String/百度距离/两种失败语义/规则热加载）；`.drl` 幸存副本由 `deploy/ci/gen-drools-seed-sql.js` 头注自动生成，改生成器而非改产物，重跑不丢警告 |
| G-D | 🟠 **DDL 部分已执行；表名裁决仍须人工** | `docs/mysql/TMS项目建库脚本/pd_auth.sql`（权限域 11 表，DDL 逐字保留、剔除 248 条 2020 演示 INSERT 含 `pd_auth_user` 口令散列）与 `pd_goods_info.sql` 已顶层化，`create database`/`undo_log` 已补（`grep -l undo_log docs/mysql/` 命中）。**未完成的是原判据里的"裁定 `P1-21` 真源表名"**：`c_common_dictionary(_item)` schema 已反推进《52》§4.2，但与《22》提议的 `pd_auth_dictionary(_item)` 冲突属 `Q6`，须人拍板（旧后端从未实现字典功能，无既有实现可靠引用）。 |
| G-E | ✅ 已执行 | 《43》新增 §二·B 快递员端全量页面→接口（登录复用 `/api/auth/anno/login`、列表 DTO 陷阱、`@Deprecated` GET、D-46 三要素、禁走 netty 告警口）；《08》《07》《51》同步 |
| G-F | ✅ 已执行（定案未改） | 《20》§2.3 整节降为"历史方案·落点已作废"并新增"✅ 现存真实契约"（`pd-netty` 三接口 + `pd_oms.pd_alarm_record` 11 字段 / 0·1 两态）；§2.4 首条改为 `GpsAlertService` 实测（同进程落库、**无 Feign**）；§2.5 DoD 拆 A 组(现存)/B 组(作废留痕)；§2.6 标注 `NotifyController` 零实现。《25CSV》T-051-1/-2/-3/-4/-5 按事实重写（-4 作废、-5 待注册 7→3）。《15》§六、《26》§2、《27》§8 此前已订正。**`D-05` 定案文本未改**，按《03》§六流程标「⚠️ 复审中 · 申请复审 2026-10-09」并列替代方案 A/B → **须人工拍板** |
| G-G | ✅ 已执行（落点改到《52》§二，非原计划的《09》/《08》） | 事件契约四小节：§2.1 总线拓扑（topic exchange `pinda.domain.event.exchange`、routingKey = `eventType`、3 队列 + DLQ、发布重试 3×2s）、§2.2 生产方（全仓库仅 3 个发布点）、§2.3 消费方与**真实副作用**（含"揽收/签收→应收结算单"链路与注释不符项）、§2.4 跨模块幂等去重口径。原计划写入《09》，实际落点为《52》§二；已于同日在《09》C6 补"事件契约真源指向《52》§二"硬指针，缺口闭合。 |
| G-H | ⛔ **未执行（须人工）** | 导出 `pd_auth.pd_auth_resource` 全量与角色绑定、Nacos 路由实测均需 MySQL/Nacos 访问；Auto Mode 下远程 `ssh` 被拦截，本审计不声称已完成。`/cargo/exception/report` → `/business/cargo/exception/report` 的**脚本侧订正已完成**（《52》§3）。 |
| G-I | ⏸ 未执行 | 8 条无 FR + 6 条无抓手未动；属编号空间治理，不阻塞契约层。 |
| G-J | ✅ 已执行（2026-10-09 第二轮，由用户提问触发） | **两件事**：① C 层删除后的悬空引用清理 + 《00-索引》作废编号规则 + 《03》§一"依据列读法"与 §七"唯一定义源"升格（详见 §4.3）；② **回答并固化"为什么有两套 MQ、重构选哪套"** → 新建 **《47》§1.2.1**（分工对比 + 方案 A/B + 不推荐项），并在《00-索引》《03》BA-09《52》§2 前言与 §5-Q2 四处指过去；同时发现并修正《47》§1.2 中间件表**原本根本不列 RabbitMQ**（这才是"文档里怎么有两个 MQ"看起来矛盾的物证），补其行并标"仍在用/本轮不清理"。⚠️ 顺带发现 BA-09 的"P0-7d 维持 2–4 人日"**不含订单事件迁移**，代价被低估——已在《03》BA-09 状态列与《52》Q2 登记，**改口径须人工拍板**（动定案）。 |
| G-M | ✅ 已执行（2026-10-09 第四轮，用户"检查文档是不是还有问题"触发） | **三类成果**：① **《08-后端接口文档》代码断言逐条自证后订正 25 处**（全部带 `file:line` 证据，明细见 §4.4）；② 顺带挖出比内容错误更严重的**结构性缺陷**——《08》35 个表块中 **27 个缺「表头 + 分隔行」，即 §三~§八 的主体接口表此前根本无法渲染**（读者看到的是普通段落，表格层级全丢）。已补齐 27 组表头，并把该缺陷抽象成"表格结构族"用脚本扫遍 49 份活文档，再清掉 12 处（涉及 7 份：《01》《15》《20》《21》《27》《49》《52》），复扫 49/49 **问题数归零**。③ **可复跑口径已入库**（此前只存在于临时目录，会话一结束就丢）：新增 `docs/architecture/scripts/count-apis.js`（接口数：剔除类级 `@RequestMapping` 与**任何带 `@FeignClient` 的文件**）与 `docs/architecture/scripts/lint-md-tables.js`（表格结构：跳过代码围栏，**退出码非 0 即可进 CI**）。本轮复跑逐字复现 6 个模块计数：manager **82/13**、driver **11/4**、courier **16/5**、customer **20/8**、netty **8/3**、pd-auth-server **91/14**；§九 内部 Feign 六模块 **64+24+24+19+6+17 = 154** 与本文头部数字加总闭合。 |

**本轮新增的三类删码风险**（原文档未识别）：
1. 🔴 `DroolsRulesServiceImpl#calcFee` 被四份文档判为"死代码可清理"，实际调用点在 `orderAmountCalc.drl:33/50/67` 的 `then` 块里——删该类则三条续重规则运行期失败、全站算不出价。已订正《21》《06》《08-Java后端升级方案_实测基线》（原第四份《36》已于同日删除），已在《06》line408 与《21》line38 留有完整方法论表述（"Java 侧 grep 看不到 `.drl` 里的引用 ⇒ grep 无调用方 ≠ 死代码"）；曾同记此教训的《36-需求文档体检报告》已于同日随 C 层归档删除，见 §4.3。
2. 🔴 《31-升级手册》§3 原写"直接 `docker stop rabbitmq && docker rm rabbitmq`"，而 RabbitMQ 仍承载短信通知与"签收→应收结算单"链路（`docker-compose.infra.yml:180` + 4 个 `@RabbitListener`）。已改为 ⛔ 暂缓 + 5 项下线前置条件。
3. 🔴 **"规划落点"被写成"现成契约"是复现缺陷类**，且带 `✅ 唯一可用` 字样时最危险。本轮同族清点结果：`D-05`(alarm) ✅ 已降级、`D-06`(`DispatchBoardController`，`grep -rl DispatchBoard --include=*.java` 零命中) 与 §2.6/《20》`NotifyController`（零命中，且两张表连建表脚本都不在 `docs/sql/`）已标"待建/勿对接"；`D-07`(courier AttachmentController) 实测**已落地**，其"待建"旧口径反向作废。
   - **2026-10-09 第三轮新增同族成员（本族当前风险最高）**：`OrderApi.updateStatus()`。《09》全文 6 处（§7.2.2 契约表、§7.3 时序、§7.4 依赖清单、§八 风险表 ×2）用它作为状态同步桥的落库出口，并在 §7.4 标 **`✅ 已存在`**。实测：全仓库**无 `OrderApi` 类**、**无 `updateStatus` 方法**（两条 `grep` 均零命中）；pd-oms 真实 Feign 是 `OrderFeign`（`path="/order"`），现存唯一状态回写通路是通用更新 `PUT /order/{id}`（`OrderController.java:137-157`，服务层 `OrderServiceImpl.updateById()` 做状态机校验、非法流转显式抛 `PdException`）。**为何最危险**：P0-1（状态同步桥）是三端状态一致性的地基，若照"已存在"实现，落地时会对接不到接口，而文档里看不出错。已在《09》§7.2.2 补红字：落地二选一（① 复用 `OrderFeign.updateById`，只塞 `status` 字段；② 新增专用 `/order/{id}/status` 并登记《08》与 `pd_auth_resource`），选择后须回写本节。

### 4.2 HTTP 动词/漏项全量清扫（2026-10-09，C-3 触发）

按"问题族"而非单点修：逐 Controller 把《06》§5.1~§5.4 与源码 `@*Mapping` 对照，**6 组错误**已订正（每处都带 `file:line`）：

| 位置 | 原文档 | 代码事实 |
|---|---|---|
| 《06》§5.1 mailing | `POST pay/{id}`、`POST cancel/{id}`、`POST {id}` | **PUT** `pay/{id}`(`:778`)、**PUT** `cancel/{id}`(`:807`)、新建是 `POST ""`(`:177`)、修改是 **PUT** `{id}`(`:597`) |
| 《06》§5.1 address | 只列 GET page / POST / GET detail | 漏 **PUT** `{id}`(`AddressBookController:123`)、**DELETE** `{id}`(`:139`) |
| 《06》§5.1 user | `POST auth/query/id` 当对外端点 | 那是 `feign/UserClient.java:15` 的**内部 Feign 声明**；`UserController` 只有 `profile` |
| 《06》§5.3 courier | 11 个路径**全部无动词** | 补 GET/PUT/POST 逐条（`CourierController:118/187/219/286/395/436/482/563/575/670/772/796`），并补漏列的 `POST exception/report`(D-41) |
| 《06》§5.3 attachment | 整行缺失（旧口径"快递员无附件"） | `AttachmentController` **已存在**（D-07 已落地） |
| 《06》§5.4 netty | `GET /trace/page`；无告警、无 byTasks | `/trace/page` 是 **POST**(`GpsTraceController:129`)；补 `GET /trace/byTasks`(`:102`) + 告警三接口整组 |

**本轮自纠**：我在写《06》《20》告警契约时一度把分页参数写成 `pageNum`，回读 `AlarmController.java:59-66` 后订正为 **`page`**（并补上越界返 400、`pageSize≤200`）。教训：**新写的契约行也必须回读方法签名，不能凭其他模块的参数名类推**。

### 4.3 ✅ C 层过程归档已被整体删除 —— 已确认为有意收敛，悬空引用已清理（2026-10-09 收口）

**状态变更**：用户已明确说明这批删除是他本人所为，理由是"我认为都是过期的，担心误导大模型"（原文见本文 §六 追加）。因此本文**不再把它当待观察风险**，转为按"有意收敛"处理：不回退、不恢复、只清理活文档里指向已删文件的指针。

**可恢复性（已实测，非推断）**：`git ls-tree -r HEAD` 显示 42 个删除路径中，`_需求审查归档/` + `_调研归档/` + 《36》 + 《50》 共 **37 份仍在 `HEAD`（commit `89971c5`）的树里**，状态为 `D `（已暂存删除、尚未提交）。即：**这些文件现在仍可无损找回**（`git restore --source=HEAD --staged --worktree -- "docs/需求文档/_需求审查归档"`），删除一旦提交则须走 `git log --diff-filter=D` 取回。⚠️ 这仍是**提交前的一次性窗口**——提交后过程留痕（谁在何时基于什么拍板）从"可查"降级为"只有《03》的一句话摘要"。

**本轮已执行的清理（逐条回读，非批量替换）**：
- 《00-索引》编号约定新增第 3、4 条：**36/50 编号作废不可复用**（目录里 35→37、49→51 的断号是有意保留，不是丢文件）+ C 层已删的全量影响说明与"新过程结论直接登记《03》/《00》，不要再新建 C 层报告"的替代路径。
- 《03》§七 前言：原句"定义全部只存在于审查报告正文里"在归档删除后**已成假命题**，改为"本节从'唯一真源'升级为'唯一定义源'，出处不可回溯，不得以'审查报告原文'主张不同释义"；§7.1 三列表头（总报告/框架层/执行层）标注为**历史命名、源文档已不存在、不要去查**。原 7.2/7.3/7.4 三行"定义出处：`_需求审查归档/…`"已在工作区被移除（非本文操作），本文复核确认已清零。
- 《04》line155：《需求文档体检报告》§四 → 改为"依据本文对 SRS §5/§6 的逐条核对"，并注明《36》已删、编号作废。
- 《47》line35：`见《36》line72` → 改为仓库可自证的 `docker-compose.infra.yml:151` + 带日期的快照引用。
- 《52》§2 前言 / §5-Q2、《31》§3、《00-索引》"环境变量"条目：把"《07-项目现状基线》实测在跑 rabbitmq 3.8"标注为 **2026-10-07 快照、早于本轮中间件升级、不可当现状**（同时消除与《07-数据字典与枚举口径》的同名歧义——它实际在 `docs/` 而非 `docs/需求文档/`）。
- 复核 `grep` 全量活文档：**已无任何指向 `_需求审查归档` / `_调研归档` / 《36》 / 《50》 的悬空指针**；`[.md](.md)` 链接与《NN-xxx.md》书名号引用经脚本逐条测存在性，**零断链**。
- 《06》内两处 `_调研归档/…`（行政区划层级方案、物流项目功能需求分析）在清理过程中已由并发编辑者移除（本文复核为 0 命中，未重复改动）。



### 4.4 本轮《08》25 处订正（22 处内容 + 3 处渲染转义，每条都回读过方法签名，非批量替换）

按缺陷类别归组，"证据"列即复核入口：

| 类别 | 处数 | 代表性订正（原文档 → 代码事实） | 证据 |
|---|---|---|---|
| **接口计数** | 6 | pd-auth-server 88 → **91**；内部 Feign 合计 ~153 → **154**（并按 §九 逐模块加总 `64+24+24+19+6+17` 闭合）；pd-dispatch 内部接口 18 → **19**；`/role` 13 → **14**；§11.1 自认清单里的 "pd-auth 除登录 82" → **84**（= 91 − `LoginController` 7）、"manager ~82" → **82（13 控制器，实测）** | `count-apis.js` 复跑（见 §4.1 G-M ③），逐控制器加总等式已写进《08》§八前言与顶部复核行 |
| **返回类型** | 4 | `POST /order-manager/order/{id}` 由 `Result` → **OrderVo**；`PUT .../cargo/{id}` → **OrderCargoVo**；`PUT .../transport-task/{id}` → **TaskTransportVo**；快递员 `/common/area/simple` 由 `List<AreaSimpleVo>` → **Result**（与管理端返回裸 List **不一致**，已加 ⚠️） | `OrderController.java:109-110`、`CargoController.java:78-79`、`TransportTaskController.java:136-137`、`CommonController.java:34-35` |
| **入参与参数名** | 4 | 三端 `POST /attachment/upload` 文档写了根本不存在的 `businessType` 入参 → 实际**只有 `file` 一个参数**，`isSingle/id/bizId/bizType` 由服务端硬编码（`false`/`null`/`null`/`"driver\|courier\|customer"`）；`/agency/user/page` 补驼峰必填 `pageSize` 与可选 `agencyId`；`GET /anno/token` 由 `—` 补成 account+password 必填 + `@Deprecated` + **默认拒绝**（prod 直接失败，非 prod 须开关 `pinda.anno.get-token.enabled=true`）；`transportLine/trips` 由"列表/分页"改为"**非分页**，返回 `List<TransportTripsVo>`" | 三份 `AttachmentController.java`（签名一致）+ 各自 `feign/AttachmentClient.java`；`AgencyController.java:109-112`；`LoginController.java:103-115`；`TransforCenterBusinessController.java:852-855` |
| **路径完整性** | 2 | `/business-hall/courier/scope` 拆两行：GET **必须带 `/{id}`**，`/courier/scope` 无 GET 映射；机构用户分页拆行 | `BusinessHallController.java:226/308`、`AgencyController.java:92/109-112` |
| **端点归属与断言纠偏** | 3 | `/enums` 的**实际应答方是网关**（再 Feign 取 auth-server 数据），不占 `/api/authority/**` 前缀；撤销原文"该路径是重构前旧契约、**本项目后端从未提供**"这句**假断言** → 网关**确有** `GET /dictionary/enums`，404 的唯一原因是多写的 `/gate` 段没有路由；新增 `GET /api/dictionary/enums` 一行 | `GeneratorController.java:24-25`（**无类级 `@RequestMapping`**）与 `:66`、`:93-97`；`AuthorityGeneralController.java:35/44/62` |
| **缺失端点补录** | 3 | `POST /anno/loginTx`、`POST /anno/logout`（"**登出必须调它**，仅前端清本地 token 无效"）；`/anno/login` 补验证码必填；**网关 `GET /{service}/v2/{ext}` Swagger 跳转代理**（`GeneratorController:48-57`）——补录后《08》§8.3 记"**网关自身对外端点 = 3**"，并写明 `zuul/api/ResourceApi.java` 的 2 条 `@GetMapping` 是 `@FeignClient` 声明、**不得计入接口数** | `LoginController.java:65/84/125/141`；`GeneratorController.java:48`；`ResourceApi.java:11-19` |
| **渲染与转义** | 3 | 《08》27 个表块补表头分隔行；`Result(Record\|null)` 裸竖线转义；顶部新增"全量复核口径"说明行（按**方法级**映射注解计数、类级不计、Feign 剔除） | `lint-md-tables.js` |

> 🔴 **本轮最重要的一条不是错字，而是"看起来权威"**：《08》是移动端/前端唯一契约源，它此前**既数错接口、又把不存在的入参写得像真的、还有一整句"后端从未提供"是反向假断言**。三类错误后果不同：计数错 → 漏做接口；参数错 → 对接报 400；假断言 → 重构时**不去实现一个真实存在的端点**。
>
> 🔧 **`count-apis.js` 的两次口径修正（工具与文档是互相校出来的，不是一次到位）**：
> 1. 首版把 `pd-web-courier` 数成 **18**（与文档 16 冲突）——差异全部来自 `feign/AttachmentClient.java`、`feign/ExceptionReportFeign.java` 两条 `@FeignClient` 声明 ⇒ 加"**目录含 `/feign/` 整文件剔除**"规则后复现 16。
> 2. 再数网关得 **5**（`GeneratorController` 3 + `zuul/api/ResourceApi` 2）——后者在 `api/` 目录、按目录规则漏剔除，但文件里确有 `@FeignClient` ⇒ 规则升级为"**按 `@FeignClient` 注解整文件剔除**"（更可靠）。改后**十个模块既有数字全部不变**（manager 82 / driver 11 / courier 16 / customer 20 / netty 8 / auth 91 / dispatch 19 / oms 24 / work 24 / base 64），只有网关从 5 收敛为 **3**。
>
> **教训**：口径类规则要写成"注解级"而不是"目录级"，否则换个目录命名习惯就漏。

### 4.5 🔴 BA-19「统一存储 WGS84」：口径已闭合、**落地一件没做**（本轮新发现的执行层缺口）

《03》BA-19 原判"✅ 已闭合"，实测**闭合的只是口径**：目标态"轨迹统一存 WGS84"在 DDL 与代码里**零落地**，四处仍缺省 BD09。

| # | 位置 | 实测事实 |
|---|---|---|
| 1 | `docs/sql/P0-4_北斗字段最小改造.sql:32/:75` | `coord_system varchar(16) NOT NULL DEFAULT 'BD09'` |
| 2 | `docs/sql/pd_truck_location_archive.sql:35` | 同一列写成 `DEFAULT NULL` ⇒ **两份建库脚本互相矛盾**，新环境初始化会随执行顺序得到不同缺省 |
| 3 | `pd-netty/.../NettyController.java:56-58` | HTTP 入口不传 `coordSystem` 时兜底 `BD09` |
| 4 | `pd-netty/.../NettyServerHandler.java:104` + `LocationEntity.java:72/74` + `LocationRecord.java:92` | TCP 入口同样兜底 `BD09`，实体与记录类注解写 `BD09` |

**为什么这条会直接误导重构**：BD09 相对 WGS84 的偏移量**随经度变化，不能用固定值校正**。若实现方按《03》"已闭合"假定现网历史轨迹已是 WGS84，就会跳过转换直接叠加显示，得到**系统性坐标偏移**——画在地图上偏、算距离也偏，而且不报错。

**已执行的登记（口径未改，只补执行层待办）**：
- 《07》§14.1：删掉原文那句假断言（"建表 DDL `pd_truck_location.coord_system` DEFAULT 'WGS84'"），替换为 7 行实测块（上表 4 条证据 + 结论"这是目标态、不是现状，已落库轨迹实际缺省 BD09" + ✅"传非法值返 400 **已实现**（`NettyController.java:60-61`），可对接"）。
- 《03》BA-19 状态列：`✅ 已闭合` → `✅ 口径已闭合｜🔴 落地未完成（2026-10-09 实测）`；§二 收口遗留表同步加前缀，原闭合记录以"｜✅ 原闭合记录"保留；统计行改为"**口径闭合** 19 项全部（BA-19 单列为执行层待办）"。
- 《03》新增 **`### BA-19 落地待办`** 6 行表（两项 DDL、HTTP 入口、TCP 入口、实体注解、**历史数据回填判定**），归属仍挂 P1-12，第 6 项须重估人日。
- 《08》`coordSystem` 行改写为"目标态 WGS84｜⚠️ 定案未落地，不传时实际落库 BD09｜400 校验已实现"。

> 📌 **落地前生效的硬口径**（已写进《03》）：任何"按 WGS84 处理存量轨迹"的写法一律视为缺陷。

### 4.6 新缺陷族成员：`pd_agency` —— "纯 SQL 层存在、代码层悬空"

§4.1 末尾"本轮新增的三类删码风险"第 3 条把"规划落点被写成现成契约"列为复现缺陷类；本轮找到一个**反方向**的同族成员：**表存在但代码不存在**。

- 《45》§1.3 把 `pd_agency` 记为 4 列且含**错误列名 `phone`** → 实测权威 DDL 为 **12 列**，正确列名 `contact_phone`（`docs/sql/pd_agency_建表与初始化.sql:25-39`），`status` 缺省 `'1'`。已补全并在《45》§2.2 后新增 🔎 实测块。
- 全仓库**无 `PdAgency` 实体、无 Mapper**（只有 `pd-base/.../entity/agency/PdAgencyScope.java`）；管理端 `AgencyController.java:47-48` 的数据经 `OrgApi`/`UserApi` 取得。**判定：SQL 层存在、代码层悬空** ⇒ 重构须自建实体 + Mapper，而"现网该表是否真有业务数据依赖"文档无法代验，已进 §4.7 人工清单。

### 4.7 人工实测清单（文档侧无法代验，**不得据本文声称已定**）

| # | 待实测事项 | 为什么文档给不出答案 | 复测入口 |
|---|---|---|---|
| M-1 | `pd_auth.pd_auth_resource` 现值是否仍为 383 条、是否含两条异常上报（`/courier/exception/report`、`/business/cargo/exception/report`） | G-A/G-H 未执行：需 MySQL 访问 | `SELECT COUNT(*) FROM pd_auth.pd_auth_resource;` |
| M-2 | 网关路由 8 条 vs 13 条（`T-03`/`EC-12`）；**外加 `pd-gateway` 的 `server.servlet.context-path` 真值**（§8.3 新行里的 `/api` 是**由现网 `:8760/api` 反推**的，仓库 `bootstrap.yml:31` 只引用不赋值） | 路由与 context-path 都在 Nacos `pd-gateway-test.yml`，**仓库里没有这个文件** | Nacos 控制台，或 `curl` 该 namespace 下 dataId |
| M-3 | `tms_order_location` 是否真实存在为表 | 见 §4.6：代码在查它，仓库无 DDL | `SHOW TABLES LIKE 'tms_order_location';` |
| M-4 | `pd_agency` 现网是否有业务数据依赖 | 见 §4.6：无实体无 Mapper，读不到用法 | `SELECT COUNT(*) FROM pd_auth.pd_agency;` + 现网 SQL 审计 |
| M-5 | `pd_notify_rule` / `pd_notify_record` 是否已建 | 全仓库只有文档，**无建表 SQL 也无实体**（《06》§6.4 已登记） | `SHOW TABLES LIKE 'pd_notify%';` |
| M-6 | `docs/sql/` 与 `docs/mysql/` 各脚本是否已落**正式库** | 本文只证明"脚本存在且 BA-19 两处互相矛盾"，不证明执行状态 | 逐库 `SHOW CREATE TABLE` 对照 |
| M-7 | 现网中间件真实版本（RabbitMQ 3.8 还是 compose 声明的 3.12） | 《07-项目现状基线》line71/86 是 **2026-10-07 快照**，早于本轮升级 | `docker ps --format "table .Names .Image"` |
| M-8 | 各服务端口与未授权请求的实际 401/403 行为 | 与 M-2 同源（网关侧配置，仓库无副本） | 逐个无 token `curl -i` 探测 |

> ⚠️ 这 8 项是本文 §一 结论里**唯一没有被本轮执行消除的部分**。它们都是"需要活体环境"，不是文档缺陷；**但 M-1/M-2 不闭合就不能宣称"网关放行清单可从文档重建"**（G-H 原判据）。

### 4.8 低优先观察项：一处真冲突 + 一处"看着像其实不是"（本轮判定后**有意未改**）

| # | 事项 | 判定 | 为什么不自动改 |
|---|---|---|---|
| O-1 | 《31》§1 验证步骤里写"用 `admin / 123456` 登录"（Nacos 控制台，`:8848/nacos`），而《26》§1.1 前言声明"**数据库/中间件账号口令不写入任何仓库文件**，按已私下提供的凭据使用" | ✅ **确为自相矛盾**（Nacos 属中间件，口令进了仓库文件） | 两个改法后果不同且**属用户取舍**：A 把口令换成"取自开发环境凭据"（符合规则，但删掉的是现网可用性信息，下次照手册升级可能卡住）；B 把《26》口径收窄为"**生产**凭据不入仓库，开发/测试内网环境例外"（保住可操作性，但等于放宽规则）。**默认推荐 B**——本文件服务的对象是内网开发测试机 `192.168.20.130`，规则原意显然不是禁止测试机手册可用 |
| O-2 | 《05》line789、《17》line44、《27》line16/114 多处出现"统一密码 `123456`"（`pinda`/`driver01`/`courier01`/`customer01`） | ⛔ **不冲突，判为正常** | 这些是 `docs/sql/testdata.sql` 造的**业务测试账号**，不是数据库/中间件口令；《26》规则管的是凭据类 secret。**特意登记，防止下一轮把它们当违规信息误删**——删掉后三端登录用例与造数脚本会同时失去账号口径 |

> 📌 观察项不进 §4.7 人工实测清单（不需要环境即可判断），但 O-1 的**改法选择须人工拍一次**，否则《26》和《31》会长期互相拆台。

---

## 五、可以删 / 暂时不能删

- **可以先删**：`pd-admin-ui`（Vue2 存量前端，UI 契约已由《14》《15》《16》《38》《39》覆盖，且 `D-32` 已换基座）、已被《02》line 129 判定废弃的 `pd_transport_alert` 相关未实现设计。
- **不能删（删了无替代）**：`pd-oms/src/main/resources/rules/*.drl` + `DroolsRulesServiceImpl` + `OrderServiceImpl#getDistance`（计费）、`pd-authority/pd_auth.sql` + `pd-auth-entity/.../Dictionary*.java`（schema）、`pd-netty/AlarmController` + `ExceptionType`（P0 契约）、`OrderEventMQListener`/`SettlementDeliveredListener`（事件语义）、`deploy/ci/gen-drools-seed-sql.js`（费率再生成）、`docs/mysql` 之外的任何 SQL 源头。
- **未实测、不得声称已定**：Nacos 网关路由 8 vs 13 条（`T-03`/`EC-12`）、`pd_auth_resource` 现值 383 是否含异常上报两行（G-H 需查库）、`pd_alarm_record` 建在 **`pd_oms` 库**却被 pd-netty 消费这一跨库归属是否要保留。✅ 第四轮已把这些扩成 **8 项带复测入口的人工实测清单**，见 §4.7（新增 `tms_order_location` 是否真表、`pd_agency`/`pd_notify_*` 现网状态、DDL 落库状态、中间件真实版本）。

---

## 六、用户输入 → 已办 / 待拍板（2026-10-09 第二~第四轮，按轮次累加）

第二轮用户原话两条：**"我删了多很多文档，我认为都是过期的，担心误导大模型。"** 和 **"文档中怎么有两个MQ，到底谁好用，或者是合适？"**（第四轮追加的原话见下表最后一行）

| 用户的担心 | 本轮处置 | 还差什么 |
|---|---|---|
| 删档会误导模型 | 已把"过期内容"换成"过期指针"这一类风险逐条清掉（§4.3）；`grep` 复核活文档中**已无指向 `_需求审查归档`/`_调研归档`/《36》/《50》 的可执行指针**，剩余命中全是"该档已删除"的历史留痕 | 删除尚未提交。**提交前 37 份归档可无损找回**（`git ls-tree` 实测在 `HEAD`），提交后只能走 `git log --diff-filter=D`。是否提交由用户决定，本文不代作 |
| **（第四轮追加）** "我的项目需求要参考广州联云信息科技有限公司和神领物流的技术架构和业务逻辑，它只有一个 RabbitMQ" + "你帮我检查一下文档是不是还有问题""其他的问题你检查到就帮我优化，一定要把需求文档写好" | **两条都已执行**：① 这句对标依据成为 BA-09 反向定案的**拍板依据 A**，已按《03》§六流程**改写定案文本**（不是加注）并连锁同步 7 处，详见 §4.1 G-L；② "检查文档"做成了**两族清扫**——《08》25 处代码断言逐条自证后订正（§4.4）+ 表格结构族 49/49 归零，并把检查工具固化进仓库（`docs/architecture/scripts/`）供复跑与进 CI，详见 §4.1 G-M。检查过程中新发现的两个执行层缺口已单列 §4.5（BA-19 零落地）与 §4.6（`pd_agency` 代码层悬空） | 对标依据 A 目前**只有一句话、无可引证据**：《37-竞品对标分析_联云TMS》全文没有 MQ 章节，"联云只有一个 RabbitMQ"在文档体系内暂无出处。若要长期作为架构决策依据，须补《37》技术架构对标节，或以实测证据 B（《47》§1.2.2）为主。⚠️ 这不构成推翻定案的理由——定案已由用户拍板，缺的只是留痕强度 |
| 通知链路到底走哪套 | 维持原状不猜：《52》§5-Q7 记录《03》BA-09① 与《20》§2.6 方向相反，且 BA-09 的改口径来源是"审查修订"而非新的用户拍板 → **裁决前按《20》同进程方案实现更安全** | Q7 与 Q2 建议一次拍完，否则会出现两套通知实现 |


