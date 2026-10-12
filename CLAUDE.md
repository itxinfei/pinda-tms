# 品达 TMS — Claude Code 项目入口（精简版）

> 本文件**每次会话自动加载**，只放"必须最先知道"的路由与硬事实；细节一律下沉到 `docs/需求文档/` 子文档，按下方顺序读，勿内联重复内容。
> 最后更新：2026-10-08（精简上下文：删除历史/重复章节，细节移入子文档）

---

## 0. 开工前必读（按顺序，先读后写）
1. `docs/需求文档/06-开发规范与需求规格说明书.md`（SRS）**第 1 章**——宪法级红线，最高优先级。
2. `docs/需求文档/12-ClaudeCode开发纪律与红线.md`——执行级摘要 + 七条硬事实（必读）。
3. `docs/需求文档/00-索引.md`——需求文档总入口，按需检索其余文档。
4. 写**管理端**页前：`14-管理端UI规范与页面模板.md` + `16-管理端Vue3升级专项实施规范.md`。
5. 写**移动端**页前：`13-UI设计规范与页面模板.md`（uni-app）。
6. 改**后端**前：`08-后端接口文档.md`（契约）+ `07-数据字典与枚举口径.md`（枚举，禁硬编码数字）。
7. 写/改**管理端菜单、路由、布局**前：`58-管理端菜单与路由开发规范.md`（动态链路断点 / 布局拍平 / 路由命名约束，防踩坑，2026-10-12 新增）。
8. 删/改**管理端依赖、组件、views 页面**前：`59-管理端技术栈保留与整合策略.md`（预置技术栈全保留，防误删；整合新栈三步，2026-10-12 用户定稿）。
9. 写**技术选型 / 新增依赖 / 演进规划**前：`60-技术栈评估与演进路线.md`（行业对照 + P0/P1/P2 路线；**Seata 不引入**、Flowable 预留、Debezium/Doris/JimuReport/快递100/Spring AI 全预留，2026-10-12 定稿）。

> 与权威文档冲突的"优化"默认无效，须先修订文档并经人工确认。

---

## 1. 语言
中文（简体）交互；代码注释中文优先。

## 2. 技术栈定调（全栈升级为准，2026-10 决议）
- **后端**：JDK 1.8 + Spring Boot 2.2.5 → **JDK 21 LTS + Spring Boot 3.5.7 + Spring Cloud 2025.0.0 + Alibaba 2025.0.0.0**（jakarta.*；2026-10-10 修订，**代码基线已同步落地**：根 pom 已升至此版本、`mvn compile` 全模块通过）。版本清单以《阶段1 技术栈现代化周计划》/ SRS §2.1 为准。
- **管理端 `pd-admin-ui-vue3`**：Vue 2.6 → **Vue ^3.5.41 + Vite ^8.2.1 + TS ^6.0.3 + Pinia ^4.0.2 + Vue Router ^5.2.0 + Element Plus ^2.14.4 + ECharts ^6.1.0**（以 package.json 实测为准，勿凭旧文档版本号）。**预置技术栈全部保留**：vxe-table / WangEditor / bpmn-js / @form-create / exceljs 及 demo 等演示页，需求未定前**禁止删除**（2026-10-12 用户定稿，见《59》）。
- **移动端**：uni-app（Vue3/Vite）+ uview-plus（新建，三端分包）。
- **服务间容错（2026-10-10 定调）**：统一往 **Resilience4j 2.2.0** 方向发展（Spring Cloud CircuitBreaker，替代 Hystrix），Feign 熔断开关开启后 Fallback 生效；**禁止再引 Hystrix/Sentinel**。同期引入 MapStruct 1.6.3（替代 `BeanUtils.copyProperties`，逐步迁移）、ArchUnit 1.3.0（架构红线可执行化，test 作用域）、JDK21 虚拟线程。详见 SRS §2.1 /《53》R2。
- 当前阶段：阶段 1 技术栈现代化进行中（详见周计划）。
- ⚠️ 旧版"JDK 锁死 1.8 / 不升级依赖 / 管理端 Vue3 不做"**已废止**，AI 不得据此拒绝升级任务。

