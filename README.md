<div align="center">

# 🚚 品达物流-TMS
**运输全流程管理解决方案 | 适用于运输公司与企业运输队**

![JDK](https://img.shields.io/badge/JDK-1.8%2B-brightgreen) ![Maven](https://img.shields.io/badge/Maven-3.3%2B-yellowgreen) ![License](https://img.shields.io/badge/License-Apache-green) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.2.5.RELEASE-brightgreen) ![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR3-blue) ![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2.2.1.RELEASE-orange) ![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.3.0-blueviolet) ![Swagger UI](https://img.shields.io/badge/Swagger-2.9.2-yellowgreen) ![前端](https://img.shields.io/badge/前端-Vue3%20%2B%20Weex-blueviolet) ![前端框架](https://img.shields.io/badge/前端框架-ECharts%20%2B%20Vant-orange) ![后端框架](https://img.shields.io/badge/后端-SpringCloud%20%2B%20MyBatis-green) ![网关](https://img.shields.io/badge/网关-SpringCloudGateway-brightgreen) ![Lombok](https://img.shields.io/badge/Lombok-1.18.4-yellow) ![Shiro](https://img.shields.io/badge/Shiro-1.4.0-red) ![Druid](https://img.shields.io/badge/Druid-1.1.22-lightgrey) ![Drools](https://img.shields.io/badge/Drools-6.5.0.Final-purple) ![Seata](https://img.shields.io/badge/Seata-1.2.0-pink)

</div>

## 📱 关注与交流

![微信公众号二维码](docs/心飞为你飞.jpg)

- 🚀 项目地址：[https://gitee.com/itxinfei/pinda-tms](https://gitee.com/itxinfei/pinda-tms)
- 👥 QQ交流群：[661543188](https://qm.qq.com/cgi-bin/qm/qr?k=gNgch-wCkfUu-QbI7DZSudrax2BN7vY0)
- 📧 邮箱：[747011882@qq.com](mailto:747011882@qq.com)

---

## 一、项目简介

**品达物流-TMS**（Transportation Management System）是面向运输公司的全流程管理系统，覆盖从用户下单到货物交付的全生命周期管理。

系统打通了订单、调度、运单、运输任务、GPS轨迹五大核心环节，让每一票货物的流转全程可控、可视、可追溯。

### 核心业务流

```
C端下单 → 运费智能计算 → 调度引擎自动派单
  → 创建运单与运输任务 → 车次/车辆/司机自动分配
  → 发车 → 在途GPS监控 → 到达确认 → 交付签收
  → 订单状态自动闭环
```

### 🎯 核心价值

- **行业适配性**：支持快递、快运、专线、三方物流四大主流模式
- **降本增效**：智能调度优化路径与资源配载，降低空驶率
- **全程可视化**：GPS实时定位 + 轨迹追踪，物流全程透明
- **数据驱动**：Druid实时分析 + 多数据源，支撑运营决策
- **扩展性强**：微服务架构，支持快速对接WMS、ERP等第三方系统

---

## 二、用户端口

| 端口类型 | 功能描述 | 技术实现 |
|----------|----------|----------|
| **后台管理端** | 基础数据维护、订单/运单/调度管理、权限配置、数据报表 | Vue + Spring Boot Admin |
| **客户端App** | 寄件下单、物流状态实时追踪、异常申报 | Weex + WebSocket实时推送 |
| **快递员App** | 接收取派件任务、扫码签收、异常上报、GPS轨迹采集 | MQTT + GPS采集 |
| **司机端App** | 接收运输指令、实时位置上报、路线导航 | 高德SDK + Kafka消息队列 |

---

## 三、模块功能详解

### 核心业务模块

#### 一、订单管理（pd-oms）

订单服务是整个物流业务的起点，负责订单从创建到签收的全生命周期管理。

- **智能运费计算**：集成Drools规则引擎，根据货物重量和运输距离自动计算运费（首重+续重阶梯定价），定价规则存储在数据库中，支持运行时热加载，无需重启服务即可调整费率
- **百度地图集成**：自动将收件人/发件人地址解析为经纬度坐标，并计算运输距离
- **12级订单状态机**：待取件 → 已取件 → 网点自寄 → 网点入库 → 待装车 → 运输中 → 网点出库 → 待派送 → 派送中 → 已签收/拒收/已取消，完整覆盖物流全流程
- **多货物管理**：每个订单支持多个货物条目，管理名称、数量、体积、重量、货值等
- **对外Feign接口**：向pd-dispatch和pd-work提供订单查询和状态更新能力，是整个系统的数据源头

#### 二、智能调度（pd-dispatch）

调度服务是TMS的"大脑"，基于Quartz定时任务触发，每轮调度自动完成5个阶段的智能决策。

- **Phase 1 - 订单分类**：自动识别到达转运中心的订单，区分"新订单"（网点入库待装车）和"中转订单"（运输中到达当前中心），按起止机构+当前机构三维度智能分组
- **Phase 2 - 路线规划**：采用递归DFS算法查找所有可达中转路径，支持按距离最短/成本最低/用时最少/中转次数最少四种策略选择最优线路，通过MD5校验自动复用已有线路计算结果
- **Phase 3 - 创建运输任务**：查询预生成运单，更新订单状态为"待装车"，创建运输任务并与运单关联
- **Phase 4 - 车次/车辆/司机规划**：按线路查找可用车次，过滤状态正常的车辆和启用的司机，按发车时间最近优先策略自动分配资源，无可用资源时自动降级为手动分配
- **Phase 5 - 生成司机作业单**：根据车次发车时间自动计算计划发车/到达时间，创建司机作业单，未分配到车辆的标记为待人工分配
- **多机构独立调度**：每个转运中心/网点可独立配置调度任务的cron表达式，互不干扰

#### 三、配送作业（pd-work）

配送作业服务管理订单从"运力调度完成"到"最终交付"的末端全流程，是整个系统的执行核心。

- **运单管理（TransportOrder）**：订单经调度拆分后的运输单元，管理运单状态（新建→已装车→到达→到达终端网点）和调度状态流转
- **运输任务（TaskTransport）**：核心实体，承载起运→在途→到达→交付全流程，采用乐观锁保护三个关键状态变更，天然防止并发冲突：
  - `depart()`：待执行 → 进行中（发车确认，记录实际发车时间）
  - `arrive()`：进行中 → 待确认（到达确认，记录实际到达时间）
  - `deliver()`：待确认 → 已完成（交付确认，记录实际交付时间）
- **末端状态自动联动**：`deliver()`成功后自动批量更新关联运单状态为"到达终端网点"，并调用pd-oms将订单状态更新为"已签收"，实现从运输到订单的完整闭环
- **司机作业单（DriverJob）**：将运输任务与司机/车辆绑定的操作单据，记录提货/交付对接人信息
- **取派件任务（TaskPickupDispatch）**：末端取件和派件任务管理，支持签收/拒收状态
- **状态流转审计**：通过`StatusTransitionHistory`记录每一次状态变更的完整历史，支持审计追溯

#### 四、基础数据（pd-base）

基础数据服务提供TMS中14张核心业务表的维护能力，是整个系统的数据基石。

- **车队管理**：车队信息维护，支持按机构筛选查询
- **货物类型**：货物分类管理，与车辆类型建立多对多关联，支持按货物类型或车型联合筛选查询
- **线路类型**：定义干线/支线/城配等线路类型，指定线路起止的机构类型约束
- **线路管理**：关联线路类型和起止机构，记录距离/成本/预计时间等关键参数
- **车次管理**：班次计划管理（发车时间+周期：天/周/月），支持与车辆/司机批量关联
- **车辆管理**：车辆信息维护，关联车型/车队/GPS设备，记录准载重/体积，支持按类型/车牌/车队多维度筛选
- **车辆类型**：标准车型定义（载重/体积/外部尺寸），与货物类型多对多关联，调度时自动匹配适配车型
- **车辆行驶证**：行驶证信息管理，保存后自动回写车辆表的行驶证ID关联
- **司机管理**：司机基本信息与驾驶证管理，支持按userId幂等保存（存在则更新不存在则插入）
- **业务范围**：机构/快递员的行政区域覆盖范围，存储多边形地理坐标，用于调度时的区域匹配

#### 五、轨迹服务（pd-netty）

轨迹服务负责接收GPS设备和司机端的位置数据上报，是物流全程可视化的数据入口。

- **双通道接入**：Netty TCP长连接 + HTTP REST双通道接收轨迹数据，兼容不同类型的设备接入方式
- **Kafka异步转发**：接收JSON格式的GPS位置数据（业务ID、经纬度、时间戳、车辆/快递员类型等），异步转发至Kafka Topic `tms_order_location`，实现与下游业务的解耦
- **下游消费持久化**：轨迹数据由pd-work/pd-dispatch等模块的Kafka Consumer消费后持久化到数据库
- **预留扩展空间**：代码中已预留pd-service-base-api、pd-service-work-api等依赖，为未来设备管理、轨迹持久化等能力做好架构准备

#### 六、C端用户（pd-user）

C端用户数据管理服务，负责客户端用户信息的存储与维护。

#### 七、权限中心（pd-authority）

品达通用权限系统，提供完整的微服务鉴权与安全管理能力，可作为多业务系统的开发脚手架。

- 用户、部门、岗位、角色、菜单的RBAC权限模型
- 网关统一鉴权（JWT Token）
- XSS防跨站攻击

---

### 支撑服务模块

| 模块 | 职责 |
|------|------|
| **pd-web** | API网关，统一路由、限流、熔断、降级 |
| **pd-aggregation** | 多模块数据聚合查询，统一查询入口 |
| **pd-druid** | 数据库连接池管理，SQL监控与统计 |
| **pd-common** | 公共工具类、状态流转校验器、跨服务共享组件 |
| **pd-service-api** | 各模块Feign接口定义、跨服务共享DTO与枚举 |

---

## 四、项目架构

**设计原则**：微服务化 + 数据分层（OLTP/OLAP分离）+ 多级缓存 + 事件驱动

| 层级 | 技术选型 |
|------|----------|
| **前端** | Vue3 + Weex + ECharts + Vant |
| **网关** | Spring Cloud Gateway + Sentinel |
| **服务** | Spring Boot + MyBatis Plus + MapStruct |
| **规则引擎** | Drools（运费计算，支持热加载） |
| **调度** | Quartz（多机构独立调度） |
| **消息** | Kafka + RocketMQ |
| **数据** | MySQL + MongoDB + HBase + Druid |
| **中间件** | Nacos + Redis + Seata + XXL-JOB |
| **监控** | Prometheus + Grafana + SkyWalking |

---

## 五、整体业务流程

![整体业务流程](docs/整体业务流程.png)

**完整流程解析**：

1. **用户下单**：C端提交订单 → 地址自动解析（百度地图） → 距离计算 → Drools规则引擎自动计算运费 → 订单入库
2. **调度触发**：Quartz定时任务触发 → 识别新订单/中转订单 → 按起止机构智能分组
3. **路径规划**：递归DFS算法查找所有可达中转路径 → 按距离/成本/时间/中转次数策略选最优线路 → MD5校验复用缓存
4. **任务创建**：生成运输任务关联运单 → 按线路匹配车次 → 过滤可用车辆和司机 → 按发车时间最近优先分配
5. **在途监控**：司机端持续上报GPS位置 → Netty接收 → Kafka异步转发 → 下游消费持久化
6. **状态流转**：发车确认 → 到达确认 → 交付确认，每一步采用乐观锁保护，防止并发冲突
7. **末端闭环**：交付确认后自动批量更新运单状态 → 调用订单服务更新订单为"已签收"，全程无需人工干预
8. **异常处理**：无可用车辆自动降级为手动分配 → 调度失败自动记录日志

---

## 六、技术亮点

- **Drools动态定价**：运费规则存储在数据库，支持运行时热加载，调整费率无需重启服务
- **递归路径规划**：DFS算法支持任意深度中转路径搜索，4种线路选择策略灵活适配不同业务场景
- **乐观锁状态机**：运输任务depart/arrive/deliver三个关键状态变更采用乐观锁保护，并发场景下数据安全可靠
- **Quartz多租户调度**：每个转运中心可独立配置调度任务cron表达式，支持不同区域差异化调度策略
- **Netty双通道接入**：TCP + HTTP双通道接收GPS轨迹数据，Kafka异步解耦，高吞吐低延迟
- **雪花算法ID**：全模块统一使用分布式Snowflake ID生成，保证分布式环境下ID全局唯一
- **状态流转审计**：`StatusTransitionHistory`记录每一次状态变更的完整历史，支持审计追溯与问题排查

---

## 七、环境要求

| 组件 | 版本要求 |
|------|----------|
| JDK | 1.8+ |
| Maven | 3.3+ |
| MySQL | 5.6.0+ |
| Redis | 4.0+ |
| Nacos | 1.1.4+ |
| MongoDB | 3.9.1+ |
| Kafka | 3.0+ |
| Quartz | 内置（无需独立部署） |

---

## 八、快速开始

### 一、克隆项目

```bash
git clone https://gitee.com/itxinfei/pinda-tms.git
```

### 二、启动中间件

- 启动 Nacos 配置中心（服务注册与配置管理）
- 启动 Redis 缓存服务
- 启动 MySQL 数据库并执行各模块的初始化SQL脚本
- 启动 Kafka 消息队列（轨迹数据管道）
- 启动 RabbitMQ（调度事件通道，可选）

### 三、启动微服务

按以下顺序启动各微服务：

```
公共依赖：pd-common → pd-service-api

基础设施：pd-authority → pd-base → pd-druid → pd-user

核心业务：pd-oms → pd-netty

调度作业：pd-dispatch → pd-work

聚合网关：pd-aggregation → pd-web
```

### 四、访问系统

- 管理端：http://localhost（网关地址）
- Swagger文档：各模块独立端口（默认81xx系列）
- Nacos控制台：http://localhost:8848/nacos

---

## 九、许可证

Apache 2.0
