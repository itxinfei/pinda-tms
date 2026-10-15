# 贡献指南 · Contributing to Xinfei TMS

感谢关注心飞 TMS！推荐流程：**Issue 讨论 → Fork → 开发 → PR**。提交前请花两分钟读完本指南。

## 1. 环境准备

| 依赖 | 版本要求 | 说明 |
| --- | --- | --- |
| JDK | 21 LTS | 后端编译运行 |
| Maven | 3.9+ | `mvn compile` 全模块构建 |
| Node / pnpm | 20+ / 10+ | 管理端与移动端构建 |
| Docker | 24+ | `docker-compose up -d` 一键起中间件 |

> 数据库建库与种子脚本随配套虚拟机镜像内置，不随本仓库分发，获取方式见 README「快速了解」。

## 2. 分支与提交规范

- 主开发分支为 `master`，功能开发请从最新 `master` 拉出独立分支
- 提交格式：`type(scope): description`，如 `feat(oms): 新增拆单接口`、`fix(dispatch): 修复路线规划空指针`
  - `type` ∈ feat / fix / docs / refactor / test / chore
  - `scope` 用模块或端名：auth / gateway / oms / dispatch / work / base / netty / admin-ui / mobile
- 描述用中文，一次提交聚焦一件事，避免夹带无关格式化改动

## 3. 开发红线（提交前自查）

- **所有请求走网关**：`http://<host>:8760/api` + 端前缀，禁止直连微服务
- **认证请求头为 `token`**（非 `Authorization`）
- **状态枚举禁止硬编码数字**，统一引用公共包共享常量
- cron / 阈值 / 地图 Key 一律外部化配置
- **模块归属**：订单 → `pd-oms`、调度 → `pd-dispatch`、作业 → `pd-work`、基础 → `pd-base`、轨迹 → `pd-netty`，严禁跨模块写业务
- 不过度设计：只做直接需要的事
- SQL 与种子数据不进仓库，涉及数据结构变更请在 Issue 中说明方案

## 4. PR 流程

1. 先在 Issue 中发起讨论，确认方向一致后再动手
2. Fork → 功能分支 → 开发自测（后端 `mvn compile`，前端 `pnpm build`）
3. 按模板填写 PR 描述：变更内容 / 自测情况 / 影响面
4. CI 编译门禁跑绿后等待 Review

## 5. 行为准则与授权

- 社区交流请保持友善、对事不对人
- 提交 PR 即表示同意贡献内容随仓库按 **BUSL-1.1** 协议一并授权
- 商业合作与授权咨询见 README「商业服务与联系」
