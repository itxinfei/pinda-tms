# 技术栈与系统架构体检（选型评估 + 结构优化）

> **本文管什么**：回答两个问题 —— ① 这套技术栈选型到底好不好；② 系统架构层面还有哪些结构性优化，哪些不该做。
> **与既有体检报告的分工**：`business-flow-understanding.md` / `flow-risks-and-gaps.md` / `support-domains-review.md` / `dispatch-and-customer-domain-review.md` 管**业务链路断点与缺陷**（编号 B-x/A-x 等）；本文管**技术栈与架构结构**，编号 `T-x`（技术栈）/ `S-x`（结构）/ `R-x`（回归网）。同一事实不重复取证，只引用其编号。
> **生成日期**：2026-10-09 ｜ **取证方式**：本地 HEAD 代码 + `pom.xml` 链 + `deploy/*` 实测 grep 计数；未连服务器（见 §7）。
> **读法**：只看结论读 §0/§1/§8；要动手读 §3–§6 的表格；每条都带 `文件:行号`，可逐条推翻。
> ⚠️ **本次环境事实**：工作区有 **116 个文件处于未提交修改**（含 `AccessFilter.java`、`docs/01`、`docs/07`、`docker-compose.infra.yml` 等），说明有并发在途改动。**本文只新增文件，未修改任何既有文档**；§2 的回写清单需授权后单独执行。

---

## 0. 一句话结论

**选型方向是对的，但 47/48 两篇优化建议文档把"决策已定"当成"现状已达"，据此打出的"已经很现代化"分不成立；真正该优化的不是换框架，而是四个结构性问题：服务粒度拆在了错的地方、数据归属是假的、公共层双轨、以及全仓零回归网就启动了一次跨大版本升级。**

---

## 1. 技术栈现状：口径纠正（T-1 ~ T-4）

### T-1 🔴 升级尚未开始，代码仍是 2019 年的栈

| 项 | 文档口径 | 本次实测（HEAD） | 证据 |
|---|---|---|---|
| Spring Boot / JDK | `47`§1.1 / `48`§三：3.3 / 21 | **2.2.5.RELEASE / 1.8** | `pom.xml:9,39`、`pom.xml:36,43`（`java.version` 根 pom **重复声明 2 次**） |
| javax→jakarta | 同上（jakarta 世界） | `javax.*` 仍在使用 **72 个文件**，`jakarta.*` **0 处** | 全仓 grep（`*.java`，排除 target/node_modules） |
| Java 文件基数 | `08` 记 870 | **876** | `find -name *.java` |
| 网关 | `47`§1.1 "Spring Cloud Gateway ✅已定案" | **Zuul 1.x** | `pd-authority/pd-apps/pd-gateway/.../zuul/**`（包路径即 `com.itheima.pinda.zuul`） |
| 管理端 | `47`§2.1 "✅ 已拉取" | 新基座 `pd-admin-ui-vue3` 144 个 `.vue` **就位**；但**当前部署在跑的是 Vue2** `pd-admin-ui`（120 个 `.vue`，`pinda/pd-admin-ui` 容器 :8080） | `pd-admin-ui-vue3/package.json:73-124`；`docker-compose.app.yml:302` |
| 移动端 | `47`/`48` "uni-app ✅ 已创建" | `pd-mobile` 仅 **2 个 `.vue`**（工程脚手架已建，页面≈未开工） | `find pd-mobile -name '*.vue'` |

**判定**：`29` 周计划的 W1 起点是 2026-10-09（今天），升级面为零。`48`§六 "我们的技术栈已经很现代化了，完全够用"评价的对象是**决策清单**，不是代码。这个错位的实际代价不是文档难看，而是：排期、验收口径、AI 的"能不能引用某 API"判断都会按 SB3/JDK21 写，而编译跑在 SB2.2.5 上。

### T-2 🔴 `08` 漏列的第二个 Netflix 硬阻断：Hystrix