## 3. 文档索引（权威源，改动前先读对应文档）
| 文档（`docs/需求文档/`） | 用途 |
|---|---|
| `06-开发规范与需求规格说明书.md`（SRS v3.0） | 总纲：红线 / 技术栈 / 架构 / 资产盘点 / 交付 / 验收（功能需求规格已拆出至《55》） |
| `55-功能需求详细规格.md` | **功能需求详细规格唯一真源**（SRS §6/§7/§8/附录A 拆出，章节号不变；101 条/29 前缀）——写业务功能/页面/需求规格前必读 |
| `12-ClaudeCode开发纪律与红线.md` | 红线执行摘要 + 七条硬事实 |
| `16-管理端Vue3升级专项实施规范.md` | 存量 Vue2 页面迁移、第三方库替换决策（本期新建） |
| `14-管理端UI规范与页面模板.md` / `13-UI设计规范与页面模板.md` | 管理端 / 移动端样式模板 |
| `08-后端接口文档.md` | 接口全量（含网关前缀） |
| `07-数据字典与枚举口径.md` | 状态枚举对照（禁硬编码数字） |
| `29-阶段1_技术栈现代化_周计划.md` | 全栈升级排期 |
| `53-全量重构总体规划.md` | **全量重构执行总纲**（D-61 重设计口径：规格基准/R1-R8 顺序/DoD/回退）——重构任务开工前必读 |
| `54-审查台账.md` | **BA/EC/Q 三系列审查编号唯一定义源**（自《03》§七 迁入）——引用 `BA-`/`EC-`/`Q-` 编号先查此文档 |
| `57-账号密码与凭据总表.md` | **全环境账号/口令/密钥唯一真源**（2026-10-11 已对 192.168.20.130 逐项实测）：中间件/控制台/业务账号/凭据文件落点/核实与轮换命令——查密码先查此文档 |
| `58-管理端菜单与路由开发规范.md` | 菜单/路由/布局硬规则（动态链路断点/布局拍平/路由命名）——写菜单路由前必读（2026-10-12 新增） |
| `59-管理端技术栈保留与整合策略.md` | **预置技术栈全保留**（防误删）+ 整合新栈三步——删依赖/组件/页面前必读（2026-10-12 定稿） |
| `60-技术栈评估与演进路线.md` | **技术栈演进与预留项真源**：Seata 不引入（触发条件：财务强一致→TCC/Saga）、Flowable 预留、新徽章全预留、P0/P1/P2 路线、工程优化点——技术选型前必读（2026-10-12 定稿） |
| `00-索引.md` | 需求文档总入口 |

## 4. 七条硬事实（违反即失败，完整版见《红线》第 7 条）
1. **所有请求走网关** `http://<host>:8760/api`+端前缀；**直连 pd-web-* 8161~8164 被 `TokenAuthInterceptor` 401 拒绝**。
2. **JWT 头字段是 `token`**（非 `Authorization`），有效期 7200s，**无 refreshToken** → 401 清 token 重登（**2026-10-10 更新**：新 `pd-admin-ui-vue3` 已统一 token 头、无 refreshToken，旧 Vue2 OAuth2 遗留已随 `pd-admin-ui` 删除消失，EC-09 闭合）。
3. **轨迹查询只认 `businessId`+`type`**（无 `orderId`）；前端不得伪造参数。
4. **新增对外接口仍须注册 `pd_auth.pd_auth_resource` 并绑角色**（RBAC/菜单基线，D-42 菜单下发依赖；无自动注册，须手工插表）。**2026-10-10 实测更新**：新 `pd-gateway`（Sa-Token）只校验登录态 + Same-Token 防直连，旧 AccessFilter"未知请求直接拒绝"机制已随 pd-authority 删除——未注册接口不再被网关拦截，但菜单/权限不可见，注册绑定仍是强制纪律。
5. **网关鉴权（2026-10-10 更新）**：新 `pd-gateway`（Sa-Token+Redis）登录校验 + Same-Token 防直连；旧"j2cache 修复/菜单空清 redis"仅作历史。**菜单动态下发（D-42）尚未实现**（pd-auth 无 menu/router 端点），菜单空先确认功能未建、勿当缓存问题排查。
6. **严禁新建/重写状态机**：`StateTransitionValidator` 已在用；真实缺口是跨端同步桥。
7. **环境**：Docker 网 `172.21.0.x`；宿主 `192.168.20.130`；Nacos `8848`（ns `1cb93ce4…`，group `pinda-tms`）。**Nacos 鉴权 2026-10-11 起已开启**（无 token 访问 403）：dev 统一账号 `nacos/123456`（13 微服务+CI 全用它；yml 默认 `${NACOS_USERNAME:nacos}/${NACOS_PASSWORD:123456}`，compose 由 .env 强制注入）；`pinda/123456` 仅命名空间读写，保留备用。**密码勿入库**（🔴 2026-10-11 口径细化见 §5.2：dev 中间件统一口令 `123456` 允许写入 yml/compose 默认值；"勿入库"现仅约束真实第三方凭证）。

> ⚠️ 历史机制已删（2026-10-10）：旧网关 `AccessFilter` 资源清单关卡（`if (resourceNeed2Auth != null)` 整段放行）随 pd-authority 删除不复存在；新网关鉴权口径见硬事实 4——"返回 200"= 已过登录校验，权限粒度由资源/菜单绑定与下游校验承担。

## 4.1 架构口径变更（2026-10-10，用户拍板）
- **旧 `pd-authority` 模块已废弃删除**（因过于复杂），**禁止再基于它开发或引用**；认证由**新建 `pd-auth`**（Sa-Token + 多租户 + 验证码，端口 9000）承担，网关为**新建 `pd-gateway`**（Spring Cloud Gateway）。
- **`codeback/` 是重构前的旧代码备份**（已 gitignore、未跟踪、不参与编译）——**定位为领域参考对象与旧知识对照物**（旧项目本就跑不起来，重构 = 参考旧的重新设计开发，非行为等价迁移）；**严禁直接复制旧代码进业务模块**；旧代码独占知识以《52-旧代码独占知识固化》为准。
- **全量重构已立项（定案 `D-61`，2026-10-10，同日二次修订）**：重构任务内解除"禁止重构"红线（SRS §1.1 第4条、§1.3 第10条）；本次 = **以 codeback/ 为领域参考、按《08》契约 / 《07》编码 / 《27》目标断言，全面重新设计与开发业务的 Java/中间件/数据库**（允许重设计表结构与内部分层，走 create-migration；禁止夹带未拍板的业务口径变更）；执行顺序与 DoD 见《53-全量重构总体规划》v1.1；专项外红线照常生效。

