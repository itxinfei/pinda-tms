# 阶段 1 · Claude Code 批量执行手册

> **用途**：把阶段 1（技术栈现代化）拆成 N 个原子任务，每个任务是一段可直接粘贴给 Claude Code 的完整 prompt。
> **项目根**：`D:\MyCode\pinda-tms`
> **执行顺序**：严格按 T-001 → T-0xx 顺序执行，前一个完成再开下一个。
> **总任务数**：19 个（W1 后端 10 + W2 前端 4 + W3 联调+基础设施 5）；中间件升级已于 2026-10-09 完成，不列入任务。

---

## 〇、全局上下文（每次新开 Claude Code 会话时，先粘贴这段）

```
你正在帮我升级品达 TMS 项目的技术栈。项目根目录：D:\MyCode\pinda-tms。

【项目背景】
这是一个物流 TMS（运输管理系统），12 个微服务模块，当前是 demo 阶段，未上线。
目标是做成开源可商用产品，所以要把技术栈从老版本升到现代版本。

【当前技术栈】
- JDK 1.8 + Spring Boot 2.2.5 + Spring Cloud Hoxton.SR3 + Spring Cloud Alibaba 2.2.1
- 网关：Zuul 1.x（Netflix）
- ORM：MyBatis Plus 3.3.0
- 连接池：Druid 1.1.22
- 鉴权：Shiro 1.4.0
- 规则引擎：Drools 6.5.0
- JSON：FastJSON 1.2.x
- API 文档：springfox-swagger 2.9.2
- 前端管理端：Vue 2.6 + Element UI 2.12 + Vue CLI 3 + node-sass

【目标技术栈】
- JDK 21 LTS（Eclipse Temurin）
- Spring Boot 3.3.x + Spring Cloud 2023.0.x + Spring Cloud Alibaba 2023.0.x
- 网关：Spring Cloud Gateway（WebFlux 响应式）
- ORM：MyBatis Plus 3.5.x（jakarta 版本）
- 连接池：Druid 1.2.23（jakarta 版本）
- 鉴权：Shiro 2.0.x（jakarta 版本）
- 规则引擎：Drools 8.x
- JSON：FastJSON2（com.alibaba.fastjson2）
- API 文档：springdoc-openapi 2.x（替换 springfox）
- 前端管理端：Vue 3.4 + Vite 5 + Element Plus 2.x + Pinia 2 + ECharts 5 + dart-sass

【绝对禁止】
1. 不要改业务逻辑（controller/service/mapper 的业务代码保持原样，只做语法迁移）
2. 不要重构代码结构（包名、类名、方法名不变）
3. 不要新增功能（只做技术栈迁移，不加业务）
4. 不要删除任何文件
5. 不要问我"要不要这样改"——按目标技术栈直接改，改完告诉我改了什么

【工作方式】
- 你改代码前，先读相关文件确认现状
- 改完后，告诉我改了哪些文件、改了什么
- 如果遇到我没提到的依赖冲突，你自己判断怎么对齐版本，告诉我你的决策
```

---

## 一、W1：后端基础迁移（10 个任务）

### T-001：JDK 21 环境切换

**粘贴 prompt**：
```
任务：把项目 JDK 从 1.8 升到 21 LTS。

操作：
1. 读 D:\MyCode\pinda-tms\pom.xml，找到 java.version / maven.compiler.source / maven.compiler.target
2. 把这些值从 1.8 改成 21
3. 找到 <release> 标签（如果有），改成 21
4. 检查所有子模块 pom.xml（pd-authority / pd-web / pd-dispatch / pd-service-api / pd-base / pd-oms / pd-common / pd-work / pd-netty / pd-user / pd-aggregation / pd-druid），如果子模块有覆盖 java.version 的，同步改成 21
5. 检查 Dockerfile（deploy/ 目录下），如果基础镜像是 openjdk:8 / jdk:8，改成 eclipse-temurin:21-jdk

验收：
- 所有 pom.xml 的 java.version = 21
- Dockerfile 基础镜像 = eclipse-temurin:21-jdk
```

