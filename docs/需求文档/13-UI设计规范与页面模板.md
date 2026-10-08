# 品达物流 TMS — UI 设计规范与页面模板（移动端 uni-app + Vue3）

> **目的**：约束 AI 编码代理（Claude Code 等）写页面时的样式与结构，**彻底杜绝"样式随手写、颜色不统一、组件各写一套、间距乱飞"**。
> **强制力**：等同《开发规范与需求规格说明书》§1 红线。所有页面**必须**遵守本规范；任何自定义 HTML/CSS 绕过组件库的行为视为不合格。
> **配套**：`开发规范与需求规格说明书.md` §7（移动端）、`ClaudeCode开发纪律与红线.md`、**《管理端UI规范与页面模板.md》（管理端 Element UI 版，品牌主色统一 `#2B6CFF`，2026-10-07 新增，索引第 34 条）**。

---

## 第 1 章 设计原则（最高优先级）

1. **只用组件库，不写裸样式**：移动端 100% 使用 **uview-plus** 组件（`u-button`/`u-cell`/`u-card`/`u-form`/`u-tag`/`u-empty`/`u-loading-page`/`u-tabbar`/`u-navbar` 等）。**禁止**为布局/按钮/卡片手写 `<div>` + `<style>` 替代组件。
2. **设计 Token 全集中**：所有颜色/间距/字号/圆角/阴影只允许引用 §2 的 CSS 变量或 uview-plus 主题变量，**禁止在页面里写死 hex 色值、写死 px**（安全区/1rpx 边框除外）。
3. **一套 Token 全端统一**：客户/快递员/司机三端共用同一套 Token 与组件，仅通过"角色化强调"区分（如司机端按钮更大、对比更强），**不允许三端各搞一套视觉**。
4. **结构走模板**：每类页面套用 §4 的标准骨架，**不允许自由布局**。
5. **三态必做**：每个列表/请求页必须有 加载中 / 空 / 错误 三态，统一用组件库实现。

---

## 第 2 章 设计 Token（唯一真源）

> 在 `common/styles/variables.scss` 定义 CSS 变量，并在 `uni.scss` 覆盖 uview-plus 主题变量（见 §6）。

### 2.1 颜色（语义化，禁止其他色值）
| Token | 值 | 用途 |
|---|---|---|
| `--c-primary` | `#2B6CFF` | 品牌主色（按钮、选中、主链接） |
| `--c-primary-dark` | `#1A4FD6` | 主色按压/深色背景 |
| `--c-primary-light` | `#E8F0FF` | 主色浅底（标签底、选中底） |
| `--c-success` | `#00B42A` | 完成/正常/成功 |
| `--c-warning` | `#FF7D00` | 待处理/预警/进行中 |
| `--c-error` | `#F53F3F` | 异常/失败/驳回 |
| `--c-text-1` | `#1D2129` | 主标题/正文重点 |
| `--c-text-2` | `#4E5969` | 正文 |
| `--c-text-3` | `#86909C` | 辅助说明 |
| `--c-text-4` | `#C9CDD4` | 占位/禁用 |
| `--c-bg` | `#F5F6F8` | 页面背景 |
| `--c-surface` | `#FFFFFF` | 卡片/弹层背景 |
| `--c-border` | `#E5E6EB` | 分割线/边框 |
| `--c-mask` | `rgba(0,0,0,0.45)` | 遮罩 |

> 业务状态色映射（统一，禁止自创）：**待处理=warning / 进行中=primary / 已完成=success / 异常=error**。

### 2.2 间距（只允许这些值）
`--s-1:4rpx` `--s-2:8rpx` `--s-3:12rpx` `--s-4:16rpx` `--s-5:20rpx` `--s-6:24rpx` `--s-8:32rpx`
> 页面内边距统一 `--s-4`(16rpx)；卡片间距 `--s-3`(12rpx)；区块间距 `--s-5`(20rpx)。不允许其他间距值。

### 2.3 字号（只允许这些值）
`--f-title:32rpx`(页面标题) `--f-sub:28rpx`(卡片标题) `--f-body:28rpx`(正文) `--f-aux:24rpx`(辅助) `--f-tip:22rpx`(说明)
> 注：uni-app 默认 `28rpx≈14px`。正文不得低于 24rpx，保证可读性。

### 2.4 圆角 / 阴影
`--r-sm:8rpx` `--r-md:16rpx` `--r-lg:24rpx` `--r-round:999rpx`
阴影：`--shadow-card:0 2rpx 12rpx rgba(0,0,0,0.04)`（卡片用，禁止重阴影）。

