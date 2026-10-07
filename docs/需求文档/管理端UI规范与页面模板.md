# 品达物流 TMS — 管理端 UI 规范与页面模板（Vue2.6 + Element UI + ECharts + 百度地图）

> **目的**：补齐管理端缺 Element UI 版设计 Token 与页面模板的缺口——管理端缺 Element UI 版设计 Token 与页面模板（既有《UI设计规范与页面模板.md》仅覆盖移动端 uni-app）。本规范约束 Claude Code 写 `pd-admin-ui`（Vue 2.6.10 + Element UI 2.12 + ECharts 4.2）业务页时的样式与结构。
> **强制力**：等同《开发规范与需求规格说明书》§1 红线 + 移动端 UI 规范 §8。所有管理端业务页**必须**遵守；自定义 HTML/CSS 绕过 Element 组件视为不合格。
> **配套**：《管理端业务页面开发任务卡.md》（页面功能/接口/验收）。
> **生成日期**：2026-10-07（复审补齐，索引第 34 条）。

---

## 第 1 章 设计原则（最高优先级）

1. **只用 Element UI 组件，不写裸样式**：布局用 `el-container`/`el-row`/`el-col`，列表 `el-table`，表单 `el-form`，弹窗 `el-dialog`，下拉 `el-select`，标签 `el-tag`，分页 `el-pagination`。**禁止**手写 `<div>`+`<style>` 替代组件做布局/按钮/卡片。
2. **设计 Token 全集中**：颜色/间距/字号/圆角/阴影只允许引用 `src/styles/tokens.scss`（§2）或 Element 主题变量，**禁止页面内硬编码 hex / px**（图表色与地图浮层除外，见 §5/§6）。
3. **品牌主色全端统一（✅ 2026-10-07 定案）**：主色 **`#2B6CFF`**（与移动端 `--c-primary` 同一品牌色）。**废弃**现状三处冲突色：Element 默认 `#1890ff`、`#409EFF`、`#E05635`（实测：dashboard 30 处 / user/Index 19 处 / auth/role 14 处硬编码冲突色，主色定案已废弃）。落地方式：`element-variables.scss` 覆写 `$--color-primary:#2B6CFF`，全局替换硬编码色为变量。
4. **结构走模板**：每类页面套用 §4 标准骨架，不允许自由布局。
5. **三态必做**：列表/查询页必须有 加载中 / 空 / 错误 三态（`v-loading` / `el-empty` / 重试按钮）。
6. **图表与地图遵循专项规范**（§5/§6）：图表色板取自 Token，地图浮层/聚合按统一交互。

---

## 第 2 章 设计 Token（唯一真源）

> 在 `src/styles/tokens.scss` 定义 SCSS 变量 + CSS 变量（双轨），并在 `element-variables.scss` 覆写 Element 主题。

### 2.1 颜色（语义化，禁止其他色值）

| Token | 值 | 用途 | 对应移动端 |
|---|---|---|---|
| `--c-primary` | `#2B6CFF` | **品牌主色**（主按钮、选中、主链接、Element primary） | `--c-primary` 同值 |
| `--c-primary-dark` | `#1A4FD6` | 主色 hover/按压 | 同 |
| `--c-primary-light` | `#E8F0FF` | 主色浅底（选中行/标签底） | 同 |
| `--c-success` | `#00B42A` | 完成/正常/成功（`el-tag type=success`） | 同 |
| `--c-warning` | `#FF7D00` | 待处理/预警/进行中 | 同 |
| `--c-error` | `#F53F3F` | 异常/失败/驳回 | 同 |
| `--c-text-1` | `#1D2129` | 主标题/正文重点 | 同 |
| `--c-text-2` | `#4E5969` | 正文 | 同 |
| `--c-text-3` | `#86909C` | 辅助说明 | 同 |
| `--c-text-4` | `#C9CDD4` | 占位/禁用 | 同 |
| `--c-bg` | `#F5F6F8` | 页面背景 | 同 |
| `--c-surface` | `#FFFFFF` | 卡片/弹层背景 | 同 |
| `--c-border` | `#E5E6EB` | 分割线/边框 | 同 |

> 业务状态色映射（全端统一，禁止自创）：**待处理=warning / 进行中=primary / 已完成=success / 异常=error**（`el-tag` 对应 type，禁止手写色值）。

### 2.2 间距 / 字号 / 圆角 / 阴影

