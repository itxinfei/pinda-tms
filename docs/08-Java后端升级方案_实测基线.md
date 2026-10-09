# Java 后端升级方案（JDK 8 → JDK 21 / SB 2.2 → 3.3）


> **定位**：本文是**基于代码实测**的升级执行方案，补《阶段1_技术栈现代化_周计划》未覆盖的阻断点。
> **实测日期**：2026-10-08 ｜ **实测方式**：读 `pom.xml` 链 + grep 真实 `javax.*` 用量 + 扫构建链Dockerfile/CI。
> **规模基数**：870 个 Java 文件（已排除 node_modules/target）。
> **目标**：JDK 21 LTS + Spring Boot 3.3 + Spring Cloud 2023 + Spring Cloud Alibaba 2023 + Spring Cloud Gateway（jakarta.* 命名空间）。

---

## 0. 结论先行（升级的真实难点不是"改版本号"）

现有周计划把升级估成 W1 约 8.5-11.5 人日。实测核查后判断：**这个估算偏乐观**，因为存在 2 个此前未被识别的阻断项，且javax 迁移的**实际影响面比预期集中**。

**三个最关键的事实**：

1. **网关现状是 Zuul，不是 Gateway**——SRS §1.2 写的"网关现状 = Spring Cloud Gateway、不是 Zuul 1.x"**与代码不符**：`pd-gateway` 的 pom 明确依赖 `spring-cloud-starter-netflix-zuul`，包路径为 `com.itheima.pinda.zuul`。Zuul 无迁移路径（Netflix Zuul 自 Spring Cloud 2020.0 起已从发行列车移除），**必须重写为 Gateway**，不能"改配置"。
2. **存在两套互不继承的 parent 链**，版本不一致 → 47 个 pom 会分裂成两个不兼容的世界，**必须先合并基线**。
3. **`javax.*` 影响 76 个文件（占 8.7%）**，其中 `javax.validation` 42 个文件（面最广）、`javax.servlet` 25 个、`javax.annotation` 9 个；`jakarta.*` 目前 **0 处**。

---

## 1. 实测基线（现状 vs 目标）

### 1.1 版本矩阵
| 组件 | 现状（实测） | 目标 | 风险 |
|---|---|---|---|
| JDK | **1.8**（根 pom properties 声明 2 次） | 21 LTS | — |
| Spring Boot | `2.2.5.RELEASE` /另一链 `2.2.2` | 3.3.x | ⚠️ 双链冲突 |
| Spring Cloud | `Hoxton.SR3` / 另一链 `Hoxton.SR1` | 2023.0.x | ⚠️ 双链冲突 |
| Spring Cloud Alibaba | `2.2.1.RELEASE` / 另一链 `2.1.1` | 2023.0.x | ⚠️ 双链冲突 |
| **网关** | **`spring-cloud-starter-netflix-zuul`**（仅 pd-gateway） | Spring Cloud Gateway | 🔴 **硬替换** |
| MyBatis Plus | `3.3.0` / 另一链 `3.2.0` | 3.5.x（`mybatis-plus-spring-boot3-starter`） | starter 坐标拆分 |
| Druid | `1.1.22`（pd-common 自带 `1.1.13`） | 1.2.23 | starter 坐标拆分 |
| **Shiro** | `1.4.0`（`shiro-spring`） | 2.0.x | 🔴 Shiro1.4 不支持 jakarta |
| **FastJSON** | `1.2.47` / `1.2.62`，**无 FastJSON2** | FastJSON2 2.x | 🔴 包名变更 |
| **Drools** | `6.5.0.Final`（pd-oms） | 8.x | 🔴 **JDK21 编译失败（JAXB）** |
| Seata | `1.2.0` | 2.x | SC2023 无 1.x starter |
| **Lombok** | **`1.18.4` / `1.18.10`** | ≥1.18.30 | 🔴 低版本不支持 JDK21 |
| MySQL驱动 | `mysql:mysql-connector-java` 6.0.6 / 8.0.19 | `com.mysql:mysql-connector-j` | 坐标变更 |
| API 文档 | **springfox 2.9.2 + knife4j 2.0.1** | springdoc-openapi 2.x | 🔴 springfox2.x 不兼容 SB3 |
| Netty | `netty-all 4.1.12.Final` | 4.1.10x+ | 需支持 JDK21 |

