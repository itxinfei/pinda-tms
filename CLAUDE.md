# 心飞物流TMS — Claude Code 项目入口（精简版）

> 本文件每次会话自动加载，只放"必须最先知道"的路由与硬事实；细节一律下沉到 `docs/需求文档/` 子文档，勿内联重复内容。

## 0. 开工前必读（先读后写）
1. `06-开发规范与需求规格说明书.md`（SRS）第 1 章——宪法级红线，最高优先级。
2. `12-ClaudeCode开发纪律与红线.md`——摘要 + 七条硬事实 + §8 横切基线红线 10 条（§8.1~§8.10，必读）。
3. `00-索引.md`——需求文档总入口，按需检索其余文档。
4. 写管理端页前：`14-管理端UI规范与页面模板.md` + `16-管理端Vue3升级专项实施规范.md`。
5. 写移动端页前：`13-UI设计规范与页面模板.md`（uni-app）。
6. 改后端前：`08-后端接口文档.md`（字段契约唯一真源，字段实名查附录 A.1）+ `07-数据字典与枚举口径.md`（枚举禁硬编码；状态码查 §〇之二，订单=紧凑 1~12，旧码已清理）。
7. 写/改管理端菜单、路由、布局前：`58-管理端菜单与路由开发规范.md`。
8. 删/改管理端依赖、组件、views 前：`59-管理端技术栈保留与整合策略.md`（预置栈全保留，防误删）。
9. 写技术选型 / 新增依赖前：`60-技术栈评估与演进路线.md`（Seata 不引入；Flowable/Debezium/Doris/JimuReport/快递100/Spring AI 全预留）。
10. 修 bug / 补功能前：`docs/测试/66-测试执行_基础横切.md` 与《12》§8 红线——P0~P2 横切缺陷（D-01~07/X-01~04/F-01~11）先对号入座，勿重新发现。

> 与权威文档冲突的"优化"默认无效，须先修订文档并经人工确认。

## 1. 语言
中文（简体）交互；代码注释中文优先。

## 2. 技术栈定调（2026-10 决议，全栈升级为准）
- **后端**：JDK 21 LTS + Spring Boot 3.5.7 + Spring Cloud 2025.0.0 + Alibaba 2025.0.0.0（jakarta.*）；根 pom 已落地，`mvn compile` 全模块通过。
- **管理端 `pd-admin-ui-vue3`**：Vue ^3.5 + Vite ^8 + TS ^6 + Pinia ^4 + Vue Router ^5 + Element Plus ^2.14 + ECharts ^6。版本以 package.json 为准。预置技术栈（vxe-table/WangEditor/bpmn-js/@form-create/exceljs 及 demo 页）全保留，禁删。
- **移动端**：uni-app（Vue3/Vite）+ uview-plus；pnpm monorepo 三独立子工程 `driver/courier/customer`，构建 `pnpm -F pd-mobile-<端> build:h5`。
- **服务间容错**：Resilience4j 2.2.0（替代 Hystrix/Sentinel）；MapStruct 1.6.3（替代 BeanUtils）、ArchUnit 1.3.0、JDK21 虚拟线程。
- ⚠️ 旧"JDK 锁死 1.8 / 不升级依赖 / 管理端 Vue3 不做"已废止，AI 不得据此拒绝升级任务。