## 5. 关键纪律（详见《红线》）
- **🔴 代码必须走 Git 仓库，禁止 scp / 直改服务器文件热部署（2026-10-13 用户定，违反即返工）**：任何代码变更只允许一条链路——**本地改 → 本地编译/自测 → `git diff` 给用户确认 → commit（中文 `type: 简述`）→ push 双远端（Gitea+Gitee）→ 服务器 `/data/pinda-tms` 内 `git pull` → ci 容器构建 → compose 滚动部署**。**严禁** scp 上传源码、严禁直接 ssh 编辑服务器工作区文件后构建：那会使服务器工作区变脏、与仓库分叉、被下次 `git pull` 冲掉或覆盖并行会话改动，且无审计留痕。仅当仓库链路不可用且经用户明确同意时才可临时直改，事后必须立即回填仓库并在《99》台账登记。dev 库 `UPDATE/INSERT` 数据修正不属代码，但执行前同样先报完整语句与 `WHERE` 条件清单、经确认再执行，并同步修正 `docs/sql/` 种子脚本防复发。
- **多会话并行开发（2026-10-10 起）**：开工前先读 `docs/需求文档/99-任务认领台账.md`，**只改自己已认领的模块/文件**；台账中"进行中"的条目禁止其他会话动手，收工及时销账。
- 先读后写；复用既有 API（后端能不改则不改）；禁止过度设计（YAGNI）。
- 禁止硬编码（cron/阈值/地图 Key/状态数字）；前端所有请求过 `src/utils/request` 拦截器。
- **严禁跨模块写业务**：订单→pd-oms、调度→pd-dispatch、作业→pd-work、基础→pd-base、轨迹→pd-netty。
- 仅 Vue3 专项内可改 `pd-admin-ui`；第三方库替换须经专项规范 §6 决策，禁"顺手"引第二个同类库 / 新中间件。

---

## 5.1 近期变更记录（2026-10-10 · 豆包助手执行，供参考勿回退）

> 本节约 2026-10-10 由豆包助手（非 Claude Code）完成的前端工作，均为**已部署生效**的线上变更；如与你认领的任务冲突，先读《99-任务认领台账》确认归属，勿擅自回退。

### A. 品达物流新 Logo 整套（提交 08c1b98/68165a3，已部署 4 端）
- 定稿：红色货车图形 + 车厢白字"品达物流"；组合标（横版）用于登录/导航，图形标（1:1）用于 favicon/头像/侧边栏，App 图标圆角浅灰底。
- 品牌资产库：`docs/brand/`（透明版/多尺寸/favicon/单色反白，4 端复用源）。
- 4 端落地位置：
  - 管理端 `pd-admin-ui-vue3`：`public/favicon.ico`、`src/assets/images/logo-mark.png`（图形标）、`logo-full.png`（组合标）、`logo.png`（旧组合标，登录页已不再引用）。
  - 三端 `pd-mobile/*/src/static/`：`logo.png`（图形标）、`logo-full.png`（组合标）、`logo-app.png`（App 图标）、`favicon.ico`；登录页 `<image src="/static/logo-full.png">`；`manifest.json` 已配 android 5 档 + ios 4 档图标。