### 1.2 javax→jakarta 实际影响面（grep 实测，870 文件基数）
| 包 | 涉及文件数 | 说明 |
|---|---|---|
| `javax.validation` | **42** | 面最广（`@NotNull`/`@Valid` 等），随 starter 换包即可批量改 |
| `javax.servlet` | **25** | 集中在 pd-tools / 网关 Filter / pd-web |
| `javax.annotation` | **9** | `@PostConstruct`/`@Resource` 等，JDK11+ 已从 JDK 移除 |
| `javax.xml.bind` | 0（源码） |但 **Drools 6.5 传递依赖它**，见 §2.3 |
| `javax.persistence` | 0 | 无 |

> **好消息**：`jakarta.*` 0 处、`module-info.java` 0 处（无阻碍）、`javax.persistence` 0 处。面比想象小，且 `javax.validation`/`javax.servlet` 多为机械import 替换。

---

## 2. 五个必须处理的阻断点

### 2.1 🔴 双parent 链版本冲突（此前未识别，建议最先处理）
- **主链**（根 pom，12 module）：SB `2.2.5` / SC `Hoxton.SR3` / SCA `2.2.1`
- **`pd-authority` 链**（独立聚合 pom，无 parent）：SB `2.2.2` / SC `Hoxton.SR1` / SCA `2.1.1`，modules = `pd-parent` / `pd-apps` / `pd-tools`
- **问题**：两条链各自 `dependencyManagement` 版本不一致，且 `pd-authority` 是**独立聚合 pom（无 parent）**，无法自动继承主链。
- **后果**：直接升SB3.3 会得到两个互不兼容的世界；网关与 auth-server 在 `pd-authority` 内，其依赖解析不随主链变化。
- **处理**：① 先让 `pd-authority` 继承根 pom（或至少对齐三者版本）；② 再统一升SB3.3 + SC2023 + SCA2023；③ `pd-parent` 中重复声明的 `lombok`/`fastjson`/`shiro` 等版本清理，避免覆盖主链。

### 2.2 🔴 Zuul → Gateway 硬替换（SRS §1.2 的表述需纠正）
- `pd-gateway` 有 **5 个自定义 Filter**：`TokenContextFilter`、`BaseFilter`、`AccessFilter`（均 `javax.servlet`）、`SwaggerResourceConfig`、`IgnoreTokenConfig`、`IgnoreResourceConfig`，另有 `GeneratorController` 与 fallback 类。
- **迁移映射**（Servlet Filter → Gateway `GlobalFilter`，**编程模型重写**）：
  | Zuul Filter | Gateway 对应 | 注意 |
  |---|---|---|
  | `TokenContextFilter`（pre，解析 JWT 注入 userid 等头） | `GlobalFilter`，改 `ServerHttpRequest.mutate().header(...)` | ⛔ 不能再用 `request.getHeader`/`addZuulRequestHeader` |
  | `AccessFilter`（pre，需鉴权资源 + 权限，j2cache） | `GlobalFilter` + j2cache 逻辑搬运 | ⛔ 需重写取缓存与放行判断 |
  | `BaseFilter` / `IgnoreTokenConfig` / `IgnoreResourceConfig` | `GlobalFilter` + `Ordered` | — |
  | 路由配置（`zuul.routes.*`） | `spring.cloud.gateway.routes.*` | ⚠️ 语义有差异（`stripPrefix` 等） |
- **红线提醒**：`AccessFilter` 是**鉴权关卡**（关卡1 需鉴权资源清单 / 关卡2 用户权限），迁移后必须复刻"清单为 null 时整段跳过"的行为（源码 `if (resourceNeed2Auth != null)`），否则**鉴权语义会变**——这是登录/联调的硬闸口。
- **验收**：`curl /api/auth/anno/login` 返回 200；`/api/authority/menu/router` 经网关返回 200（现为已知验收命令）。