- **证据**：`pd-authority/pd-apps/pd-auth/pd-auth-server/pom.xml:134` 依赖 `spring-cloud-starter-netflix-hystrix`。
- **影响**：Hystrix 与 Zuul 同批从 Spring Cloud 2020.0 发行列车移除，**SB3/SC2023 下没有对应 starter**。`08`§2 只把 Zuul 列为硬替换，升级执行到 pd-auth-server 时会撞上第二个"无迁移路径"。
- **动作**：在 `08`/`29` 的阶段 1 依赖替换清单里补一项 —— 熔断降级统一改 **Spring Cloud CircuitBreaker + Resilience4j**（属既有能力替换，不算引入新中间件）；或确认降级完全交给 Feign fallback 后删除 Hystrix 依赖。
- **置信**：high（依赖声明直证）。

### T-3 🟡 Feign fallback 是否生效，全仓无开关 —— 会改写既有结论 B-17

- **证据**：`pd-service-api` 下 23 个 `@FeignClient`，其中 **12 个声明了 `fallback`**；但除 pd-auth-server 外**没有任何模块依赖 Hystrix**，仓内也搜不到 `feign.hystrix.enabled=true`。另有 **10 个 `*Fallback.java` 仍在 `return null`**（`GoodsTypeFeignFallback:23,29,59`、`TransportLineFeignFallback:23,29,53`、`TransportTripsFeignFallback:23,29,36,42,61`、`TruckFeignFallback:24,30,54`、`CargoFeignFallback:28,34,46`、`OrderFeignFallback:27,33,63` …）。
- **两种可能，结论完全不同**：开关在 Nacos 里开了 → fallback 生效，B-17"降级静默返回 null 造成假成功"成立；没开 → `fallback` 属性被忽略，下游故障时**直接抛异常**，B-17 的表现形式要改写。
- **动作**：先查 Nacos `pinda-tms` group 下各服务配置里有无 `feign.hystrix.enabled`，再决定 B-17 怎么修。**不要先动代码。**
- **置信**：high（依赖与开关缺失均直证）；业务表现为 **unknown**，须验证。

### T-4 🟢 值得保留的判断：选型本身我不建议改

D-29 那份清单（SB3.3 / SC2023 / MP3.5 / Druid1.2.23 / Shiro2.0 / Drools8 / FastJSON2 / springdoc2 / Seata2 / Nacos2 / MySQL8）与"单机 15G、Docker Compose、7 库一实例"的实际约束是匹配的，且全部是既有能力的版本推进，不新增运维面。**`47`§1.1 提的 Sa-Token 换 Shiro、Easy Rules 换 Drools 属引入新框架，与 `01`§5"不引入新中间件/不过度设计"红线冲突，且按 `CLAUDE.md` 属"与权威文档冲突的优化默认无效"。** 详见 §6。

---

## 2. 文档治理冲突：待授权回写清单（本次未执行）

AI 按文档写代码，所以口径冲突的代价高于代码 bug。以下每一条都只**新增冲突事实**，不重复 §1 已给的证据。