### B. 管理端登录页与国际化（提交 cd47b17 / e9e2d12 / 5d78f63，已部署）
- 登录卡片品牌区：46px 图形标 + "品达物流" + "FASTER AND SAFER"（原 44×44 方框塞横版组合标导致变形不可见，已修复）。
- 侧边栏 `LayoutLogo.vue`：20px 图形标（原同样变形）。
- 工具栏 `ThemeSwitcher` / `LangSelect`：**点击直接切换**（主题 亮→暗→自动 循环；语言 中↔英 toggle），去掉了下拉菜单——**全站生效**（LayoutToolbar 同款按钮行为一致）。
- 登录页文案**全部接入 i18n**（`src/lang/package/zh-cn.json` / `en.json` 新增 `login.title/desc/*Placeholder/slogan/copyright/footer/accounts` key）；品牌名与 FASTER AND SAFER 不翻译。
- 左侧插画区不再叠加品牌条（`login-brand__header` 已移除，保持插画+书法标语）。
- **管理端菜单样式（追加，提交 `9421bea` 已部署，2026-10-10）**：侧边栏子菜单标题 `.el-sub-menu.has-active-child > .el-sub-menu__title` **取消淡蓝背景块+主色文字**（原 `--el-color-primary-light-9` 背景），统一为普通文字色（`--menu-text`）+ 透明背景，仅保留 hover 反馈；选中态只由具体菜单项 `.el-menu-item.is-active` 表达（浅色/dark/sidebar-color-blue/mix 布局同一规则）。修改文件：`pd-admin-ui-vue3/src/styles/layout.scss`（三处规则块）。与顶栏菜单（无背景、仅文字色+底部 2px 主色指示条）视觉统一。
- **管理端菜单最终定案：平级铺开，不做前端归组（提交 `debcc44` 已部署，2026-10-11，推翻当日早些时候的分组方案）**：
  - **演进历史（避免走回头路）**：曾按联云科技 TMS 做过前端归组（`groupFlatMenus()` + `MENU_GROUP_MAP` 把平级菜单归成"工作台/订单中心/调度中心/…"目录树，提交 `1298862`/`680d2fd`）。**用户最终拍板撤销**——`el-sub-menu` 会渲染分组标题层 `el-sub-menu__title`，占纵向空间且视觉冗余。
  - **现行策略**：`permission.ts` 的 `generateRoutes()` 直接 `buildRoutes(routeData)`，**菜单按后端 sys_menu 下发结构原样渲染**，前端不做任何归组、不生成 `el-sub-menu` 分组标题。`groupFlatMenus`/`findGroupByPath`/`MENU_GROUP_MAP` 已整块删除（勿恢复）。**将来若要业务分组，正解是在后端 sys_menu 配目录层级（children 树），而不是前端硬编码映射。**
  - **⚠️ 根布局壳必须拍平（提交 `d854feb` 已部署，2026-10-11，logo 下方空白/折叠态整组弹层的真正根因）**：后端动态菜单 `/api/v1/menus/routes` 下发的是**一个 `path:"/"`、无 `meta.title` 的根 Layout，children 里挂全部业务菜单**。该壳只承担路由层 Layout（sidebar+navbar+content），**绝不能在侧边栏渲染成 `el-sub-menu`**——否则展开态多出一个 40px 空 `el-sub-menu__title` 行（即 logo 下方那块空白），折叠态还会把全部菜单收进一个多余 popper。已在 `src/layouts/composables/useLayout.ts` 的 `flattenSidebarRoutes()` 处理：侧边栏/顶栏取数（`routes` computed）时，把「无标题 + 有可见 children」的纯容器拍平，children 提升为同级并用 `path.resolve(base, child.path)` 补全绝对路径；**带标题的真实目录分组原样保留**（可正常折叠）。此函数只影响菜单渲染数据，不动路由注册。**勿删该拍平逻辑；改侧边栏取数务必继续走 `flattenSidebarRoutes(permissionStore.routes)`。**
  - **前端发布缓存（已正确，勿回退）**：`deploy/apps/nginx/admin-h5.conf` 对 `/`、`/index.html`、SPA fallback 均返回 `Cache-Control: no-cache,no-store`，带 hash 的 js/css 才 immutable 1 年。部署后页面仍旧基本是浏览器标签未硬刷新（Ctrl+Shift+R）或时序问题，不是 nginx 配置问题。
  - **⚠️ 大模型开发菜单时必须遵守的规则（以此为准）**：
    1. **禁止在前端写菜单分组映射/归组函数**；菜单层级只由后端 sys_menu 的 children 决定
    2. **图标为空时自动 fallback**：`LayoutMenuIcon.vue` 已加空值保护，icon 为空渲染 Element Plus `Document` 默认图标，不显示黑色方块
    3. **菜单结构**：叶子项只有一层 `el-menu-item`，禁止 AppLink/router-link 包裹（避免 a>li 双层留白），`@click` 直接 `router.push`（外链 `window.open`）
    4. **视觉（`src/styles/layout.scss`）**：菜单项高 40px、图标 18px、文字 14px；`.el-menu` 与 `.el-scrollbar__view` 顶部 `padding:0`（消除 logo 与首项之间空白）；选中项左侧 3px 主色竖条
    5. 三套主题（浅色白/经典蓝/dark）自动适配；想要联云蓝黑风在设置切 `sidebarColorScheme: classic-blue`
    6. **动态链路断点（2026-10-12 实测，完整规范见《58》）**：守卫 `isRouteGenerated` 初值 true（动态路由未启用）、后端无 `/api/v1/menus/**`、`sys_menu` 为旧表结构、mock routes 是模板演示菜单——**禁止单独打开动态路由开关**（4 前提缺一不可，见《58》§1.1）；菜单管理页增删改暂不生效属预期，禁止绕道硬编码"实现"。
    7. **布局取数**：Mix/Top/Double 顶部菜单必须走 `useLayout.ts` 的 `flattenSidebarRoutes`，禁止直接 filter 原始树（无标题根壳会产生空白顶级项）；`setMixLayoutSideMenus` 只认顶级路由，根壳场景先拍平再查找。
    8. **路由 name 唯一**：动态子路由 name 禁止与静态同名（User/Role/Dict/Log/…）或相互重复；新增页面用 routeName 派生（如 SystemUser），否则 addRoute 告警、按 name 导航失效。

### C. 公开仓库卫生与部署方式（提交 6525774 / merge cc733b4，已同步 Gitea+Gitee）
- `.gitignore` 新增：`.claude/`、`.atomcode/`、`.creative-design/`、`.gitea/`、`mvn_build*`（AI 工具配置/内网 CI 配置不再入库）。
- `.gitea/workflows/*.yml` 已 `git rm --cached`（文件留在服务器本地）：**Gitea Actions 自动构建已停**——前端/后端 push 不再自动部署，统一手动（服务器 `bash /data/pinda-deploy/deploy-fe.sh <admin|customer|courier|driver>` 串行执行；后端如需部署另行手动构建）。
- 双远端 master 现状：Gitea + Gitee = `cc733b4`；**本地 master（9bb62e4 分叉线）尚未 merge**——你（Claude Code）继续提交前必须先 `git pull` / merge 远端，避免推送被拒与历史分叉。