### 2.5 图标
统一使用 **uview-plus 内置图标** 或项目 `common/icons/` 内 SVG（线性、2px 描边、currentColor）。**禁止**引入第二个图标库、禁止 emoji 当图标、禁止 base64 内联大图。

---

## 第 3 章 全局结构规范

### 3.1 必须统一的容器
- 每个页面根节点：`<view class="page">`（背景 `--c-bg`，最小高度 100vh）。
- 顶部：`u-navbar`（统一标题、返回键、右侧操作位）。
- 三端底部：`u-tabbar`（图标+文字，选中态用 `--c-primary`）。
- 内容区：纵向滚动容器，统一内边距 `--s-4`。

### 3.2 统一反馈组件（禁止自造）
- 加载：`u-loading-page`（整页）/ `u-loading-icon`（局部）。
- 空态：`u-empty`（统一文案"暂无数据"）。
- 错误：`u-empty` + 重试按钮（调同一请求方法）。
- 提示：`u-toast`（成功/失败）、`u-modal`（确认）。
- 下拉刷新/上拉加载：页面 `onPullDownRefresh` + `uni.showLoading`，列表用 `u-list`/`u-loadmore`。

### 3.3 统一操作栏
- 底部固定操作栏：`<view class="action-bar">` 内含 `u-button`（块级、高度 88rpx、圆角 `--r-round`）。
- 主操作=primary 填充；次操作=outline/light；危险操作=error。

---

## 第 4 章 标准页面模板（必须套用）

> 每个新页面先选模板，再填内容。**禁止**偏离模板结构。

### 4.1 列表页（任务/订单/运单）
```
[u-navbar: 标题]
[筛选栏: u-search + u-tabs（状态）]   // 可选
[u-list 滚动]
  [u-card ×N: 左=标题+状态tag; 右=距离/时间; 底=关键字段; 点击进详情]
[u-loadmore: 加载更多/没有更多]
[空态: u-empty] [错态: u-empty+重试]
[u-tabbar]
```

### 4.2 详情页
```
[u-navbar: 返回+标题]
[状态条: 大号状态 tag + 关键时间]
[u-card: 分段标题 + u-cell 每行字段]
[u-card: 轨迹/地图入口按钮]
[底部 action-bar: 主操作按钮(如"扫码签收"/"交付")]
```

### 4.3 表单页（寄件/地址/上报）
```
[u-navbar: 标题]
[u-form: u-form-item(标签左、输入右)]
  [u-input / u-textarea / u-picker(时间/地区) / u-upload(图片)]
[u-button block: 提交]   // 校验失败用 u-form 的 rule + u-toast
```

### 4.4 地图页
```
<map 全屏>（⚠️ **uview-plus 没有 `u-map` 组件**，必须用 uni-app 原生 `<map>`）
  [浮层: u-button 定位/打卡/导航(右上角竖排)]
  [底部 sheet: `u-popup`（或 `u-action-sheet`）+ 自定义内容，显示当前点信息(车牌/距离/状态)]
  > ⚠️ **uview-plus 没有 `u-sheet` 组件**
```
> 小程序端坐标须先经 `common/utils/coord.ts` 做 BD-09→GCJ-02 转换。

### 4.5 任务大厅页（司机/快递员核心）
```
[角色化标题]
[u-list]
  [任务卡片: 顶部状态tag; 中=起终点(大字); 右=距离/预计时长; 底=时间+金额/件数; 主按钮=扫码/提货/交付]
[u-tabbar]
```

### 4.6 POD 电子签收页
```
[u-navbar: 运单号]
[运单摘要 card]
[签名区: canvas 手写签名组件(uview-plus 无则放 common/components/SignaturePad)]
[拍照区: u-upload(带 GPS 水印, 水印由公共方法叠加)]
[异常选项: u-radio-group(正常/破损/拒收)]
[底部 action-bar: 提交]
```

---

## 第 5 章 角色化 UI 规范

### 5.1 司机端（App，户外/单手）
- **按钮加大**：主按钮高度 ≥ 96rpx、字号 `--f-sub`，**单手可达**（主操作置底、thumb-zone 内）。
- **强对比**：背景白/浅灰，文字 `--c-text-1`，状态用大号 tag。
- **信息密度低**：一屏一件事（任务卡片 / 地图 / 交付），不堆表单。
- **离线提示**：无网络时顶部红条提示"离线模式，数据将缓存后补传"。

### 5.2 快递员端（取派）
- **扫码为主操作**：首页/任务卡显眼 `u-button` 唤起 `uni.scanCode`。
- **任务卡片优先**：列表即工作台，状态 tag + 距离/时间 + 一键操作。
- **POD 入口显式**：签收按钮直接进入 §4.6。