| # | 冲突 | 处置建议 | 优先级 |
|---|---|---|---|
| G-1 | `docs/07:35` 仍写"技术栈（**写死，不升级**）"，而 `docs/01`§5 明确该条已于 2026-10-08 废止 | `07` 该行改为"升级前基线，目标见 `01`§5"；`07`§2.2"移动端工程不存在"也已过时（`pd-mobile` 已建） | P0：AI 读到会直接拒绝升级任务 |
| G-2 | `47`§1.2 "中间件 ✅ 已升级完成（8.0.46/7.4.11/2.3.2）"与 `29` 阶段3"中间件对齐**未做**"、`08`§阶段3 待办互斥 | 先取运行时真值（服务器 `docker ps`）→ 谁对改谁 | P0 |
| G-3 | 同一事项三档优先级：可观测性 `47`/`48`=P1 vs `32`D4=**P0（上线最小集）**；MySQL 主从 `47`=P1 / `48`=P2；链路追踪 `47`=P1 / `48`=P2 | 以 `32`（上线差距清单）为唯一优先级源，`47`/`48` 只保留"选型建议"，删掉自造优先级 | P1 |
| G-4 | `47`§4.1 "管理端前端 ⚠️ 待部署" —— 实际 `docker-compose.app.yml:302` 已部署 :8080，`07`§3.1 实测在跑 | 改正现状描述 | P1 |
| G-5 | `47`§1.3 "参数校验缺失 P0"、§5 "全局异常处理缺失 P0" —— 部分不实 | `validation-api` + `@Valid` 已在 **15 个文件**使用；`DefaultGlobalExceptionHandler` 存在于 `pd-authority/pd-tools/pd-tools-common/.../common/handler/`。**真问题是覆盖错位**：该 advice 属权限侧 pd-tools 世界，业务模块不依赖它，所以 pd-oms / pd-web-* 确实没有全局 advice，Controller 里手写 try-catch（`TransforCenterBusinessController` 10 处、`MailingController` 6 处）。建议按 §3 S-3 重写这一条 | P0：错误诊断会导致重复造一个已存在的东西 |
| G-6 | `47`§3.1 给 4 个 `pd-web-*` BFF 与 `pd-auth-server` 全打"✅ 合理"，唯一质疑 `pd-aggregation` | 与本文 §3 S-1/S-2 结论不同：pd-aggregation 不是唯一应质疑的。建议按证据改判 | P1 |
| G-7 | `flow-risks-and-gaps.md` B-18 / `business-flow-understanding.md` §4.2 记"网关 fail-open + 降级返回 null 先 NPE" | **HEAD 仍成立**（`AccessFilter.java:73,74,78`）；但工作区已有一份**未提交**的 fail-closed 修复（`git diff` 显示 `AccessFilter.java` 42 行改动，改为判 `isError` + 拒绝，`ResourceApiFallback.list()` 由 `return null` 改 `R.timeout()`）。待该改动提交后，B-18 才可标"已修"。**本次未代为提交，也未回写状态**，避免踩在并发改动上 | P0（跟踪项，非新缺陷） |

---

## 3. 系统架构：结构性问题（S-1 ~ S-5）

### S-1 🟠 服务粒度：拆在了错的地方（14 个 JVM / 单机 15G）

| 观察 | 证据 | 判定 |
|---|---|---|
| `pd-druid` **9 个 Java 文件**独立成服务 + 独立端口 8193，且 `FROM tms_order_location` 查一张任何库都不存在的表 | 文件计数；`DruidServiceImpl.java:45,63,80`；`docker-compose.app.yml:240`；断点见 B-05 | 服务名与职责都不成立，独立部署纯属历史包袱 |
| `pd-aggregation` 15 个文件 + 独立库，只为读 **12 张只写不读的副本**（mapper 内 0 条 insert/update） | `business-flow-understanding.md` 附表；副本空实测定型为 B-04 | 同 S-2 |
| `pd-web` 4 个 BFF 各自独立打成镜像、各占一个端口（8161-8164），**却没有共享模块** | `pd-web/pom.xml:16-19`（仅 4 个子 module，无 common）；各子模块 pom 均自带 `spring-boot-maven-plugin`（`pd-web-manager/pom.xml:79`） | 结构性重复 |
| 重复的具体形态 | `AttachmentController`/`AttachmentClient` ×3（driver/customer/courier）、`UserController` ×3、`CommonController` ×3、`ConfigurationSupport` ×4；BFF 合计 113 文件 / 30 Controller | **快递员端就是在这场复制里漏掉了 AttachmentClient** → POD 签收照片证据链落不下（B-21）。这不是"风格问题"，是会产生漏改缺陷的结构问题 |