### D. 前端工程规范待办（尚未执行，欢迎认领/推进，详见 `docs/前端工程优化与规范清单-2026-10-10.md`）
1. **移动三端去重**（P1）：customer/courier/driver 的 `src/common` 重复且结构不一致（customer .js 17 文件 vs courier .ts 9 文件）。推荐抽 `pd-mobile-shared` 共享包；轻量方案先统一结构 + pd-mobile 根级 lint。
2. **管理端 lint 落地**（P2）：`pnpm lint`（eslint/prettier/stylelint 已配）提交前跑通；`src/views/demo/` 模板示例页清理（需同步 router 引用）；~~`package.json` repository 指向品达仓库~~ **✅ 已改并推送（ac43192，`https://gitee.com/itxinfei/pinda-tms.git`）**。
3. **收尾**（P3）：三端 manifest `name/description` 已补、App 图标已配 9 档（见条目 K3，提交 84566a6），**仅剩 `appid` 未配置**（发布小程序前需真实 appid）；driver/courier pages.json tabBar 无 iconPath（App 端 tab 无图标，待设计 tab 图标）；根目录垃圾日志已清理（见 E）。

### E. 根目录垃圾文件清理（2026-10-10）
已删除：`mvn_build*.log`（10 个）、`hs_err_pid*.log`、`replay_pid*.log`、`spring-core-6.1.14.jar`（JVM 崩溃/构建临时产物，均已被 .gitignore 忽略、不入库）。

### F. 宝塔反代 /api 路由修复（2026-10-10，线上生效）
- **现象**：`http://192.168.20.130/#/login` 打不开（验证码/登录接口全失败）。
- **根因**：新前端打包后 `VITE_API_BASE=/api`（新 pd-gateway 真实前缀 /api），但宝塔反代 `pinda-fronts.conf` 只配了 `/prod-api/` → 8760；`/api/*` 被最长前缀匹配落入 `location /` → 前端容器 SPA fallback 返回 index.html，接口全收到 HTML。
- **修复**：`/www/server/panel/vhost/nginx/pinda-fronts.conf` 新增 `location /api/ { proxy_pass http://127.0.0.1:8760; }`（透传，网关认 /api 前缀；备份 `.bak-api-20261010`）；宝塔 init reload 未生效，改用 `kill -HUP <master_pid>` 强制重载。
- **验证**：`GET /api/auth/anno/captcha` → 200 PNG；`POST /api/auth/anno/login` → 200 JSON（业务响应）；`/prod-api/` 兼容路由保留。**前端 API 前缀统一用 `/api`，勿改回 `/prod-api`。**

### F2. 双重前缀 401 修复（2026-10-10，线上生效）
- **现象**：`/prod-api/api/auth/anno/captcha?...` 401（登录页报 `TypeError: n.t is not a function` 后挂掉）。
- **根因**：`.env.production` 的 `VITE_API_BASE=/prod-api`，而 api 文件写死绝对前缀 `/api/...`（如 `AUTH_BASE_URL="/api/auth/anno"`），axios 拼接成 `/prod-api/api/...` → 旧 rewrite 变成 `/api/api/...` → 网关不认 → 401。
- **修复**（服务器，无需重建前端）：`location /prod-api/` 内加两条兼容 rewrite：`rewrite ^/prod-api/api/(.*)$ /api/$1 break;` 优先（吃双重前缀），再保留 `rewrite ^/prod-api/(.*)$ /api/$1 break;`（单前缀）。备份 `.bak-dualprefix-20261010`。
- **验证**：`/prod-api/api/auth/anno/captcha`、`/prod-api/auth/anno/captcha`、`/api/auth/anno/captcha` 三种前缀均 200 PNG；登录 POST 200 JSON。
- **⚠️ 治本待办（前端，Claude Code 认领）**：api 文件前缀设计冲突——`VITE_API_BASE=/prod-api` 与 api 写死 `/api` 前缀重复。建议统一：改 `VITE_API_BASE=`（空）并让 api 保持 `/api/...` 绝对前缀（走反代 `/api/` 路由）；或 api 改相对路径配合 `/prod-api`。二选一，改后需重新构建部署 admin。

### G. 登录页白屏修复：缺 useI18n 定义（2026-10-10，已部署 manual-20261010185920）
- **现象**：接口全通后登录页仍报 `TypeError: n.t is not a function`（白屏）。
- **根因**：`src/views/login/index.vue`（gitea 线）模板/校验规则/演示账号共 10+ 处调用 `t("login.xxx")`，但 `<script setup>` **漏了 `import { useI18n }` 与 `const { t } = useI18n()`**（早期国际化提交 5d78f63 引入的遗漏）。
- **修复**（提交 `5aad093`，已推送双远端）：在 login/index.vue 补两行（与本地 Claude 版一致）：
  ```ts
  import { useI18n } from "vue-i18n";
  const { t } = useI18n();
  ```
- **验证**：服务器构建通过（新镜像 healthy），`/` 200、`/api/auth/anno/captcha` 200 PNG。**登录页文案全部走 i18n 语言包（zh-cn/en），写死中文视为缺陷。**