## 3. 文档索引（权威源，改动前先读对应文档）
| 文档（`docs/需求文档/`） | 用途 |
|---|---|
| `06-开发规范与需求规格说明书.md`（SRS v3） | 总纲：红线/技术栈/架构/资产/交付/验收 |
| `55-功能需求详细规格.md` | 功能需求详细规格唯一真源（101 条/29 前缀） |
| `12-ClaudeCode开发纪律与红线.md` | 红线摘要 + 七条硬事实 + §8 基线红线 10 条 |
| `16-管理端Vue3升级专项` / `14-管理端UI规范` / `13-UI设计规范` | Vue3 迁移 / 管理端 / 移动端样式模板 |
| `08-后端接口文档.md` | 接口全量 + 附录 A.1 字段实名字典（契约唯一真源） |
| `07-数据字典与枚举口径.md` | 状态枚举（禁硬编码）+ §〇之二 状态码反幻觉闸 |
| `29-阶段1技术栈现代化周计划` / `53-全量重构总体规划` | 升级排期 / 重构执行总纲（R1-R8/DoD/回退） |
| `54-审查台账` | BA/EC/Q 审查编号唯一定义源 |
| `57-账号密码与凭据总表.md` | 全环境账号/口令/密钥唯一真源（查密码先查此） |
| `58-管理端菜单与路由开发规范` | 菜单/路由/布局硬规则 |
| `59-管理端技术栈保留与整合策略` | 预置技术栈全保留 + 整合新栈三步 |
| `60-技术栈评估与演进路线` | 演进与预留项真源（Seata 不引入等） |
| `03-全局定案与待确认项清单.md` | D-01~D-63 定案源；D-63 = 文档五大真源锚点体系 |
| `../测试/65-全量测试计划_v2.md` | 测试策略唯一真源；§L0 = 《12》§8 红线↔检测点对应表 |
| `00-索引.md` | 需求文档总入口 |

## 4. 七条硬事实（违反即失败）
1. **所有请求走网关** `http://<host>:8760/api`+端前缀；直连 pd-web-* 8161~8164 被 `TokenAuthInterceptor` 401 拒绝。
2. **JWT 头字段是 `token`**（非 `Authorization`），有效期 7200s，**无 refreshToken** → 401 清 token 重登。
3. **轨迹查询只认 `businessId`+`type`**（无 `orderId`）；前端不得伪造参数。
4. **新增对外接口仍须注册 `pd_auth.pd_auth_resource` 并绑角色**（RBAC，手工插表，无自动注册）。新 `pd-gateway`（Sa-Token）只校验登录态 + Same-Token 防直连；未注册接口不再被网关拦，但菜单/权限不可见，注册绑定仍强制。
5. **网关鉴权**：新 `pd-gateway`（Sa-Token+Redis）登录校验 + Same-Token 防直连。菜单动态下发（D-42）未实现（pd-auth 无 menu/router 端点），菜单空先确认功能未建、勿当缓存问题排查。
6. **严禁新建/重写状态机**：`StateTransitionValidator` 已在用；真实缺口是跨端同步桥。
7. **环境**：Docker 网 `172.21.0.x`；宿主 `192.168.20.130`；Nacos `8848`（ns `1cb93ce4…`，group `xinfei-tms`，鉴权已开，无 token 403）。dev 统一账号 `nacos/123456`（13 微服务+CI）；`xinfei/123456` 仅命名空间读写备用。

> 历史机制已删（2026-10-10）：旧 `pd-authority` 的 `AccessFilter` 资源关卡随模块删除不复存在；新网关口径见硬事实 4。

## 4.1 架构口径变更（2026-10-10 拍板）
- 旧 `pd-authority` 已废弃删除，禁再基于它开发/引用；认证由新建 `pd-auth`（Sa-Token+多租户+验证码，端口 9000）承担，网关为新建 `pd-gateway`（Spring Cloud Gateway）。
- `codeback/` 是重构前旧代码备份（gitignore、未跟踪、不编译），定位为领域参考与旧知识对照；**严禁直接复制旧代码进业务模块**，旧知识以《52-旧代码独占知识固化》为准。
- 全量重构已立项（D-61）：以 codeback/ 为参考、按《08》契约 / 《07》编码 / 《27》目标断言，全面重设计开发业务 Java/中间件/数据库（允许重设计表结构与内部分层，走 create-migration；禁夹带未拍板业务口径变更）。顺序与 DoD 见《53-全量重构总体规划》。