**优化动作**（成本与收益都标注）：
1. **4 个 BFF 合成 1 个**，按 `/manager|/driver|/courier|/customer` 前缀分路由。收益：省 3 个 JVM（按 `compose` 锚点 `-Xmx192m` + 堆外，约 0.5–0.7G 内存，单机 15G 里是实打实的余量）；漏改类缺陷（B-21 型）结构性消失。成本：1 次部署 + 网关路由 + CORS 改造。**P2，且禁止与升级同期做**（同时改拓扑和版本 → `08`R7"故障定位困难"成立）。
2. **`pd-druid` 并进 `pd-oms` 或直接删**（它只做车辆位置/大屏查询，且表名已错）。P2。
3. **`pd-aggregation` 去留取决于 S-2**，先定数据归属再谈合并，否则会丢接口。

### S-2 🔴 数据归属：所谓"分库微服务"在单实例 MySQL 上是假的

- **事实**：7 个业务库全跑在**同一个 MySQL 实例**上（`docker-compose.infra.yml:25` 单服务；各服务 `jdbc:mysql://…/<db>` 由 Nacos 注入）。`02`§1"各自拥有数据源，避免跨库事务"是**命名隔离，不是数据隔离**。
- **第三态问题**：`pd-aggregation` 的 12 张副本表既不是 owner 读、也不是 Feign 调，且**没有任何写入方** —— 于是管理端/网点列表稳定返回 0 行（B-04，实测 `counts=0`）。这类"数据没有"会被当成前端或权限问题查半天。
- **归属漂移**：`pd-netty` 与 `pd-druid` 都连 `pd_oms` → 订单库同时承载 **订单 + 轨迹 + 位置查询** 三个域，容量/备份/权限纠缠（§2.3）。
- **遗留噪声**：`pinda_tms` 65 张同名表仍在线，无服务连接（B-16）。
- **`47`§3.2 说"pd_aggregation 可以去掉，用视图代替"** —— 同实例下跨 schema 视图技术上可行，但那只是把"假隔离"换成"假透明"，视图仍会让 pd-oms 的 schema 变更悄悄打断聚合层。**不作为推荐方案。**

**优化动作（这是决策，不是重构）**：给每个共享对象定一句归属，二选一，写进文档，**禁止保留第三态**：
- `pd_order` / `pd_core_org` / `pd_transport_order` 这类：**只允许 owner 服务读**，聚合层改 Feign；或
- 明确承认同实例可利用跨 schema 查询，则把数据源配置写死为显式 `db.table` 前缀并在 `02`§6 记录"单实例跨库读"为受控例外。
- 无论选哪种：`pinda_tms` 确认无引用后**改名归档**（`pinda_tms_retired`），不删。

### S-3 🟠 公共层双轨 + 同根包名（既有文档未覆盖）

- **证据**：`pd-common`（业务侧，被 21 个 pom 引用）与 `pd-authority/pd-tools/pd-tools-common`（权限侧）**共用同一根包 `com.itheima.pinda.common.*`**，两边都有 `common/enums` 子包；返回体一份 `Result`（`pd-common/.../common/utils/Result.java`）、一份 `R`（`pd-authority/pd-tools/pd-tools-core/.../base/R.java`）；全局异常 advice 只在 pd-tools 侧。
- **叠加**：`pd-aggregation` 与 `pd-druid` **同时依赖这两个包** → 同名包进同一 classpath。
- **本次核对结论**：逐文件名比对两棵树，**当前没有实际重复类**（`comm -12` 结果为空），所以这是**风险敞口而非现症**。但贵司已有一次同类事故：j2cache-core 同名 properties 遮蔽导致网关阻塞（见项目记忆 / commit `c17f9b3`、正解 `7fa609f`）。同一失效模式，只是还没被触发。
- **物理根因**就是 `08`§2.1 的双 parent 链：根 pom 12 个 module **不含 pd-authority**（`pom.xml:22-33`），`pd-authority/pom.xml` 是无 parent 的独立聚合 pom（版本 1.0-SNAPSHOT，modules = pd-parent/pd-apps/pd-tools）。两条链各自 `dependencyManagement`，公共层自然长成两份。