### H. 登录页占位符 {{ }} 修复 + favicon 缓存绕过（2026-10-10，已部署 manual-20261010191539）
- **占位符 bug**：`login/index.vue` 三处（username/password/captcha）写成 `placeholder="{{ t('login.xxxPlaceholder') }}"`——Vue3 属性绑定里 `{{ }}` 按字面量显示（验证码框空时肉眼可见）。修复：`placeholder="..."` → `:placeholder="t('...')"`（去 `{{ }}`、加 `:` 绑定）。**Vue3 属性绑定一律 `:prop="expr"`，禁止 `{{ }}`。**
- **favicon 诡异行为（已定位）**：宿主 `http://192.168.20.130/favicon.ico`（无 query）返回 **4286 字节模板默认 V 形**（md5 3c10464d），带 `?v=xxx` 返回容器货车版（3808，md5 a88f308a）。容器直连 8081 恒为货车版；宝塔 nginx 运行配置（`/www/server/nginx/sbin/nginx -T`，注意 PATH 里的 nginx 读不到宝塔配置）favicon regex 存在。**判定为宝塔 WAF/lua 层对无 query URI 的旧响应缓存**（早期模板部署时缓存），带 query 即绕开。
- **修复**：`pd-admin-ui-vue3/index.html` favicon 引用 `/favicon.ico` → **`/favicon.ico?v=20261010`**（版本号 query，同时绕过服务端缓存与浏览器缓存）。提交 `ad6b729` 推双远端。以后换 favicon 直接改 query 版本号。
- **验证**：新镜像 healthy；`/favicon.ico?v=20261010` → 200/3808/a88f308a（货车）；`/` 200、验证码 200 PNG。**用户硬刷新（Ctrl+Shift+R）后标签页 favicon 应为红色货车。**

### I. 四端登录全链路体检与修复（2026-10-10，已部署生效）
- **体检结论**：四端容器/页面/验证码接口全部在线；真实登录暴露 2 处阻塞，均已修复并验证通过。
- **I1 后端根因（admin 登录 500"系统繁忙"）**：旧 `pd-auth-server` 镜像（5d78f63）application.yml 数据源指向 **`pd_auth` 库（不存在）**；代码基线改为连 `pinda_tms` 后，登录又报 **`Table 'pinda_tms.pd_auth_tenant' doesn't exist`**（HEAD 代码实体表名仍是 `pd_auth_tenant/pd_auth_user`，本地工作区 Claude 正改 `sys_` 版未提交）。
  - **修复（服务器，不入代码）**：在 `pinda_tms` 库执行 `docs/sql/pd_auth_多租户重建_20261009.sql`（去掉 CREATE DATABASE/USE 的库内版）→ 建 `pd_auth_tenant/pd_auth_user/pd_core_org/pd_core_station/pd_area` + 种子（platform/pinda 租户、admin/pinda 账号，BCrypt=123456）；**补 pinda 租户 admin**（管理端固定 `tenantCode:pinda`）；补三端测试账号 `driver04/courier01/customer01`（同 BCrypt 123456）。
  - **auth 重建部署**：服务器 `mvn -f pd-auth/pom.xml -am package`（ci/pinda-ci 容器）→ `docker build -t pinda/pd-auth:<short-sha>` → **compose 必须带 `TAG=<short-sha>` 环境变量**（compose image 用 `${TAG:-latest}`，不带会去拉 latest 失败）→ `docker compose up -d pd-auth`（旧名 pd-auth-server）。当前线上 `0ac2b3a` healthy。
  - **验证**：验证码取 Redis 值（`pinda:captcha:<key>`，TTL 120s）后真实登录：admin → `{"code":0,"msg":"ok","data":{"token":...}}`。
- **I2 三端登录缺 tenantCode（400"企业编码不能为空"）**：`customer/driver/courier` 的 `login/index.vue` 提交体只有 `{account,password,key,code}`，后端 `LoginParamDTO` 必填 `tenantCode`（三端 `config.ts` 已有 `TENANT_CODE='pinda'` 未用）。
  - **修复（提交 `051ba07`，已推双远端）**：三端 `login/index.vue` 各补 `import { TENANT_CODE } from '@/common/config'`（customer 相对路径）+ 提交体加 `tenantCode: TENANT_CODE`。**移动三端登录必须带 tenantCode，与 admin 一致。**
  - **部署**：`bash /data/pinda-deploy/deploy-fe.sh driver|courier|customer`（当前 `manual-20261010200107/09/11` healthy）。
  - **验证**：driver04/courier01/customer01 + 验证码 → 三端均返回 token。
- **线上登录账号（dev，密码均 123456）**：管理端 `admin`（tenantCode=pinda）；三端 `driver04` / `courier01` / `customer01`；**统一演示账号 `demo`（三端通用，同密码）**。`member_info`、`base_truck_driver` 仍为空——三端登录能过但业务数据（寄件/运单）尚无种子。
- **数据库遗留（2026-10-12 已清理）**：面向旧分库（pd_auth/pd_base）的 7 个资源注册/角色授权类脚本已删除（`阻断C_三端接口注册与账号闭环`、`pd_auth_resource_注册模板_阻断C`、`三端异常上报接口_资源注册_20261008`、`客户订单轨迹_orderTrace_资源注册_20261009`、`管理端接口资源注册_20261007`、`资源注册路径修正_20261007`、`恢复_角色授权_20261007_2340`），旧 AccessFilter 资源表机制已随 pd-authority 移除；如需三端接口资源/角色授权，按 `pinda_tms` 单库 + 新表名另写。