---

### T-002：Spring Boot 3.3 + Spring Cloud 2023 + Alibaba 2023 版本对齐

**粘贴 prompt**：
```
任务：把 Spring Boot / Spring Cloud / Spring Cloud Alibaba 版本升到目标版本。

操作：
1. 读根 pom.xml，找到 <parent> 里的 spring-boot-starter-parent
2. 版本从 2.2.5.RELEASE 改成 3.3.5（或最新 3.3.x）
3. 找到 <properties> 里的 spring-cloud.version，从 Hoxton.SR3 改成 2023.0.3（或最新 2023.0.x）
4. 找到 spring-cloud-alibaba.version，从 2.2.1.RELEASE 改成 2023.0.3.2（或最新 2023.0.x 对应版本）
5. 检查 dependencyManagement 里有没有硬编码的 Spring Boot 版本，对齐到 3.3.x

注意：版本号要互相兼容——Spring Boot 3.3.x 对应 Spring Cloud 2023.0.x，Spring Cloud Alibaba 2023.0.x。

验收：
- spring-boot-starter-parent = 3.3.x
- spring-cloud.version = 2023.0.x
- spring-cloud-alibaba.version = 2023.0.x
```

---

### T-003：javax → jakarta 全量迁移

**粘贴 prompt**：
```
任务：把所有 Java 文件的 javax.* import 迁移到 jakarta.*。

背景：Spring Boot 3.x 强制使用 jakarta 命名空间，javax 已经废弃。

操作：
1. 在项目根目录运行：Get-ChildItem -Recurse -Filter *.java | Select-String "import javax\." 找出所有需要迁移的文件
2. 按包名映射替换：
   - javax.servlet.* → jakarta.servlet.*
   - javax.persistence.* → jakarta.persistence.*
   - javax.validation.* → jakarta.validation.*
   - javax.annotation.* → jakarta.annotation.*
   - javax.inject.* → jakarta.inject.*
   - javax.transaction.* → jakarta.transaction.*
   - javax.websocket.* → jakarta.websocket.*
   - javax.mail.* → jakarta.mail.*
   - javax.xml.bind.* → jakarta.xml.bind.*
3. 每个文件改完后确认 import 语句正确

重点检查模块：pd-authority（Shiro 过滤器用 servlet）、pd-web（Controller 用 validation）、pd-common（工具类）

验收：
- 全局搜 "import javax." 结果为 0（javax.crypto / javax.net 这种 JDK 自带的不算，只找 javax.servlet/persistence/validation/annotation/inject/transaction/websocket/mail/xml.bind）
```

---

### T-004：MyBatis Plus 3.5 + Druid 1.2 升级

**粘贴 prompt**：
```
任务：升级 MyBatis Plus 和 Druid 到 jakarta 兼容版本。

操作：
1. 找 MyBatis Plus 版本（根 pom 或子 pom），从 3.3.0 改成 3.5.7（或最新 3.5.x）
2. 找 Druid 版本，从 1.1.22 改成 1.2.23
3. 注意：Druid 在 Spring Boot 3 下要用 druid-spring-boot-3-starter（不是 druid-spring-boot-starter），检查 pom 里的 artifactId，改成 druid-spring-boot-3-starter
4. MyBatis Plus 在 Spring Boot 3 下要用 mybatis-plus-spring-boot3-starter，检查 artifactId，改成 mybatis-plus-spring-boot3-starter

验收：
- mybatis-plus artifactId = mybatis-plus-spring-boot3-starter
- druid artifactId = druid-spring-boot-3-starter
```

---

### T-005：Shiro 2.0 升级