### 2.3 🔴 Drools 6.5 → 8.x（JDK21 编译期即失败）
- `pd-oms` 依赖 `kie-spring` + `drools-compiler` 6.5.0.Final。**Drools 6.5 依赖 `javax.xml.bind`/`javax.activation`（JAXB）**，JDK 11+ 已从 JDK 移除 → **在 JDK21 下编译期直接失败**。
- 两条路：
  - **推荐**：升级到 Drools 8.x + `kie-spring`（新坐标），运费规则 DSL 需翻译并回归验证；
  - **临时**：先补 `jaxb-api` + `jaxb-runtime` + `javax.activation` 依赖让编译过，但**不建议**——JDK21 下仍留隐患。
- **风险**：运费计算是**核心链路**（`OrderServiceImpl#calculateAmount` 内联 KieSession 真实计费）。🔴 **订正 2026-10-09**：原文判 `DroolsRulesServiceImpl#calcFee` 为"死代码勿用"**有误**——它由 `.drl` 的 `then` 块调用（`orderAmountCalc.drl:33/50/67`），Java 静态检索看不到引用，删掉即三条续重规则运行期失败；升级 Drools 时**必须连同该类一起回归**。规则 DSL 翻译错误会直接算错钱（口径唯一真源：《52》§1）。
- **验收**：运费试算接口返回正确金额，且与上线前基线金额一致（**必须先跑基线取值再改规则**）。

### 2.4 ⚠️ Lombok 低版本不支持 JDK21
- 两处 properties 声明 `1.18.4` / `1.18.10`（root + pd-common；pd-parent 另有 `1.18.10`），均**不支持 JDK 21**。
- 需升至 **≥1.18.30**，并把 `maven-compiler-plugin` 的 `source/target` 改为 `<release>21</release>`（`release` 比 source/target 更严格，能防"用旧API 编译通过但运行报错"）。
- 根 pom 当前**未配置** `maven-compiler-plugin`（仅 `pd-parent` 用 source/target）。

### 2.5 ⚠️ 构建链三处硬编码 JRE8
| 文件 | 现状 | 改为 |
|---|---|---|
| `deploy/ci/Dockerfile` | `FROM maven:3.6.3-jdk-8` | `maven:3.9-eclipse-temurin-21`（或同等） |
| `deploy/apps/Dockerfile.base` | `FROM eclipse-temurin:8-jre-jammy` | `eclipse-temurin:21-jre-jammy` |
| `deploy/apps/Dockerfile` | `FROM pinda/jre8-fontconfig:1` | 改用 temurin21 基础镜像并重建 |
- CI（`.gitea/workflows/deploy.yml`）用 `mvn -T 4 clean package ...`（**无 `java-version` 声明**），JDK 版本实际由 `deploy/ci/Dockerfile` 决定 → 改 Dockerfile 即改 CI 的 JDK。
- ⚠️ 注意：Dockerfile.base tag 为 `pinda/jre8-fontconfig:1`（**本地自建镜像**，非公开）——需确认字体（PDF 水印等）处理是否依赖其中的 fontconfig，换基础镜像时保留该能力。

---

## 3. 建议升级路线（按依赖顺序，非按业务顺序）

>原则：**先让依赖树能解析，再让代码能编译，再让服务能启动，最后联调**。不要在编译不过时纠结业务功能。

### 阶段 0 · 基线统一（前置，最容易被忽略但必须先做）
- [ ] 让 `pd-authority`（`pd-parent`/`pd-apps`/`pd-tools`）与根 pom 版本一致，清理重复版本声明
- [ ] 本机/CI 装Temurin 21；根 pom 改 `<release>21</release>`；Lombok ≥1.18.30
- [ ] 记录**升级前基线**：各服务端口、登录接口返回、`/menu/router` 经网关返回 200（便于对比）
- **验证**：`mvn -o clean compile -DskipTests` 通过（此时可能仍失败，但错误应只剩 javax/依赖类）