### J. 品牌色切换：神领物流配色（2026-10-10，用户定案，已提交 8675d94 待部署）
- **决策**：移动端品牌主色由品达蓝 `#2B6CFF` 改为**神领物流橙红体系**（用户明确要求参考神领物流项目配色；神领仓库 gitee.com/giaqiqi/shenling-logistics-template-backup 源码统计：主色 `#E15536`、登录页深藏青 `#1A1C41`/`#283443`、浅橙 `#FFAB98`/`#FFEEEB`、辅助蓝 `#419EFF`、菜单激活 `#FF7A63`）。
- **代码落地（提交 `8675d94`，已推双远端 gitea+gitee）**：
  1. 三端 `common/styles/variables.scss`：`--c-primary:#E15536; --c-primary-dark:#B53D22; --c-primary-light:#FFEEEB;`（原 `#2B6CFF/#1A4FD6/#E8F0FF`）
  2. 三端 `uni.scss`：`$u-primary:#E15536`
  3. 三端 `pages.json`：tabBar `selectedColor` + `navigationBarBackgroundColor` → `#E15536`
  4. 三端全部页面（home/profile/mailing/address/agency/track 等）品牌蓝硬编码 → `#E15536` 系（批量替换，`grep 2B6CFF` 三端 src 零残留）
  5. 三端登录页 `login-bg.png` 替换为神领橙系插画（深藏青底+橙红货车/快递员），`login/index.vue` hero 用背景图 cover + 卡片负边距上浮布局；customer 另含 token 取值兼容修复（一层/两层）。
- **部署**：三端 `deploy-fe.sh driver|courier|customer` 已触发（先配 `git config --global --add safe.directory /data/pinda-tms` 解决 dubious ownership；pdwl 已配 NOPASSWD sudo）。
- **文档**：`13-UI设计规范与页面模板.md` §2.1 Token 表+代码块已改为神领橙；`03-全局定案` D-10 已注改案；`14-管理端UI规范` 加注（管理端 `#2B6CFF` 暂保留，待统一）。**md 文档不上 Gitee**。
- **遗留**：管理端（pd-admin-ui-vue3）品牌色未换，待用户决策统一。

### K. 三端登录页整体重做：广州联云布局 × 神领配色（2026-10-10，用户定案，提交 8087068 已部署）
- **决策**：用户要求三端（driver/courier/customer）登录页美化并"模仿广联的产品"——**参考对象澄清为广州联云（https://www.gzlcloud.com/）的三端**（首版误按广联达 glodon PC 风格重写 41f5aec，用户纠正后改为联云式并重提交）。
- **联云风格要点**（抓官网 + App Store 截图归纳）：物流 TMS SaaS 同行；App 浅色清爽风——**顶部渐变头部**（个人中心为渐变蓝紫头+白底列表）、白色表单、**橙色强调按钮**（订单详情"添加评论"橙按钮）、司机端 tabBar=委托/接单/执行/历史/我的。
- **落地（提交 `8087068`，已推双远端 gitea+gitee，三端已部署）**：三端 `login/index.vue` 重做为**联云式结构 × 神领配色**：
  1. 页面背景 `#f5f6f8` 浅灰白（联云浅色风）；
  2. 顶部品牌头 `login-hero`：神领深藏青渐变 `#1A1C41→#283443→#2D3A4B` + 底部 48rpx 圆角，内居中 `logo-full.png`（240rpx）+ "品达物流"44rpx + 各端 slogan；神领橙系插画 `login-bg.png` 移入 hero 作底部装饰（opacity .5，深藏青底与渐变融合，不再全屏 cover）；
  3. 白底表单卡 `login-card` 负边距上浮覆盖渐变头（-80rpx）、圆角 32rpx、轻阴影；输入框灰底 `#f5f6f8` 圆角 field（customer 用 u-input / driver·courier 用 u--input，保持各端既有组件与 script 逻辑、token 兼容写法）；
  4. 登录按钮神领橙渐变 `#E15536→#F1784F` 96rpx 大圆角 + loading 态（"登录中..."）；
  5. footer 版本文字改深灰（`--c-text-4`）。