| 类 | 值（对齐移动端精神，px 管理端） |
|---|---|
| 间距 | `--s-1:4px` `--s-2:8px` `--s-3:12px` `--s-4:16px` `--s-5:20px` `--s-6:24px` `--s-8:32px`；页面内边距 `--s-4`，卡片间距 `--s-3`，区块间距 `--s-5` |
| 字号 | `--f-title:18px`(页标题) `--f-sub:16px`(卡片标题) `--f-body:14px`(正文) `--f-aux:13px`(辅助) `--f-tip:12px`(说明)；正文不得低于 12px |
| 圆角 | `--r-sm:4px` `--r-md:8px` `--r-lg:12px` `--r-round:999px` |
| 阴影 | `--shadow-card:0 2px 12px rgba(0,0,0,.04)`（卡片用，禁重阴影）；表格弹层用 Element 默认 |

### 2.3 图表色板（ECharts，§5 详规）

`--chart-1:#2B6CFF --chart-2:#00B42A --chart-3:#FF7D00 --chart-4:#F53F3F --chart-5:#8E44AD --chart-6:#13C2C2 --chart-7:#FFC53D --chart-8:#86909C`

---

## 第 3 章 全局结构规范

### 3.1 页面骨架（统一容器）

```
<template>
  <div class="page">                     <!-- 背景 --c-bg，内边距 --s-4 -->
    <div class="page-header">            <!-- 标题 + 主操作按钮(右) -->
      <span class="page-title">订单管理</span>
      <div><el-button type="primary">新建</el-button></div>
    </div>
    <el-card class="filter-card">…</el-card>   <!-- 筛选区 ≤3 行 -->
    <el-card class="table-card">…</el-card>    <!-- 表格 + 分页 -->
    <el-dialog>…</el-dialog>                    <!-- 弹窗表单 -->
  </div>
</template>
```

- 路由由后端 `getRouter` 下发（`filterAsyncRouter` 映射 `@/views/${path}.vue`），新增页面必须注册 `pd_auth_resource` 并绑定菜单角色，否则网关拒绝（阻断 C）。
- 请求统一走 `src/utils/request`（Axios，`baseURL=/api`，自动注入 `token` 头，`code!==0` 为失败，401 清 token 跳登录）。**禁止裸 `fetch`**。

### 3.2 统一反馈（禁止自造）

| 场景 | 组件 | 说明 |
|---|---|---|
| 整页加载 | `v-loading` + `element-loading-text="加载中…"` | 列表/详情首次加载 |
| 空态 | `<el-empty description="暂无数据">` | 数据为空 |
| 错态 | `<el-empty>` + 重试按钮（调同一请求） | 请求失败 |
| 成功/失败提示 | `this.$message`（success/error） | 提交/操作反馈 |
| 确认弹窗 | `this.$confirm` | 删除/取消/改派等危险操作 |
| 分页 | `el-pagination`（`page`+`pagesize` 小写，`total` 后端返回） | 统一布局 `total` + `pager` + `sizes` + `jumper` |

### 3.3 列表页交互约定

- 筛选区（`el-card`）：≤3 行；状态筛选用 `el-select`（数据源走 `CommonController common/*/simple`，**禁止硬编码数字**，见《数据字典与枚举口径》红线）；时间用 `el-date-picker`（datetimerange）；查询/重置按钮。
- 表格：行高默认；列顺序按任务卡"字段/操作清单"；操作列固定右侧（`fixed="right"`）；超长文本 `show-overflow-tooltip`。
- 批量操作：`el-table` `@selection-change` 记录选中，批量按钮仅在选中数>0 时可用（主操作唯一）。

---

## 第 4 章 标准页面模板（必须套用）

### 4.1 列表页（订单/运单/车辆/司机/告警/结算…）

```
[page-header: 标题 + 主操作(新建/导入/导出)]
[filter-card: 筛选表单 ≤3 行(状态 select/关键字 input/时间 range) + 查询/重置]
[table-card: el-table(列按任务卡清单) + el-pagination]
[dialog: 新建/编辑表单]
```

### 4.2 详情页（订单/运单/任务详情）

```
[page-header: 返回 + 标题(单号)]
[状态条: 大号 el-tag(状态色映射) + 关键时间]
[el-card 分段: 基本信息(收/发件人、金额、付款方式) + 货物明细(可展开) ]
[el-card: 状态时间轴(el-steps/自绘 timeline，数据调状态机，禁硬编码)]
[操作区: 主操作(取消/改单/打印) 按状态显隐]
```

### 4.3 表单页（弹窗或整页）