### 阶段 1 · 依赖替换 + javax 迁移（主体工作量）
- [ ] SB3.3 / SC2023 / SCA2023 / MyBatisPlus 3.5(`-spring-boot3-starter`) / Druid 1.2.23 / Shiro 2.0 / FastJSON2 / Seata 2.x / MySQL 驱动换坐标 / Netty 4.1.10x+
- [ ] springfox 2.9.2 + knife4j → **springdoc-openapi 2.x**（注解 `@Operation`/`@Schema`，不保留 springfox）
- [ ] javax→jakarta：优先 OpenRewrite（`rewrite-maven-plugin`）批量，再手工清残留
- [ ] Drools 6.5 → 8.x（运费 DSL 翻译 + 金额基线回归）
- **验证**：`mvn -o clean compile` 通过；`grep -rn "javax\." --include=*.java` 仅剩允许项（如确有理由的 `javax.xml.bind`）

### 阶段 2 · 网关 Zuul → Gateway（独立可验证，建议单独立项）
- [ ] 5 个 Filter → `GlobalFilter`；路由配置迁 `spring.cloud.gateway.routes.*`
- [ ] **复刻 `AccessFilter` 鉴权语义**（含 null 清单放行行为）
- **验证**：登录 200、`/menu/router` 经网关 200、资源已注册接口 200、未注册接口 401/拒绝

### 阶段 3 · 中间件对齐（✅ 已完成，2026-10-09）
- [x] MySQL 8.0.46 / Redis 7.4.11 / Nacos 2.3.2 / RabbitMQ 3.12
- [ ] Seata Server 注册 Nacos，`@GlobalTransactional` 真实生效
- **验证**：中间件全部正常运行，待 Java 微服务升级后重新部署业务服务

### 阶段 4 · 联调回归
- [ ] 用《链路测试用例与造数脚本》跑 TC-01~11；重点回归**鉴权、运费试算、下单、轨迹上报**四条
- **验证**：核心链路用例全过

---

## 4. 风险登记与回滚

| # | 风险 | 影响 | 缓解 |
|---|---|---|---|
| R1 | 双 parent 链未合并就升版本 | 依赖分裂、编译错乱 | 阶段 0 先合并基线 |
| R2 | 运费规则 DSL 翻译错误 | **算错钱**（核心链路） | 升级前跑基线金额，改后逐单比对 |
| R3 | `AccessFilter` 迁移改变鉴权语义 | 未鉴权接口被放行 / 正常接口被拒 | 复刻原逻辑（含 null 放行）；用已注册/未注册接口对比验收 |
| R4 | Drools JAXB 依赖未解决 | JDK21 编译期失败 | 阶段 1 直接升 Drools 8.x，不走补依赖的临时路 |
| R5 | Springfox 无 jakarta 版 | API 文档栈崩溃 | 整体换springdoc-openapi |
| R6 | 自建 `jre8-fontconfig` 镜像换基座 | PDF 水印等字体能力丢失 | 换镜像时保留 fontconfig |
| R7 | 一次性全量替换，改动过大难回滚 | 故障定位困难 | 按阶段 0~4 分次提交，每阶段可独立回滚；**上线前保留可回滚镜像 tag** |

---

## 5. 与既有文档的关系
- 《阶段1_技术栈现代化_周计划》：方向一致，本篇补其未覆盖的**双parent 链、javax 76 文件实测面、Drools JAXB 硬阻断**，并纠正"872 文件"与"Zuul 迁移 2-3 天"的乐观估算。
- **SRS §1.2 第 7 条需纠正**：现写"网关现状 = Spring Cloud Gateway、不是 Zuul 1.x"，**与代码不符**（实测为 Zuul）。网关是升级期硬替换，不是"不得回退"。
- 版本选型以 **SRS §2.1** 为准；本文只补"怎么升、先后顺序、卡点在哪"。

---

*本文依据 2026-10-08 代码实测撰写。执行前请以当前 pom.xml 为准复核（外部进程可能改动）。*