## 5. 关键纪律
- 🔴 **代码必须走 Git**：本地改 → 编译自测 → `git diff` 给用户确认 → commit（中文 `type: 简述`）→ push 双远端（Gitea+Gitee）→ 服务器 `/data/xinfei-tms` 内 `git pull` → ci 构建 → compose 滚动部署。**严禁** scp 上传源码或 ssh 直改服务器文件（使工作区分叉、无审计）。仅仓库链路不可用时经用户同意可临时直改，事后立即回填并在《99》台账登记。dev 库 `UPDATE/INSERT` 修正也须先报完整语句 + `WHERE` 经确认再执行，并同步 `sql/` 种子脚本防复发。
- **多会话并行**：开工前读 `99-任务认领台账.md`，只改自己认领的模块/文件；"进行中"条目禁止其他会话动手，收工销账。
- 先读后写；复用既有 API；禁止过度设计（YAGNI）；禁硬编码（cron/阈值/地图 Key/状态数字）；前端请求过 `src/utils/request` 拦截器。
- **严禁跨模块写业务**：订单→pd-oms、调度→pd-dispatch、作业→pd-work、基础→pd-base、轨迹→pd-netty。
- 仅 Vue3 专项内可改 `pd-admin-ui`；第三方库替换须经专项规范决策，禁"顺手"引第二个同类库/新中间件。
- 🔴 **SQL 一律放仓库根 `sql/<业务域>/`、不进 git**（已在 .gitignore，手动维护）：建库/迁移/种子/补丁按 `auth/base/oms/dispatch/work/member/iot/settlement/seed/infra/patch` 分域；新增上线迁移走 `.claude/skills/create-migration`。禁在 docs/codeback 等新增会被 git 跟踪的 .sql（触发 CI DB 门禁 `deploy-backend.yml`，diff 含 `*.sql` 须 `db_approved=true` 放行）。

## 5.1 已生效变更与约束（2026-10-10 起，勿回退）
> 以下为已落地生效的约束与参考事实；详细变更历史见 git log，不赘述。

- **前端 API 前缀**：统一 `/api`，勿改回 `/prod-api`（宝塔反代已按 `/api` 配）。
- **i18n**：界面文案走 zh-cn/en 语言包，写死中文视为缺陷；品牌名/英文 slogan 不翻译。
- **favicon 换版**：改 `index.html` 引用加 `?v=版本号` query 绕过缓存。
- **移动三端登录**：提交体必须带 `tenantCode`（统一 `xinfei`）。
- **管理端菜单**：平级铺开，不做前端归组；无 title 的 `/` 根 Layout 壳须在侧边栏/顶栏取数时拍平（`useLayout.ts` 的 `flattenSidebarRoutes`），禁直接 filter 原始树；**禁止单独开动态路由开关**（4 前提缺一不可，见《58》）。
- **CI/CD**：`.gitea/workflows/build-check.yml` push 自动编译门禁；`deploy-backend.yml` 仅手动 Run workflow；前端统一 `bash /data/xinfei-deploy/deploy-fe.sh <admin|customer|courier|driver>`。自动部署已停，自动编译门禁仍在。
- **品牌色**：全端统一心飞红 `#FF0033`（按压 `#CC0029`、浅底 `#FFE5EA`）；原神领橙红 `#E15536`/管理端蓝 `#2B6CFF` 已废止（见《03》D-10、《13》§2.1、《14》§3）。
- **线上 dev 账号（密码均 123456）**：管理端 `admin`(tenantCode=xinfei)；三端 `driver04`/`courier01`/`customer01`；统一演示 `demo`。`member_info`/`base_truck_driver` 仍空（业务数据无种子）。
- **Logo 落地**：品牌资产库 `docs/brand/`；管理端 `public/favicon.ico` + `src/assets/images/`（logo-mark.png 图形 / logo-full.png 组合 / logo.png 旧组合标已不引用 / login-hero.png）；移动三端 `src/static/`（logo-mark-transparent.png 图形透明标 / logo-full-login.png 登录组合标 / logo-app.png App 图标 / favicon.ico / logo-<48~1024>.png 供 manifest），登录页 `/static/logo-full-login.png`。
- **Nacos 分组迁移 xinfei-tms（2026-10-14 已生效）**：13 个后端服务 yml（含 `pd-web/pd-web-*` 嵌套目录，共 42 处 `group:`）+ Nacos 服务端 14 个配置已从 `pinda-tms` 组迁至 `xinfei-tms` 组，滚动部署后全部服务已验证注册新分组；旧组配置暂留作回退，稳定后可删。
- **服务器品牌文案（2026-10-14 已改）**：Gitea `APP_NAME = 心飞物流`（已重启生效，配置备份 `.bak-xinfei`）、宝塔 `pinda-fronts.conf` 与 `/data/pinda-deploy/deploy-fe.sh` 注释已替换；`/data/pinda-tms/deploy/` 10 个文件的"品达TMS"注释已改（均带 `.bak-xinfei` 备份）。镜像/容器/库名等运行时标识符由 `0c41133` 停机迁移统一处理，勿散改。
- **CI 前端部署哨兵值修复（2026-10-14，提交 72463356）**：`build-check.yml` 全量收敛路径曾把 `APPS=ALL` 直拼镜像名导致 `pinda/pd-ALL-h5` 非法标签，已改为真实四端列表 `admin driver courier customer`；后端 `SERVICES=ALL→$ORDER` 翻译逻辑不变。
- **xinfei/xinfei-tms 仓库已建（2026-10-14）**：Gitea 新组织 `xinfei`（full_name: Xinfei Logistics）+ 公开仓库 `xinfei/xinfei-tms`，master 已推送；workflow 的 clone URL 从此仓库拉取，`check_only` 门禁与 fe-compile 已验证跑绿。本地 git remote 共三个：`gitea`（pinda/pinda-tms 旧）、`origin`（Gitee）、`xinfei`（新，过渡期与 gitea 同步推）。旧仓库暂不删，停机迁移收口后再定去留。