```
[el-form label-width="100px" :rules="rules" ref="form"]
  [el-form-item ×N: 标签左 + el-input/el-select/el-date-picker/el-upload]
[底部: 提交(主) + 取消(次)]   // 校验失败定位首个错误项
```

### 4.4 看板页（运输任务 5 状态列 / 成本油耗 / 利润钻取）

```
[page-header: 标题 + 时间筛选]
[el-row gutter: 指标卡 el-card(数字 + 环比 + 迷你趋势) 一行 4 个]
[主体: el-row 左=看板列(拖拽)/图 右=明细表]
[拖拽列看板: 5 列(待执行/进行中/待确认/已完成/已取消)，卡片拖拽只触发查询/跳转，状态变更走后端状态机]
```

### 4.5 地图页（在途监控 / 调度一张图）

```
[百度地图 GL 全屏或主区(>60% 宽)]
  [右上角竖排操作: 定位/刷新/图层切换(车辆/订单/轨迹)]
  [底部浮层: el-card 当前选中点信息(车牌/状态/距离/最新时间)]
  [轨迹回放: 顶部进度条(播放/暂停/重置)]
[右抽屉: 选中任务详情(表格)]
```

### 4.6 大屏页（dashboard / 控制塔驾驶舱）

```
[深色背景(沿用现有 dashboard 风格，色板取 §2.3)]
[指标卡行: 业务量/毛利/异常数/成本占比(数字必须接后端 api/Dashboard.js，禁硬编码静态值)]
[图表区: ECharts(地图散点/柱状/饼图按 §5)]
[底部: 刷新时间 + 数据来源标注]
```

---

## 第 5 章 图表规范（ECharts 4.2）

1. **色板**：只允许 §2.3 八色，`color:['#2B6CFF','#00B42A','#FF7D00','#F53F3F','#8E44AD','#13C2C2','#FFC53D','#86909C']`，禁止自定义色。
2. **轴与网格**：`textStyle.color: var(--c-text-2)`；`splitLine` 用 `#E5E6EB`；`axisLabel` 字号 12px；`grid` 留出图例空间。
3. **图例**：`legend` 必配（顶部，`type:'scroll'`）；单系列可省。
4. **空态/加载**：图表容器数据为空显示 `<el-empty>` 覆盖层（不渲染空白 canvas）；加载用 `v-loading`。
5. **Tooltip**：`trigger:'axis'`（多系列）/`item`（单系列），`valueFormatter` 统一单位（元/趟/件/公里）。
6. **数据口径**：图表数值一律来自接口聚合（dashboard 现状硬编码静态值属违规，P0 补接 `api/Dashboard.js`）；单位/口径与《数据字典与枚举口径》一致。
7. **禁止**：引入第二个图表库（Chart.js/Highcharts）；echarts 实例不销毁（`beforeDestroy` 必须 `dispose`）。

---

## 第 6 章 地图规范（百度地图 JS API GL，✅ 已锁定 2026-10-06）

1. **库**：`vue-baidu-map-3x` 或直引 GL JS 脚本；**不引第二地图库**（AMap/Leaflet）。`location.ak` 为占位值，上线前替换真实 key（开发期测试 key / mock 数据先行）。
2. **坐标**：后端轨迹即 **BD-09**（`pd-oms BaiduMapUtils`），前端**零转换**；`coordSystem` 字段就绪前按 BD-09 处理（R2-1 改造后切换）。移动端小程序侧腾讯地图（GCJ-02）不受影响，转换走 `common/utils/coord.ts`。
3. **浮层交互**：地图浮层统一 `el-card`（白底、圆角 `--r-md`、阴影 `--shadow-card`）；点选标记 → 浮层展示（车牌/状态/最新时间）；浮层可关闭。
4. **聚合**：车辆/订单点多于 50 用 `BMapGL.MarkerClusterer` 聚合；点击聚合下钻一级。
5. **轨迹回放**：`polyline`（主色 `#2B6CFF` 系）+ 播放进度条；起点/终点 Marker 区分（绿=起点，红=终点）；轨迹点串来自 `GET /api/netty-service/trace/replay?businessId=&type=`。
6. **禁**：无底图时画 ECharts 折线冒充地图（现有 `pinda/trace/index` 属待整改）；坐标写死/手填。

---

## 第 7 章 页面编写禁止清单（AI 易犯，一律回退）