**粘贴 prompt**：
```
任务：把 Shiro 从 1.4.0 升到 2.0.x（jakarta 版本）。

操作：
1. 找 Shiro 版本，改成 2.0.0（或最新 2.0.x）
2. Spring Boot 3 下要用 shiro-spring-boot3-starter（不是 shiro-spring-boot-starter），检查 artifactId
3. 检查 Shiro 相关的 Filter / Realm / Configuration 代码：
   - import 里的 javax.servlet 已在 T-003 改成 jakarta.servlet
   - Shiro 2.0 的 API 有变化（比如 SomeFilter 类路径变了），按编译错误修

验收：
- shiro artifactId = shiro-spring-boot3-starter
- mvn compile 通过 Shiro 相关模块
```

---

### T-006：FastJSON2 替换

**粘贴 prompt**：
```
任务：把 FastJSON 1.x 替换成 FastJSON2。

操作：
1. 找 FastJSON 依赖（com.alibaba:fastjson），改成 com.alibaba.fastjson2:fastjson2，版本 2.0.53（或最新 2.x）
2. 全局搜 import com.alibaba.fastjson.，改成 import com.alibaba.fastjson2.
3. FastJSON2 API 基本兼容：
   - JSON.toJSONString() / JSON.parseObject() / JSON.parseArray() 都保留
   - JSONObject / JSONArray 也保留，包名换成 com.alibaba.fastjson2
4. 如果有 @JSONField 注解，包名也要换

验收：
- 全局搜 "import com.alibaba.fastjson."（不带 2）结果为 0
- pom 里 fastjson 依赖改成 fastjson2
```

---

### T-007：springdoc-openapi 替换 springfox

**粘贴 prompt**：
```
任务：把 springfox-swagger 替换成 springdoc-openapi 2.x。

背景：springfox 已停更，不支持 Spring Boot 3。

操作：
1. 找 springfox 相关依赖（springfox-swagger2 / springfox-swagger-ui），全部删除
2. 加 springdoc-openapi-starter-webmvc-ui 依赖，版本 2.6.0（或最新 2.x）
3. 全局搜 swagger 注解：
   - @Api → @Tag
   - @ApiOperation → @Operation
   - @ApiModel → @Schema
   - @ApiModelProperty → @Schema
   - @ApiParam → @Parameter
4. import 全部从 springfox.io.swagger.annotations 换成 io.swagger.v3.oas.annotations.*

验收：
- pom 里无 springfox 依赖
- 全局搜 "import springfox" 结果为 0
- 全局搜 "import io.swagger.annotations" 结果为 0
```

---

### T-008：Zuul 1.x → Spring Cloud Gateway 迁移

**粘贴 prompt**：
```
任务：把网关从 Zuul 1.x 改成 Spring Cloud Gateway。

背景：Zuul 1.x 是 Netflix 的老网关，不支持 Spring Boot 3。Spring Cloud Gateway 是官方替代。

操作：
1. 找到网关模块（应该在 pd-authority/pd-apps/pd-gateway 或类似目录）
2. 删除 Zuul 依赖（spring-cloud-starter-netflix-zuul）
3. 加 Spring Cloud Gateway 依赖（spring-cloud-starter-gateway）
4. 注意：Gateway 是 WebFlux 响应式，不是 Servlet。检查网关模块的 pom：
   - 如果有 spring-boot-starter-web（Servlet），要改成 spring-boot-starter-webflux（响应式），或者删掉 spring-boot-starter-web
   - Gateway 不能同时有 spring-boot-starter-web 和 spring-boot-starter-webflux
5. 迁移路由配置：
   - 原来 Zuul 的路由配置（zuul.routes.*），改成 Spring Cloud Gateway 的路由配置（spring.cloud.gateway.routes.*）
   - 如果原来用 Nacos 配置路由，保持 Nacos，只改配置格式
6. 迁移过滤器：
   - 原来 Zuul Filter（继承 ZuulFilter）改成 Gateway GlobalFilter
   - 重点：Token 鉴权过滤器、401 处理过滤器
   - Zuul Filter 的 run() 方法 → Gateway GlobalFilter 的 filter() 方法
   - RequestContext → ServerWebExchange

先读现有网关代码，再改。

验收：
- 网关模块 pom 无 zuul 依赖
- 网关模块 pom 有 spring-cloud-starter-gateway
- 网关启动后，/api/auth/anno/login 接口可访问
```

