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

## MCP服务
当前配置的MCP服务:
- pencil (设计工具)
- mysql (数据库查询)

## Skills
项目使用的关键skills:
- springboot-patterns
- java-ruoyi
- vue3-admin