- ❌ 手写 `<div>`/`<span>` 替代 `el-*` 组件做布局/按钮/卡片。
- ❌ 页面 `<style>` 写死 hex / px / 魔法数字 / `!important`（图表色、地图浮层除外）。
- ❌ 引入第二个 UI 库、第二个图表库、第二个地图库、或 Tailwind/UnoCSS 原子化框架。
- ❌ 硬编码品牌色（`#1890ff`/`#409EFF`/`#E05635` 均已废弃，统一 `#2B6CFF`）。
- ❌ 同一状态不同颜色（必须按 §2.1 状态色映射）。
- ❌ 缺加载中/空/错误三态之一。
- ❌ 裸 `fetch` 直连接口（必须走 `src/utils/request` 拦截器）。
- ❌ 分页参数混用 `pageSize`/`page`（管理端统一 `page`+`pagesize` 小写）。
- ❌ 状态筛选硬编码数字（必须走 `common/*/simple` 枚举源）。
- ❌ 列表无分页/无刷新；表单无校验、提交无 loading/无反馈。
- ❌ dashboard 大屏硬编码静态数字（必须接后端）。
- ❌ 新增路由不注册 `pd_auth_resource`（网关必拒，阻断 C）。
- ❌ 删除/取消/改派等危险操作不经 `$confirm` 确认。

---

## 第 8 章 验收（管理端 UI DoD）

1. 所有颜色/间距/字号取自 §2 Token 或 Element 主题变量，源码无硬编码 hex/魔法数字（图表/地图浮层除外，色板合规）。
2. 全部使用 Element 组件，无裸 HTML 布局替代。
3. 三态（加载/空/错）齐全；列表有分页/刷新；筛选区 ≤3 行。
4. 品牌主色全端一致 `#2B6CFF`（验证：改 `tokens.scss` 一处全端生效）。
5. 主操作按钮统一 `type="primary"`、位置固定（页面右上/表格操作列）。
6. 图表色板/地图浮层/聚合/轨迹回放符合 §5/§6。
7. 请求全走拦截器；分页参数统一；状态枚举走接口源。
8. 无控制台样式报错、无重复/冲突 CSS；echarts/map 实例已销毁。

---

## 第 9 章 落地代码片段

`src/styles/tokens.scss`：
```scss
$c-primary:#2B6CFF; $c-primary-dark:#1A4FD6; $c-primary-light:#E8F0FF;
$c-success:#00B42A; $c-warning:#FF7D00; $c-error:#F53F3F;
$c-text-1:#1D2129; $c-text-2:#4E5969; $c-text-3:#86909C; $c-text-4:#C9CDD4;
$c-bg:#F5F6F8; $c-surface:#FFFFFF; $c-border:#E5E6EB;
:root {
  --c-primary:#2B6CFF; --c-primary-dark:#1A4FD6; --c-primary-light:#E8F0FF;
  --c-success:#00B42A; --c-warning:#FF7D00; --c-error:#F53F3F;
  --c-text-1:#1D2129; --c-text-2:#4E5969; --c-text-3:#86909C; --c-text-4:#C9CDD4;
  --c-bg:#F5F6F8; --c-surface:#FFFFFF; --c-border:#E5E6EB;
  --s-1:4px; --s-2:8px; --s-3:12px; --s-4:16px; --s-5:20px; --s-6:24px; --s-8:32px;
  --f-title:18px; --f-sub:16px; --f-body:14px; --f-aux:13px; --f-tip:12px;
  --r-sm:4px; --r-md:8px; --r-lg:12px; --r-round:999px;
  --shadow-card:0 2px 12px rgba(0,0,0,.04);
  --chart-1:#2B6CFF; --chart-2:#00B42A; --chart-3:#FF7D00; --chart-4:#F53F3F;
  --chart-5:#8E44AD; --chart-6:#13C2C2; --chart-7:#FFC53D; --chart-8:#86909C;
}
```

`src/styles/element-variables.scss`（覆写 Element 主题，必须提交）：
```scss
$--color-primary:#2B6CFF;
$--color-success:#00B42A;
$--color-warning:#FF7D00;
$--color-danger:#F53F3F;
$--color-text-primary:#1D2129;
$--color-text-regular:#4E5969;
$--color-text-secondary:#86909C;
$--color-text-placeholder:#C9CDD4;
$--border-color-base:#E5E6EB;
$--background-color-base:#F5F6F8;
$--border-radius-base:8px;
```

---

*本规范与《开发规范与需求规格说明书》§1 红线、移动端《UI设计规范与页面模板.md》具有同等强制力。冲突以本文档 + 规格书红线为准，修订须人工确认。管理端主色定案 `#2B6CFF`（2026-10-07）——若后续品牌改色，只改 `tokens.scss` + `element-variables.scss` 两处，禁止逐页改色。*