---

### T-009：Drools 8.x 升级

**粘贴 prompt**：
```
任务：把 Drools 从 6.5.0 升到 8.x。

背景：Drools 7+ API 完全重写，运费规则 DSL 要重写。

操作：
1. 找 Drools 依赖（drools-core / drools-compiler / drools-decisiontables），改成 Drools 8.x（org.drools:drools-core:8.x）
2. 读现有运费规则代码（应该在 pd-base 或 pd-service-api 里，搜 KieSession / KieHelper / drools）
3. Drools 7+ 的 API 变化：
   - KieServices / KieContainer / KieSession 保留，但包路径可能变了
   - kmodule.xml 配置可能要改
4. 读现有运费规则 DSL（.drl 文件），语法基本兼容，但可能要调整 import

先读现有 Drools 相关代码和 .drl 文件，再改。

验收：
- drools 依赖版本 = 8.x
- mvn compile 通过 Drools 相关模块
- 运费试算接口（mailing/totalPrice）能跑通
```

---

### T-010：Seata 2.x 版本对齐

**粘贴 prompt**：
```
任务：把 Seata 从 1.2.0 升到 2.x。

操作：
1. 找 Seata 依赖（spring-cloud-starter-alibaba-seata），版本对齐到 Spring Cloud Alibaba 2023.0.x 对应的 Seata 版本
2. 检查 @GlobalTransactional 注解的 import，应该还是 io.seata.spring.annotation.GlobalTransactional
3. 检查 seata 配置文件（registry.conf / file.conf / application.yml 里的 seata.*），对齐到 2.x 格式

注意：Seata Server 也要部署 2.x 版本，但这是后续任务，先把客户端版本对齐。

验收：
- seata 依赖版本 = 2.x
- @GlobalTransactional 注解正常 import
```

---

## 二、W2：前端管理端（4 个任务）

> 中间件升级（MySQL 8.0 / Redis 7 / Nacos 2.3 / RabbitMQ 3.12）已于 2026-10-09 完成，无对应执行任务；现网版本、容器名、`.env` 落点与 dataId 见《31》《26》§1。⚠️ 消息总线为 RabbitMQ 单总线（《03》BA-09），Kafka/Zookeeper 已移除；勿按旧文档把 RabbitMQ 当待移除组件清理——订单事件 4 个 `@RabbitListener`（`pd-dispatch/OrderEventMQListener` 3 个 + `pd-oms/SettlementDeliveredListener` 1 个）正在跑短信与应收结算单（权威口径见《26》§1.1）。

### T-015：管理端 Vite + Vue3 + Element Plus 工程搭建

**粘贴 prompt**：
```
任务：新建管理端 Vue3 + Vite 工程（替换旧 Vue2 工程）。

操作：
1. 在 D:\MyCode\pinda-tms\pd-admin-ui 目录下（或新建 pd-admin-ui-v3），用 Vite 脚手架新建 Vue3 工程：
   - 包管理器：npm
   - Vue 版本：3.4
   - 不要用 Vue CLI，用 Vite
2. 安装依赖：
   - element-plus（最新 2.x）
   - vue-router@4
   - pinia
   - axios
   - sass（dart-sass，不要 node-sass）
   - echarts@5
3. 配置 vite.config.js：
   - 代理 /api 到 http://localhost:8760（网关）
   - 端口 9528（和旧工程一致）
4. 新建登录页（简单的用户名密码表单，POST 到 /api/auth/anno/login）
5. 新建主布局：侧边栏（菜单）+ 顶栏（用户信息）+ 内容区

先读旧工程的 package.json 和 src/views/ 目录结构，了解有哪些页面，新工程保持目录结构一致。

验收：
- npm run dev 秒级启动
- 登录页可访问，输入账号密码能登录
- 登录后进入主布局，侧边栏显示菜单
```

---

### T-016：动态路由对接后端菜单

