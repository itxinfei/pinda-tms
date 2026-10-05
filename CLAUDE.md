# 品达TMS项目配置

## 语言设置
- **工作语言**: 中文 (Simplified Chinese)
- **交互界面**: 中文
- **代码注释**: 中文优先

## 项目概述
- **项目名称**: 品达物流TMS (运输管理系统)
- **技术栈**: Spring Cloud Alibaba + Vue3
- **模块数量**: 12个微服务模块

## 核心模块
| 模块 | 说明 |
|------|------|
| pd-oms | 订单服务 |
| pd-dispatch | 智能调度 |
| pd-work | 配送作业 |
| pd-netty | 轨迹服务(GPS) |
| pd-base | 基础数据 |
| pd-auth | 鉴权中心 |

## 业务流程序列
订单创建 → 智能调度 → 创建运单 → 运输任务 → 发车/到达/交付 → GPS轨迹上报

## 开发规范
- 使用中文注释
- 接口方法使用英文(Feign规范)
- 实体类属性使用英文
- 枚举类使用中文描述

## 已完成的优化
1. TaskTransport状态更新方法 (depart/arrive/deliver)
2. TransportOrder创建逻辑完善
3. GPS数据Kafka发送确认
4. 订单-运单-任务状态联动

## 事实校正（2026-10-05 源码实测，见 docs/需求文档/文档与代码一致性核查报告.md）
- 技术栈：前端 `pd-admin-ui` 实际是 **Vue 2.6 + Element UI 2.12 + ECharts 4.2**（非 Vue3，且已决策不迁移）。
- 第 4 条"状态联动"**只完成一半**：仅"运输任务交付"环节回写运单(4)与订单(23006 网点出库)；**司机发车(depart)不写订单 23005「运输中」**，客户看不到运输中状态。改造前不得视为已完成。
- 登录：鉴权中心 `pd-auth-server` 已有 `POST /anno/login`；JWT 请求头字段是 `token`；**禁止直连 pd-web-* 端口**（`TokenAuthInterceptor` 会 401），必须走网关 `:8760/api`。

## MCP服务
当前配置的MCP服务:
- pencil (设计工具)
- mysql (数据库查询)

## Skills
项目使用的关键skills:
- springboot-patterns
- java-ruoyi
- vue3-admin