- **部署**：三端 `deploy-fe.sh driver|courier|customer` 已触发（手动部署，从 Gitea 拉 8087068 构建）。
- **布局定案（追加，提交 `2980069` 已部署）**：用户对联云式布局仍不满意（"说不出哪里不好"），要求参考互联网案例。调研货拉拉司机端/斑马快跑/运满满司机端登录页后给三方案，**用户选定方案一「货拉拉式极简白底」**：纯白背景 + 顶部仅居中小 logo（logo-full.png 200rpx，去掉渐变头/插画/slogan 堆叠）+ 灰底描边圆角输入框（带 account/lock 图标）+ 橙色实心圆角按钮（#E15536，非渐变非全圆）+ 底部"登录即代表同意《用户协议》和《隐私政策》"+ 版本文字。`login-bg.png` 插画不再引用（文件保留于 static 供其他页面使用）。
- **布局定案 2（追加，提交 `6cbdaf2` 已部署）**：用户要求"把我生成的插画放到登录页面当背景 + 表单美化 + logo 放大 + 加端名"。最终版（当前线上）：`login-bg.png`（神领橙系插画，三端各自图）**全屏 aspectFill 背景**（z-index 0，页面底色 #1a1c41）；品牌区 z-index 1 居中放大 logo-full.png **280rpx** + 其下"品达物流 · 司机端/快递员端/客户端"白字（30rpx/600/阴影）；白色圆角表单卡（rgba(255,255,255,.97)、圆角 32rpx、阴影 0 24rpx 60rpx）浮于插画上：卡内"欢迎回来"+ 副题 + 三个**等高输入框**（field 92rpx、灰底 #f7f8fa、描边 #e7e9ee、圆角 20rpx、图标、u--input height=88rpx）+ 验证码图 200x72 + 橙色按钮 92rpx/圆角 24rpx（:active 0.85）+ 协议小字；footer 白字带阴影。
- **文档**：本条目即记录；后续登录页再调整以本条为准。
- **布局定案 3（追加，提交 `84566a6`，已推双远端并部署 202610102338+，三端 healthy/200）**：
  1. **登录页 logo 放大 3 倍**：`login-logo` width 280rpx → **840rpx** + `max-width:90vw`（防溢出），brand `padding-top` 130→96rpx；
  2. **customer 修复**（`pd-mobile/customer/src`）：App.vue 补登录守卫（无 `pd_token` 即 reLaunch 登录页）；**token key 全端统一 `pd_token`**（stores/user.js、common/request/index.js、App.vue，原 'token' 键作废，改后旧会话需重新登录）；home tab 切页 `navigateTo`→**`reLaunch`**（防页面栈叠加；list/profile 页原已正确）；首页入口图标汉字→`u-icon`（box/search/map/location）；`request/index.js` 实现 `options.loading`（showLoading/hideLoading）；
  3. **删除 customer 死代码**（零引用已确认）：`common/request/index.ts`、`common/store/user.ts`（目录已删）、`common/utils/coord.ts`、`common/api/customer.ts`；
  4. **driver/courier API 传参统一**：手拼 query（`?id=`）改为 params 对象（cargoDetail/cargoOrders/carInfo/traceLatest/traceReplay/areaSimple；courier detail/route 同）；
  5. **manifest**：三端补 `name`/`description`；App 图标 9 档（android 48/72/96/144/192 + ios 76/120/167/1024 → `static/logo-<size>.png`，manifest 已引用，`logo-app.png` 保留为源图）；
  6. **遗留**：driver/courier tabBar 无 iconPath；customer 无原生 tabBar（自定义 u-tabbar+reLaunch 方案已定案）；appid 待填。

## 5.2 当前阶段工作定调（🔴 2026-10-11 用户拍板，当前最高优先级）

> 权威全文：SRS《06》§1.6；执行摘要《12》§0/§4。项目处于内网 dev、半成品、无真实用户、不上线；开发阶段一切工作围绕管理端可见可用展开。

1. **唯一主战场 = 管理端 `pd-admin-ui-vue3`**：按《55》《14》《16》把功能做全做完、页面设计好；**DoD = 写完 → 提交 → `bash /data/pinda-deploy/deploy-fe.sh admin` 部署 → 192.168.20.130 浏览器可见可用**。后端任务（含本仓库各服务）以"打通管理端所需接口/数据"为优先级依据，不为架构完美阻塞页面。
2. **dev 不做生产级安全功课**：中间件 dev 口令统一 `123456`（MySQL/Redis/RabbitMQ/Nacos），`${ENV:123456}` 默认值可直接写 yml/compose；不要求密钥外置/0600 env/轮换，不主动加 dev 安全层（Same-Token 深化、入站加固、口令复杂度、预配回调白名单等）。**已落地的 Sa-Token/网关/多租户机制保留运行、不深化不拆除**（同时承担功能路由与数据隔离）。
3. **底线两条**：① 真实第三方凭证（地图 AK、微信/支付宝商户密钥、短信密钥）不入库，走环境变量/部署注入；② 真实用户/公网上线前另立生产加固清单统一收紧。
4. 本条与其他文档/记忆中的旧安全口径冲突时，dev 阶段以本条为准；不构成上线后的永久豁免。
5. **决策边界：用户定方向，实现层不请示**（权威全文 SRS《06》§1.6-5、《12》§0-4）。
   - **问**：业务口径变更、新依赖/新中间件、表结构重设计、跨 3+ 文件架构改动、UI 方向取舍、生产/不可逆操作、红线清单配置文件。
   - **不问直接做**：① 符合《阿里巴巴 Java 开发手册》的常规实现；② 符合市场通行规范的选择（RESTful、HTTP 状态码、分页惯例、BCrypt、标准组件用法等），不给选项让用户挑；③ 符合国家/行业强制规范的合规细节；④ 简单 bug、编译错误、判空边界、文档已明确的内容。历史"拍板/待确认"记录是留痕不是待办，照做不提请。拿不准时默认按实现做，交付说明里注明自行决策点。

---

*本文件为入口。任何与 `docs/需求文档/` 权威文档冲突的"优化"默认无效，须先修订文档并经人工确认。*

<!-- open-wot agent instructions start -->
## Wot UI Agent Instructions

Before generating or modifying wot-ui component code, read the project Skill at `.claude/skills/wot-ui-v2/SKILL.md` and query the configured `wot-ui` MCP server for version-accurate APIs and examples.

<!-- open-wot agent instructions end -->