### 5.3 客户端（C 端，极简）
- **流程最短**：寄件 3 步（地址→货物→确认付费）；查单 1 入口。
- **运费即时反馈**：输入即调 `mailing/totalPrice` 试算，滑块/卡片展示。
- **实时追踪**：地图 + 状态时间轴，弱化设置类功能。

---

## 第 6 章 主题变量落地（代码片段）

`uni.scss`（覆盖 uview-plus 主题，必须提交，禁止改其他值）：
```scss
/* 品牌主色 */
$u-primary: #2B6CFF;
$u-success: #00B42A;
$u-warning: #FF7D00;
$u-error:   #F53F3F;
$u-main-color: #1D2129;
$u-content-color: #4E5969;
$u-tips-color: #86909C;
$u-border-color: #E5E6EB;
$u-bg-color: #F5F6F8;
$u-radius: 16rpx;
```

`common/styles/variables.scss`：
```scss
page {
  --c-primary:#2B6CFF; --c-primary-dark:#1A4FD6; --c-primary-light:#E8F0FF;
  --c-success:#00B42A; --c-warning:#FF7D00; --c-error:#F53F3F;
  --c-text-1:#1D2129; --c-text-2:#4E5969; --c-text-3:#86909C; --c-text-4:#C9CDD4;
  --c-bg:#F5F6F8; --c-surface:#FFFFFF; --c-border:#E5E6EB; --c-mask:rgba(0,0,0,.45);
  --s-1:4rpx; --s-2:8rpx; --s-3:12rpx; --s-4:16rpx; --s-5:20rpx; --s-6:24rpx; --s-8:32rpx;
  --f-title:32rpx; --f-sub:28rpx; --f-body:28rpx; --f-aux:24rpx; --f-tip:22rpx;
  --r-sm:8rpx; --r-md:16rpx; --r-lg:24rpx; --r-round:999rpx;
  --shadow-card:0 2rpx 12rpx rgba(0,0,0,.04);
  background: var(--c-bg); color: var(--c-text-2);
}
.page { min-height:100vh; background:var(--c-bg); padding:var(--s-4); }
.action-bar { position:fixed; left:0; right:0; bottom:0; padding:var(--s-3) var(--s-4);
  background:var(--c-surface); border-top:1rpx solid var(--c-border); }
.card { background:var(--c-surface); border-radius:var(--r-md); box-shadow:var(--shadow-card);
  padding:var(--s-4); margin-bottom:var(--s-3); }
```

> 页面样式**只允许**使用上述 class 与变量；新增通用样式须先加到 `variables.scss` 并经人工确认。

---

## 第 7 章 页面编写禁止清单（AI 易犯，一律回退）

- ❌ 手写 `<div>`/`<span>` 替代 `u-*` 组件做布局或按钮。
- ❌ 页面 `<style>` 里写死 `#xxxxxx` / `16px` / 任意魔法数字；写死 `!important`。
- ❌ 引入第二个 UI 库、第二个图标库、或 Tailwind/UnoCSS 等原子化框架。
- ❌ 三端各写一套颜色/间距（必须共用 §2 Token）。
- ❌ 同一状态用不同颜色（必须按 §2.1 状态色映射）。
- ❌ 缺加载中/空/错误三态之一。
- ❌ 自定义导航栏样式绕过 `u-navbar`（除非全屏地图页）。
- ❌ 按钮高度/圆角不一致；主按钮非 `--r-round` 非块级。
- ❌ 用 emoji 当图标、用截图当占位图。
- ❌ 表单无校验、提交无 loading/无反馈。
- ❌ 列表无分页/无下拉刷新/无限加载（数据可能多）。
- ❌ 直接 `uni.request` 调接口（必须经 `common/request/index.ts` 拦截器）。

---

## 第 8 章 验收（UI DoD）

1. 所有颜色/间距/字号取自 §2 Token 或 `uni.scss`，源码无硬编码色值/魔法数字。
2. 全部使用 uview-plus 组件，无裸 HTML 布局替代。
3. 三态（加载/空/错）齐全；列表有刷新/分页。
4. 三大端视觉一致（同 Token、同组件、同结构），仅角色化强调差异。
5. 主操作按钮统一块级 + `--r-round` + 固定高度。
6. 通过 `uni.scss` + `variables.scss` 一处改主题全端生效（验证：改 `--c-primary` 全端变）。
7. 无控制台样式报错、无重复/冲突 CSS。

---
*本规范与《开发规范与需求规格说明书》具有同等强制力。冲突以本文档 + 规格书红线为准，修订须人工确认。*