**粘贴 prompt**：
```
任务：把管理端动态路由对接后端菜单接口。

背景：旧 Vue2 工程有 filterAsyncRouter 逻辑，根据后端返回的菜单数据动态生成路由。Vue3 + Pinia 要重写。

操作：
1. 读旧工程的路由生成逻辑（搜 filterAsyncRouter / addRoutes / permission.js）
2. 在新 Vue3 工程里用 Pinia store 实现：
   - 登录后调用 /api/auth/anno/menu 或类似接口获取菜单树
   - 根据菜单树动态生成路由（component 用 import.meta.glob 加载 views 下的 .vue）
   - 动态 addRoute 到 router
3. 保持后端菜单数据结构不变（pd_auth_resource 表）

验收：
- 登录后侧边栏菜单显示正确
- 点击菜单能跳转对应页面
```

---

### T-017：系统管理页重写（25 个 CRUD）

**粘贴 prompt**：
```
任务：把旧 Vue2 工程的系统管理页重写成 Vue3 + Element Plus 版本。

背景：旧工程 pd-admin-ui/src/views/pinda/ 下有 43 个 .vue 文件，全是系统管理类（auth/base/developer/file/msgs/ofpay/sms/trace/user）。

操作：
1. 读旧工程每个 .vue 文件，了解页面结构（表格 + 搜索表单 + 新增/编辑弹窗）
2. 在新 Vue3 工程里逐个重写：
   - 用 <script setup> 组合式 API
   - 用 Element Plus 组件（el-table / el-form / el-dialog / el-pagination）
   - 用 Pinia store 管理状态
   - API 调用保持和旧工程一致（路径不变）
3. 先做核心页：用户管理 / 角色管理 / 菜单管理 / 机构管理 / 字典管理 / 操作日志
4. 其他页（文件管理 / 消息 / 支付 / 短信 / 轨迹）批量做

不要逐字复制旧代码，用 Vue3 风格重写，但保持功能和接口路径一致。

验收：
- 系统管理所有页面可访问
- 每个页面的 CRUD 功能正常
```

---

### T-018：dart-sass 替换 node-sass

**粘贴 prompt**：
```
任务：把 node-sass 替换成 dart-sass。

背景：node-sass 已废弃，不支持 Node 17+，导致 CI 构建失败。

操作：
1. 新 Vue3 工程已经用 Vite，默认用 dart-sass（不用装 node-sass）
2. 检查 package.json，如果有 node-sass 依赖，删除
3. 装 sass（dart-sass 的 npm 包名）
4. 检查所有 .vue 文件的 <style lang="scss">，语法基本兼容

验收：
- package.json 无 node-sass
- package.json 有 sass（dart-sass）
- npm run build 成功
```

---

## 三、W3：联调 + 开源基础设施（5 个任务）

### T-019：全链路冒烟验证

**粘贴 prompt**：
```
任务：验证技术栈升级后全链路可跑通。

操作：
2. mvn compile 全模块编译
3. 逐个启动 14 个微服务（或 docker compose up 启动所有）
4. 启动管理端前端（npm run dev）
5. 跑通冒烟用例：
   - 登录（POST /api/auth/anno/login）
   - 下单（POST /api/oms/mailing）
   - 调度（分配运单）
   - 轨迹上报（POST /api/netty/push）
   - 签收（POST /api/web-courier/delivered/{id}/{status}）

把每一步的结果告诉我：成功 / 失败 / 报错信息。
```

---

### T-020：CI/CD 工作流调整

**粘贴 prompt**：
```
任务：调整 Gitea Actions CI 工作流，适配 JDK 21。

操作：
1. 读 D:\MyCode\pinda-tms\.gitea\workflows\deploy.yml
2. 找构建步骤，如果用的 docker 镜像是 maven:3.x-jdk-8 / adoptopenjdk:8 之类，改成 maven:3.9-eclipse-temurin-21
3. 前端构建步骤，如果用 node:14 / node:16，改成 node:20（Vite 5 需要 Node 18+）
4. 检查构建产物路径是否正确

验收：
- workflow 文件里 maven 镜像 = jdk-21
- workflow 文件里 node 镜像 = node:20+
```