## 5.2 当前阶段工作定调（🔴 2026-10-11 拍板，最高优先级）
> 权威全文：SRS《06》§1.6；执行摘要《12》§0/§4。项目处于内网 dev、半成品、无真实用户、不上线；开发一切围绕管理端可见可用展开。

1. **唯一主战场 = 管理端 `pd-admin-ui-vue3`**：按《55》《14》《16》做全做完、设计好；DoD = 写完 → 提交 → `bash /data/xinfei-deploy/deploy-fe.sh admin` 部署 → 192.168.20.130 浏览器可见可用。后端任务以"打通管理端所需接口/数据"为优先级，不为架构完美阻塞页面。
2. **dev 不做生产级安全功课**：中间件 dev 口令统一 `123456`（MySQL/Redis/RabbitMQ/Nacos），默认值可直接写 yml/compose；不要求密钥外置/0600/轮换。已落地的 Sa-Token/网关/多租户保留运行、不深化不拆除。
3. **底线**：真实第三方凭证（地图 AK、微信/支付宝商户密钥、短信密钥）不入库，走环境变量/部署注入；真实用户/公网上线前另立生产加固清单统一收紧。
4. 本条与其他文档/记忆中的旧安全口径冲突时，dev 阶段以本条为准；不构成上线后的永久豁免。
5. **决策边界：用户定方向，实现层不请示**（权威全文 SRS《06》§1.6-5、《12》§0-4）：
   - **问**：业务口径变更、新依赖/新中间件、表结构重设计、跨 3+ 文件架构改动、UI 方向取舍、生产/不可逆操作、红线清单配置文件。
   - **不问直接做**：符合《阿里巴巴 Java 开发手册》的常规实现；符合市场通行规范的选择（RESTful/HTTP 状态码/分页/BCrypt/标准组件）；国家/行业强制合规细节；简单 bug、编译错误、判空边界、文档已明确内容。历史"拍板/待确认"是留痕不是待办，照做不提请；拿不准默认按实现做并注明决策点。

---

*本文件为入口。任何与 `docs/需求文档/` 权威文档冲突的"优化"默认无效，须先修订文档并经人工确认。*

<!-- open-wot agent instructions start -->
## Wot UI Agent Instructions

Before generating or modifying wot-ui component code, read the project Skill at `.claude/skills/wot-ui-v2/SKILL.md` and query the configured `wot-ui` MCP server for version-accurate APIs and examples.

<!-- open-wot agent instructions end -->