**优化动作**：`08` 阶段 0"合并双 parent 链基线"时**顺带把公共层收敛成一份**（异常处理、返回体、枚举、包名），否则升完 SB3 仍是两个世界，G-5 那个"缺全局异常处理"的误诊会反复出现。收敛方式选**把 advice 上移到业务侧也能依赖的模块**，不要再写第三个。

### S-4 🔴 信任边界画错（架构问题，非 bug 清单）

- **证据链**：`docker-compose.app.yml` 把 8760/9000/8185-8193/**8161-8164** 全部 `-p` 发布到宿主（`:113,133,160,172,184,196,208,220,233,245,258,270,282,294`）；而 `pd-common/.../interceptor/TokenAuthInterceptor.java:47-48` 只判断 `userid` 头**是否存在**，类注释 `:15-16` 自己写明设计前提是"网关已验 JWT 并注入"。
- **后果**：任何能连 `192.168.20.130` 的人伪造 `userid: 1` 即可绕过全部业务鉴权，多网点数据隔离形同不存在（B-19）。**"接口返回 200"与"鉴权生效"是两件事**（`CLAUDE.md`§4 警示语同源）。
- **优化动作**（成本极低，我把它排在所有 P0 最前）：
  1. compose 收口 —— 业务/BFF 服务改 `expose` 或走仅容器内可达的 docker 网络，只有 `pd-gateway`（+ 前端）发布宿主端口；
  2. 网关降级路径统一 **fail-closed**（工作区已有未提交版本，见 G-7）；
  3. 加一条**断源测试**：停 `pd-auth-server` 后调需鉴权接口，断言 401/503 而非 200；不带 token 直连 8162 应被拒。

### S-5 🟡 一致性策略缺一份决策表

- **事实**：`@GlobalTransactional` 全仓仅 **5 处**（清单见 `business-flow-understanding.md`§数据与事务边界小结），`@Transactional` 23 处；`pd-common/pom.xml:120` 带 `seata-spring-boot-starter`，8 个库 `undo_log` 齐（铺好了没人用）；转发中心 77 处跨服务 Feign 写、0 个事务注解（B-20）；签收/交件/妥投三处多实体裸写（B-06）。
- **另有两处会卡住"多实例"的硬编码**：`pd-base/.../CustomIdGenerator.java:16` 与 `pd-oms/.../CustomIdGenerator.java:16` 都写死 `new IdWorker(1, 1)`（机器位固定，扩实例必撞号）；`pd-dispatch/.../IdUtils.java:6` 用的是无参 `new IdWorker()`。
- **可以放心的地方**：`pd-dispatch/.../ScheduleConfig.java:30` 已设 `org.quartz.jobStore.isClustered=true` —— 调度侧扩实例是安全的，**这条不用动**。
- **优化动作**：产出比"要不要上 Seata"更有用的东西 —— 一张**链路一致性分级表**：哪些必须强一致（签收、费用、资金、状态流转）、哪些允许最终一致 + 对账补偿（轨迹、心跳、通知）。分完级，事务注解往哪补就自动确定了，也不用为假想需求预建抽象（`01`§5 红线）。

---

## 4. 最大的架构风险：零回归网上启动跨大版本升级（R-1）

`47`/`48` 两篇通篇没有提测试。实测：

| 指标 | 值 | 证据 |
|---|---|---|
| Java 文件 / 测试文件 | **876 / 18**（2%） | `find` 计数 |
| 测试分布 | pd-oms 5、pd-auth-server 4、pd-dispatch 2、pd-netty 2、pd-tools-jwt 2、pd-tools-log 1、pd-druid 1、pd-work 1 | `find */src/test` 按模块计数 |
| **测试为 0 的区域** | `pd-base`、`pd-user`、以及**整个接入层 pd-web**（4 模块 / 113 文件 / 30 Controller） | 同上 |

**为什么这是架构问题而不是质量问题**：`08` 自己登记的两个最高危风险 —— R2"Drools DSL 翻译错误 → **算错钱**"、R3"`AccessFilter` 迁移改变鉴权语义" —— 恰好落在**运费计算**和**网关鉴权**上，而这两块没有回归网。跨大版本升级的验证成本本应由测试承担，现在会全部转嫁给上线后的人工联调。

**R-1 建议（性价比最高的一条，且不新增中间件、不改拓扑、不与任何定案冲突）**：

> 在 W1 之前、**还在旧栈上**补两组特征测试（characterization test）锁住基线，升级后逐条比对：
> 1. **运费试算输入/金额矩阵** —— 现有 `27-链路测试用例与造数脚本` 的样本 × 各计价策略，记录升级前金额（`08`§2.3 验收要求"逐单比对基线金额"，但没有东西能产出这个基线）。⚠️ 注意 `support-domains-review` 实测 `pd_oms.rule` 与 `pinda_tms.rule` **各 0 行**、`OrderServiceImpl.java:257-262` 当前算费返回 null —— **先修规则数据，否则基线本身是空的**。
> 2. **网关鉴权矩阵** —— 已注册接口 / 未注册接口 / 权限服务降级，三态各自的期望 HTTP 与业务码（与 S-4 的断源测试是同一件事的两半）。
>
> 落点建议插进 `29` 周计划**阶段 0**，作为"升级前基线记录"的具体产出物 —— `29` 里这条现在只有一句"记录各服务端口/登录返回"。

---

## 5. 处置顺序（与 D-29/D-32/D-33、`01`§5 红线全部兼容）

| 档 | 动作 | 编号 | 为什么在这里 |
|---|---|---|---|
| **P0 · 升级前/期内** | 特征测试锁基线（运费 + 鉴权矩阵） | R-1 | 决定后面一切验证是否可信 |
| | 服务端口收网 + 网关 fail-closed + 断源测试 | S-4 / G-7 | 成本最低，且是"看起来跑通了"假象的根源 |
| | 查 Nacos 是否有 `feign.hystrix.enabled` → 定性 B-17 表现 | T-3 | 查清前不动 Fallback 代码 |
| | 文档口径归一（`07`"写死不升级"、`47`"已升级完成"、G-5 误诊） | G-1/G-2/G-5 | AI 按文档写码，错口径会被批量放大 |
| | `08`/`29` 补 Hystrix 阻断项 | T-2 | 阶段 1 会撞上 |
| **P1 · 升级后立刻** | 数据归属定案（副本表同步 or 删，禁第三态）+ `pinda_tms` 改名归档 | S-2 | 不做，每个新页面都会再踩"莫名空数据" |
| | 公共层收敛（advice / 返回体 / 包名），与双 parent 链合并同期 | S-3 | 一次改造解决两个问题 |
| | 链路一致性分级表 → 据此补事务注解 | S-5 | 避免"为要不要上 Seata"空转 |
| **P2 · 功能稳定后** | BFF 4→1、`pd-druid` 合并/删、`pd-aggregation` 去留 | S-1 | 拓扑改动必须等版本迁移落地，否则定位成本翻倍 |
| | 可观测性（按 `32` **D4=P0** 口径推进，别沿用 `47`/`48` 的 P1）、主从 / 多实例 | G-3 | 多实例前先解 `IdWorker(1,1)` 硬编码（S-5） |

---

## 6. 明确不建议做的"优化"

| 不建议 | 理由 |
|---|---|
| Sa-Token 换 Shiro、Easy Rules 换 Drools（`47`§1.1） | D-29 已定案；且 `47` 自己标的是"建议非定案"，按 `CLAUDE.md` 与权威文档冲突的优化默认无效。**更关键**：Drools 现在的真实状态是规则表 0 行、算费返回 null（`support-domains-review`§6）—— 换引擎不会让计价变对，先修数据和规则 |
| 引入 Sentinel / ES / MongoDB / RocketMQ / XXL-JOB / K8s | `01`§5"不引入新中间件"红线**仍然有效**；单机 Compose 下这些收益兑现不了 |
| 把 7 个库做物理拆分 / 每服务一实例 | 结构上更"正确"，但当前容量下收益为零、备份与运维成本翻 7 倍。要优化的是**归属规则**（S-2），不是**物理位置** |
| 为"聚合层/监控层"新建第二套服务或第二张业务表 | `01`§1 冲突提示 1 已定方向"契约优先、基座适配后端"，禁止另起一套 |
| 升级期同时做拓扑合并（BFF 4→1） | `08`R7；等 `mvn compile` 在 21 下全绿再动 |

---

## 7. 未知与验证动作（不当成事实）

| 未知 | 为什么静态判不了 | 验证动作 |
|---|---|---|
| 中间件运行时真实版本（MySQL 5.7 还是 8.0） | `compose` 声明 ≠ 运行容器；本次**未授权连服务器** | `docker ps --format '{{.Image}}'` + `SELECT VERSION()`；结果回填 G-2 |
| Nacos 内的网关路由 / 各服务数据源 / `feign.hystrix.enabled` / `cors` | 配置在 Nacos，仓内**一条 `jdbc:mysql` 都搜不到**（实测） | 导出 `pinda-tms` group 下全部 dataId，与仓内 `bootstrap-*.yml` 做差异清单 |
| 工作区 116 个未提交改动的归属与意图 | 静态看不到是谁、为何改 | 需人工确认；**在其落地前我未改任何既有文件**，避免冲突 |
| `pd-mobile` 2 个 `.vue` 是未开工还是另在他处 | 只有脚手架文件 | 与 `51-移动三端建设规划` 对照 |

## 8. 证据边界与方法

- 取证范围：本地 `D:\MyCode\pinda-tms` HEAD 代码、47 个 `pom.xml`、`deploy/apps/docker-compose.app.yml`、`deploy/middleware/docker-compose.infra.yml`、`docs/` 与 `docs/需求文档/` 与 `docs/architecture/` 既有文档。
- 关键计数为直接命令实测：Java 876、测试文件 18、`javax.*` 72 文件、`jakarta.*` 0、`@GlobalTransactional` 5、`@Transactional` 23、`@FeignClient` 23（含 fallback 12）、Fallback 仍 `return null` 10 文件、Controller 76（引 `07`§1.2）。
- **本文未验证任何运行时行为**（无服务器访问）。所有"实测"字样若出自 `07`/`47`/既有体检报告，均已标注来源日期；B-18 一条经本次复核发现**结论已变**（见 G-7），说明引用旧体检结论前必须回代码复核 —— 与项目记忆"约 1/3 结论会被推翻"一致。
- 置信标注：T-1/T-2/T-3/S-2/S-4/R-1 为 **high**（代码与配置直证）；S-1 合并收益估算为 **medium**（内存锚点直证，省出量按 compose `-Xmx192m` + 堆外推算）；S-3 为 **high 事实 + medium 影响**（当前无重复类，风险为推断）；中间件版本为 **unknown**。

## 附：图文件

| 文件 | 回答的问题 | 说明 |
|---|---|---|
| `current-runtime-topology.dot` | 现在实际怎么跑、信任边界画错在哪 | Graphviz DOT，`rankdir=TB`；实线=代码/配置双证，虚线=推断待证；红=边界违规 |
| `architecture-optimization-roadmap.dot` | 优化项之间的先后依赖 | 菱形=结构问题，矩形=动作，虚线=收益推断；标注 P0/P1/P2 |

渲染：`dot -Tsvg current-runtime-topology.dot -o current-runtime-topology.svg`（本机未装 Graphviz，故本次**只交付 .dot 源**，与 `docs/architecture/` 既有 4 张 `.dot` 保持一致）。