---

### T-021：LICENSE 文件

**粘贴 prompt**：
```
任务：在项目根目录创建 LICENSE 文件。

操作：
1. 在 D:\MyCode\pinda-tms\ 根目录创建 LICENSE 文件
2. 内容是 Apache License 2.0 全文（从 https://www.apache.org/licenses/LICENSE-2.0.txt 复制）
3. 在文件开头的 Copyright 行填：Copyright 2026 Pinda TMS Authors

另外创建一个 COMMERCIAL-LICENSE.md 文件，说明：
- 核心功能 Apache 2.0 开源免费
- 商业版高级功能（多租户/SSO/高级报表/开放API）需要授权
- 联系方式留你的 GitHub/Gitee 主页或邮箱
```

---

### T-022：README.md 初稿

**粘贴 prompt**：
```
任务：写项目 README.md。

操作：
1. 在 D:\MyCode\pinda-tms\ 根目录创建 README.md（如果已有，重写）
2. 内容结构：
   - 项目标题 + 一句话描述（开源物流运输管理系统）
   - badge（license: Apache 2.0 / Spring Boot 3.x / Vue 3.x / PRs welcome）
   - 项目截图（管理端首页 / 订单列表 / 调度地图 / 轨迹回放）——先用占位图，后续替换
   - 快速开始（docker compose up 一键启动，30 秒跑通）
   - 功能列表（订单/调度/在途监控/POD签收/财务结算）
   - 在线 Demo（留占位地址）
   - 文档链接（VitePress 文档站，后续建）
   - 贡献指南（CONTRIBUTING.md 链接）
   - 商业授权说明（多租户/SSO/高级报表需要商业授权）
   - 打赏/支持（微信收款码占位）

风格参考：RuoYi / JeecgBoot / pig 的 README，简洁有力。
```

---

### T-023：一键部署脚本

**粘贴 prompt**：
```
任务：写一键部署脚本，让新机器 clone 后 30 分钟跑通。

操作：
1. 在 deploy/ 目录创建 quick-start.sh（Linux）或 quick-start.ps1（Windows）
2. 脚本内容：
   - 检查 docker / docker compose 是否安装
   - 检查磁盘空间（至少 20GB）
   - docker compose -f docker-compose.infra.yml up -d（启动中间件）
   - 等待中间件健康（循环检查 MySQL / Nacos 端口）
   - docker compose -f docker-compose.app.yml up -d（启动微服务）
   - 输出访问地址：管理端 http://localhost:9528 / API 文档 http://localhost:8760/doc.html
3. 创建 .env.example 文件，列出所有需要配置的环境变量（数据库密码 / Nacos 地址 / 百度地图 Key 等）
```

---

## 四、执行顺序与依赖

```
T-001（JDK21）→ T-002（Boot/Cloud 版本）→ T-003（javax→jakarta）→ T-004~T-010（各依赖适配，可并行）
                                                                              ↓
T-015（前端工程搭建）→ T-016（动态路由）→ T-017（系统管理页）→ T-018（sass）
                                                                              ↓
T-019（全链路冒烟）→ T-020（CI/CD）→ T-021~T-023（开源基础设施）
```

---

## 五、完成判定（DoD）

- [ ] 19 个任务全部完成（T-001~T-010、T-015~T-023；T-011~T-014 中间件升级已于 2026-10-09 完成，不列入本手册）
- [ ] `mvn compile` 在 JDK 21 下通过
- [ ] 14 个微服务 docker compose up 全部 healthy
- [ ] 管理端 Vue3 可登录、菜单显示、系统管理页 CRUD 正常
- [ ] 走通 登录→下单→调度→轨迹→签收 端到端冒烟
- [ ] Gitee/GitHub 仓库可公开（README/LICENSE 就绪）
- [ ] 新机器 clone 后 docker compose up 30 分钟跑通

