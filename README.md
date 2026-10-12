<div align="center">

# 🚚 品达物流 TMS

<p>
  <b>面向运输企业与承运商的开源运输管理系统（TMS）</b><br/>
  <span>全流程闭环 · 四端协同 · 微服务架构 · 开箱即用</span>
</p>

<p>
  <img src="docs/badges/license.svg" alt="License"/>
  <img src="docs/badges/spring-cloud.svg" alt="Spring Cloud"/>
  <img src="docs/badges/jdk.svg" alt="JDK 21"/>
  <img src="docs/badges/gateway.svg" alt="Spring Cloud Gateway"/>
  <img src="docs/badges/sa-token.svg" alt="Sa-Token"/>
  <img src="docs/badges/nacos.svg" alt="Nacos"/>
  <img src="docs/badges/mysql.svg" alt="MySQL"/>
  <img src="docs/badges/redis.svg" alt="Redis"/>
  <img src="docs/badges/minio.svg" alt="MinIO"/>
  <img src="docs/badges/elasticsearch.svg" alt="Elasticsearch"/>
  <img src="docs/badges/xxl-job.svg" alt="XXL-Job"/>
  <img src="docs/badges/kafka.svg" alt="Kafka"/>
  <img src="docs/badges/drools.svg" alt="Drools"/>
  <img src="docs/badges/or-tools.svg" alt="OR-Tools"/>
  <img src="docs/badges/spring-ai.svg" alt="Spring AI"/>
  <img src="docs/badges/kuaidi100.svg" alt="快递100"/>
  <img src="docs/badges/doris.svg" alt="Apache Doris"/>
  <img src="docs/badges/flowable.svg" alt="Flowable"/>
  <img src="docs/badges/jimu-report.svg" alt="积木报表"/>
  <img src="docs/badges/debezium.svg" alt="Debezium"/>
  <img src="docs/badges/resilience4j.svg" alt="Resilience4j"/>
  <img src="docs/badges/mapstruct.svg" alt="MapStruct"/>
  <img src="docs/badges/zipkin.svg" alt="Zipkin"/>
  <img src="docs/badges/druid.svg" alt="Druid"/>
  <img src="docs/badges/baidu-map.svg" alt="百度地图"/>
  <img src="docs/badges/netty.svg" alt="Netty"/>
  <img src="docs/badges/vue.svg" alt="Vue"/>
  <img src="docs/badges/uniapp.svg" alt="uni-app"/>
</p>

</div>

<p align="center"><b>简体中文</b> · <a href="README.en.md">English</a></p>

> *Pinda TMS — Open-source Transportation Management System with 4-end synergy, powered by JDK 21 + Spring Boot 3.5 + Vue 3.5.*

> **品达 TMS** 是一款**四端协同**（管理端 + 客户端 + 快递员端 + 司机端）的开源运输管理系统，基于 **JDK 21 + Spring Boot 3.5.7 + Vue 3.5** 构建，覆盖寄件 → 订单 → 智能调度 → 作业 → 轨迹 → 回单全链路。Drools 规则引擎驱动智能调度，Netty 自建轨迹接入，Sa-Token 多租户 SaaS 就绪——覆盖订单管理、配载作业、调度分配、行车管理、GPS 车辆定位、数据报表全流程，让运输运作效率与成本管控一步到位。

<p align="center">
<b><a href="#sec-tech"><code>13</code></a></b> 个微服务 · <b><code>4</code></b> 端协同 · <b><a href="docs/需求文档/01-业务需求全景.md"><code>101</code></a></b> 条功能需求 · <b><a href="docs/sql"><code>54</code></a></b> 张数据表 · <b><a href="docs/01-技术栈说明.md"><code>27</code></a></b> 项技术栈 · 全链路业务闭环
</p>

<p align="center">
🔗 <b>在线体验</b>：<code>http://&lt;内网服务器IP&gt;</code>（部署后替换为实际地址，公网 Demo 规划中）｜ 账号 <code>admin / 123456</code>
</p>

---

## 📑 目录

- [一、项目是什么 · 解决什么问题](#sec-intro)
- [二、核心优势](#sec-advantages)
- [三、解决的核心痛点](#sec-pain)
- [四、四端协同](#sec-ends)
- [五、功能全景](#sec-features)
- [六、技术方案](#sec-tech)
- [七、快速了解](#sec-quickstart)
- [八、系统截图](#sec-screens)
- [九、相关资料](#sec-docs)
- [十、配套虚拟机（环境镜像）](#sec-vm)
- [十一、商业服务与联系](#sec-contact)
- [十二、里程碑](#sec-milestones)
- [十三、常见问题（FAQ）](#sec-faq)
- [十四、参与贡献](#sec-contrib)

---

<a id="sec-intro"></a>

## 一、项目是什么 · 解决什么问题

物流运输市场最普遍的四种业态是 **快递、快运、专线、三方**，它们支撑着全国商品货物的流通。无论是哪一类企业，核心痛点都高度一致：**车辆、驾驶员、线路散落难管，运输过程不透明，调度与计费靠人工，成本与时效无从考核**。

品达物流 TMS 正是为解决这些问题而生——它把「运力资源准备 → 下单 → 调度 → 运输 → 派送 → 签收 → 结算」的全链路搬进一套系统，对车辆、驾驶员、线路进行全面详尽的统计与考核，帮助企业**提高运作效率、降低运输成本，在激烈的市场竞争中处于领先地位**。

> 💡 **为什么做这个项目？** 市面上的 TMS 要么**太贵**（商业年费 3-30 万），要么**太旧**（JDK 8 + Vue 2 停留在上个时代），要么**太重**（实施周期 6 个月起步）。品达 TMS 想做**第三个选择**：开源免费、全链路闭环、技术前沿、四端协同——开箱即用，二次开发友好。

---

<a id="sec-advantages"></a>

## 二、✨ 核心优势

### 2.1 六大差异化亮点

| # | 亮点 | 为什么别家没有 |
| --- | --- | --- |
| 1 | **四端协同** | 管理端 + 客户端 + 快递员端 + 司机端，覆盖全角色——多数开源 TMS 只有管理端 |
| 2 | **JDK 21 + SB 3.5.7 + Vue 3.5** | 同类开源 TMS 中最前沿的栈，多数仍在 JDK 8 + SB 2.x + Vue 2 |
| 3 | **Drools + XXL-Job 智能调度** | 规则引擎驱动调度与运费计算，非 if-else 硬编码；分布式任务调度替代 Quartz |
| 4 | **Netty 自建轨迹接入** | 不依赖第三方 SDK，`pd-netty` 独立微服务，支撑高并发终端接入 |
| 5 | **Sa-Token 多租户** | SaaS 化就绪，一套代码服务多个租户——多数开源 TMS 单租户 |
| 6 | **清晰的演进路线** | Kafka → TDengine/鹰眼 → OR-Tools → Spring AI 规划中，不是做完就停的项目 |

### 2.2 谁适合用品达？

- 🚛 **中小微车队**（10-50 辆）：想数字化，但买不起年费 3-30 万的商业 TMS
- 🚀 **物流初创团队**：需要一个功能全、栈新、能跑的四端 TMS 做 MVP
- 📚 **Java 学习者**：一套代码学微服务 + 规则引擎 + Netty + 多租户实战
- 🔧 **二次开发商**：给客户做定制交付，省 3 个月底子工程

### 2.3 与同类方案对比

| 能力 | 品达 TMS | 同类开源 TMS | 商业 TMS |
| --- | --- | --- | --- |
| 四端协同（管理+客户+快递员+司机） | ✅ | ❌ 仅管理端 | ✅ |
| JDK 21 + Spring Boot 3.5 | ✅ | ❌ 多为 JDK 8 | 部分已升级 |
| 多租户 SaaS | ✅ | ❌ | ✅ |
| 智能调度（Drools 规则引擎） | ✅ | ❌ if-else 硬编码 | ✅ |
| Netty 自建轨迹接入 | ✅ | ❌ | ✅ |
| 年费 | **0（Apache-2.0）** | 0 | **3-30 万** |
| 二开自由 | ✅ 代码全开源 | ✅ | ❌ 黑盒交付 |

> 「同类开源」泛指 Gitee/GitHub 上主流开源 TMS 项目，不针对特定项目；如有不准确欢迎 Issue 纠正。

### 2.4 眼见为实

<table>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/admin-login.png" alt="管理端登录"/>
  <br/><sub><b>管理端登录</b> · 统一账号 + 验证码校验</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/admin-dashboard.png" alt="管理端仪表盘"/>
  <br/><sub><b>工作台仪表盘</b> · 订单 / 告警 / 待办与图表统计</sub>
</td>
</tr>
</table>

> 🎬 **GIF 动图演示制作中**：登录 → 下单 → 智能调度 → 轨迹回放全流程动图即将上线（录制中，欢迎 Star 关注更新）。

### 2.5 功能进度矩阵

|  |  |  |  |
| --- | --- | --- | --- |
| ✅ 订单管理 | ✅ 智能调度 | ✅ 作业管理 | ✅ 轨迹追踪 |
| ✅ 多租户认证 | ✅ 基础数据 | ✅ 运单管理 | ✅ 数据报表 |
| ✅ 四端 App | ✅ 权限 / 菜单 | ✅ 车辆 / 线路 | ✅ 人员管理 |
| 🔲 财务结算 | 🔲 智能排线（OR-Tools） | 🔲 回单 OCR | 🔲 动态 ETA |
| 🔲 AI 客服（Spring AI） | 🔲 ES 检索 | 🔲 对象存储（MinIO） | 🔲 消息管线（Kafka） |

---

<a id="sec-pain"></a>

## 三、🎯 解决的核心痛点

| 行业痛点 | 品达 TMS 的解法 |
| --- | --- |
| 车辆 / 驾驶员 / 线路分散，难以统一考核 | 基础数据集中管理 + 多维统计报表，人车线一目了然 |
| 调度靠人工、配载凭经验 | 定时调度 + 路径规划自动生成运输任务与车次分配 |
| 运输过程黑盒，在途不可见 | GPS 车辆定位 + 轨迹回放，在途状态实时可查 |
| 运费计算不透明、易出错 | Drools 动态规则引擎，计费规则可配置、可追溯 |
| 客户查件无渠道、签收无留证 | 客户端 App 查物流 + 电子签收 / 回单留证 |
| 数据孤岛、统计靠手工 | 数据聚合 + 实时分析，经营报表自动产出 |

---

<a id="sec-ends"></a>

## 四、📱 四端协同

| 端 | 名称 | 使用对象 | 核心能力 |
| --- | --- | --- | --- |
| 后台系统管理端 | 品达 TMS 管理端 | 公司内部管理员 | 基础数据维护、订单管理、运单管理 |
| 客户端 App | **品达速运** | 外部客户 | 寄件、查询物流信息 |
| 快递员端 App | **品达快递员** | 公司内部快递员 | 接收取派件任务 |
| 司机端 App | **品达司机宝** | 公司内部司机 | 接收运输任务、上报位置信息 |

---

<a id="sec-features"></a>

## 五、🔍 功能全景

### 5.1 整体业务流程

![整体业务流程](docs/business-flow.png)

从寄件人下单到收件人签收，品达 TMS 覆盖完整业务链路：

```
寄件下单 📦 → 订单创建 📋 → 智能调度 🤖（Drools）→ 司机接单 🚚
→ 作业执行 ⚙️ → 轨迹追踪 📍（Netty）→ 回单签收 ✅ → 对账结算 💰
```

### 5.2 核心业务模块

| 模块 | 功能描述 |
| --- | --- |
| **订单管理** | 寄件下单、货物管理、订单全生命周期状态管理、运费试算 |
| **智能调度** | 订单分类、路径规划、运输任务创建、车次 / 车辆 / 司机分配 |
| **运输管理（行车管理）** | 运输任务执行、司机作业单、在途状态跟踪 |
| **GPS 车辆定位 / 在途监控** | 车辆实时定位、轨迹回放、偏航与异常监控 |
| **配送执行** | 取派件任务、运单管理、电子签收（POD） |
| **车辆 / 线路 / 车次管理** | 车队、车辆、车型、线路、车次等运输资源维护 |
| **人员管理** | 司机、快递员、岗位与组织管理 |
| **客户门户** | 客户端 App 寄件、查单、物流跟踪 |
| **回单与结算** | 回单签收留证、费用结算、对账 |
| **数据报表** | 运输量、时效、成本、绩效等多维统计 |
| **基础信息维护** | 组织机构、菜单、权限、字典等支撑数据 |
| **统一认证与权限** | 基于开源 Sa-Token 的用户、角色、菜单、多租户认证与鉴权 |

### 5.3 订单状态机（示例）

订单生命周期由一套多级状态机驱动，典型流转如下：

```
待取件 → 已取件 → 待装车 → 运输中 → 网点出库 → 待派送 → 派送中 → 已签收
                                                  └→ 拒收 / 已取消
```

---

<a id="sec-tech"></a>

## 六、🏗 技术方案

### 6.0 项目结构

```
pinda-tms/
├── 微服务（13 个）
│   ├── pd-gateway/           # 统一网关（Spring Cloud Gateway）
│   ├── pd-auth/              # 认证中心（Sa-Token + 多租户）
│   ├── pd-base/              # 基础数据（机构 / 车辆 / 线路 / 货物）
│   ├── pd-oms/               # 订单管理（OMS）
│   ├── pd-dispatch/          # 智能调度（Drools + XXL-Job）
│   ├── pd-work/              # 作业管理（取派件 / 运输任务）
│   ├── pd-user/              # 用户与会员
│   ├── pd-netty/             # 轨迹接入（Netty TCP）
│   ├── pd-aggregation/       # 数据聚合
│   └── pd-web/               # BFF 聚合层（四端接口）
│       ├── pd-web-manager/   #   管理端 BFF
│       ├── pd-web-courier/   #   快递员端 BFF
│       ├── pd-web-customer/  #   客户端 BFF
│       └── pd-web-driver/    #   司机端 BFF
├── 支撑库（2 个 jar）
│   ├── pd-common/            # 公共组件（工具 / 上下文 / 拦截器）
│   └── pd-service-api/       # 服务间 API 契约（Feign 接口）
├── 前端（4 端）
│   ├── pd-admin-ui-vue3/     # 管理端（Vue3 + Element Plus）
│   └── pd-mobile/            # 移动三端（uni-app Vue3）
│       ├── customer/         #   客户端 · 品达速运
│       ├── courier/          #   快递员端 · 品达快递员
│       └── driver/           #   司机端 · 品达司机宝
├── deploy/                   # Docker Compose 编排与部署脚本
└── docs/                     # 文档 + 截图 + 徽章
```

### 6.1 系统架构

![系统架构](docs/system-arch.png)

> 系统分四层：**展示层**（管理端 Vue3 + 移动三端 uni-app）→ **网关层**（Spring Cloud Gateway，Sa-Token 鉴权 + Same-Token 防直连）→ **服务层**（13 个业务微服务，Nacos 注册/配置中心）→ **数据层**（MySQL 单库分域 + Redis + RabbitMQ）。

### 6.2 微服务架构

![微服务架构](docs/microservice-arch.png)

> 微服务按业务域拆分：`pd-auth`（认证）、`pd-gateway`（网关）、`pd-base`（基础数据）、`pd-oms`（订单）、`pd-dispatch`（调度）、`pd-work`（作业）、`pd-netty`（轨迹接入）、`pd-aggregation`（聚合）等，通过 OpenFeign 互调，Nacos 统一注册发现。

### 6.3 技术架构

![技术架构](docs/tech-arch.png)

> 后端 JDK 21 + Spring Boot 3.5.7 + Spring Cloud 2025.0.0 + MyBatis-Plus + Drools + XXL-Job；前端 Vue 3 + Vite + Element Plus + Pinia + ECharts；移动端 uni-app Vue3 + uview-plus；中间件 MySQL 8 + Redis 7 + RabbitMQ + Nacos + Docker Compose。

### 6.4 技术亮点

#### 6.4.1 核心亮点（已落地）

| 技术 | 在系统中的作用 |
| --- | --- |
| **JDK 21 虚拟线程** | 高并发 IO 场景吞吐提升，降低线程开销 |
| **Drools 8.38 动态规则引擎** | 运费计算与业务规则配置，规则可热更新 |
| **XXL-Job 2.4.1 分布式任务调度** | 统一编排智能调度等周期任务（替代 Quartz） |
| **Sa-Token + JWT 多租户认证中心** | 统一登录认证与无状态鉴权，支持多租户与验证码 |
| **Spring Cloud Gateway 统一网关** | 路由转发、登录态校验与防直连 |
| **Netty** | 高并发 TCP 通道，支撑轨迹上报与设备接入 |
| **百度地图** | 地址解析、距离计算、轨迹展示 |
| **Druid 连接池** | 连接管理与 SQL 监控 |

#### 6.4.2 技术选型理由

| 选型 | 为什么不是别的？ |
| --- | --- |
| **Sa-Token** 而非 Shiro | 更轻量、多租户原生、中文文档全、Boot3 生态无兼容包袱 |
| **XXL-Job** 而非 Quartz | 分布式调度自带管理台，Quartz 只能自研调度面 |
| **Drools** 而非 if-else | 调度规则可视化、可热更新、不用改代码发版 |
| **单库分域** 而非 8 分库 | dev / 中小规模运维成本低，规模化再演进时序库 |
| **Resilience4j** 而非 Hystrix | Hystrix 停更，R4 是 Spring Cloud 官方容错方向（规划中） |

#### 6.4.3 代码片段

**Drools 调度规则**（`pd-dispatch`）——规则即配置，热更新不发版：

```java
rule "同城订单优先分配"
when
    $order : Order(distanceType == "LOCAL", priority >= 5)
    $driver : Driver(status == "IDLE", city == $order.pickupCity)
then
    $order.assignTo($driver);
end
```

**Netty 轨迹接入**（`pd-netty`）——自建 TCP 通道，不依赖第三方 SDK：

```java
@ChannelHandler.Sharable
public class LocationServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf in) {
        // 解析位置上报协议 → 轨迹入库 → 推送调度 / 监控
    }
}
```

#### 6.4.4 技术演进路线（规划中）

以下为技术演进评审通过的规划项，尚未落地，按优先级分批实施（见《全量重构总体规划》与《53》R2）：

| 技术 | 用途 | 优先级 |
| --- | --- | --- |
| **MinIO 对象存储** | 回单、签收照片等非结构化数据存储（S3 协议） | P1 近期 |
| **Zipkin 链路追踪** | Micrometer Tracing + Zipkin 微服务调用链监控 | P1 近期 |
| **Resilience4j 服务容错** | 熔断、降级与限流（Spring Cloud CircuitBreaker） | P1 近期 |
| **MapStruct 对象映射** | 替代反射拷贝，编译期生成类型安全转换 | P1 近期 |
| **Kafka 消息流** | 轨迹流管线（Netty → Kafka → 时序存储），高吞吐解耦 | P2 中期 |
| **Debezium CDC 数据同步** | binlog 变更捕获，MySQL → Kafka → ES / Doris 实时同步 | P2 中期 |
| **Elasticsearch 检索** | 运单多条件与地址模糊查询、运营聚合统计 | P2 中期 |
| **Google OR-Tools 智能排线** | VRP 组合优化求解（CVRP / VRPTW），辅助调度配载 | P2 中期 |
| **Apache Doris OLAP** | 报表与经营分析，湖仓一体，MySQL 协议兼容 | P2 中期 |
| **百度鹰眼轨迹服务** | 轨迹纠偏与电子围栏 | P2 中期 |
| **Spring AI 2.0 智能应用框架** | 大模型对话 / RAG / Agent 编排，支撑智能客服与查件 Agent | P3 远期 |
| **快递100 开放平台** | 多承运商轨迹查询与推送、电子面单打印、寄递下单 | P3 远期 |
| **Flowable 工作流引擎** | 异常件审批、赔付改派、对账等人工审批流编排 | P3 远期 |
| **积木报表 JimuReport** | 数据可视化报表与大屏仪表盘，在线设计 | P3 远期 |

### 6.5 数据库设计

全量重构后系统收敛为**单库 MySQL**（`pinda_tms`），按业务域划分表前缀，规模化后可按域平滑拆分：

| 业务域 | 主要表内容 |
| --- | --- |
| 基础数据 | 货物类型、车辆、车队、线路、组织、站点等 |
| 订单（OMS） | 订单、货物信息、订单位置等 |
| 物流作业 | 取派件任务、运输任务、司机作业单、运单等 |
| 智能调度 | 订单分类、路线缓存等调度数据 |
| 认证与权限 | 租户、用户、角色、菜单、资源等 |
| 轨迹与位置 | 车辆轨迹与位置信息（规模化后将引入时序库，见 §6.4 规划项） |

---

<a id="sec-quickstart"></a>

## 七、🚀 快速了解

### 7.1 环境与技术栈

- **后端**：JDK 21 + Spring Boot 3.5.7 + Spring Cloud 2025.0.0（Spring Cloud Alibaba 2025.0.0.0）微服务架构，Nacos 注册 / 配置中心，OpenFeign 服务调用，Spring Cloud Gateway 统一网关鉴权，Sa-Token 多租户认证，XXL-Job 分布式任务调度，Drools 规则引擎。
- **前端**：管理端 `pd-admin-ui-vue3`（Vue 3 + Vite + Element Plus + Pinia + ECharts）；移动端 uni-app（Vue 3 / Vite + uview-plus）三端 App（客户端 / 快递员端 / 司机端）。
- **存储与中间件**：MySQL（单库分域）、Redis 缓存、RabbitMQ 消息队列、Druid 连接池；MinIO 对象存储与 Elasticsearch 检索为规划项（见 §6.4）。
- **支撑服务**：统一认证中心（Sa-Token 多租户 + 验证码）、网关防直连、Netty 轨迹接入。

### 7.2 五分钟跑起来

```bash
git clone https://gitee.com/itxinfei/pinda-tms.git
cd pinda-tms
docker-compose up -d        # 起中间件：MySQL / Redis / RabbitMQ / Nacos
# ① 执行 docs/sql 建库脚本（pinda_tms 单库 + 种子数据）
# ② 启动 pd-auth → pd-gateway → 业务微服务（mvn spring-boot:run 或部署脚本）
# ③ 启动管理端（pnpm i && pnpm dev）与移动端（HBuilderX 运行 uni-app）
```

> 预计 5-10 分钟。详细的部署、配置与二次开发说明见下方「9. 相关资料」；配套虚拟机镜像（中间件全预置）见文末「10. 配套虚拟机」，可向作者索取省去环境搭建。

---

<a id="sec-screens"></a>

## 八、📸 系统截图

> 以下截图来自真实运行环境（2026-10-12 更新）。管理端核心业务页与移动三端均已补充截图。

### 8.1 后台管理端

<table>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/admin-login.png" alt="管理端登录"/>
  <br/><sub><b>管理端登录</b> · 统一账号 + 验证码校验</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/admin-dashboard.png" alt="管理端仪表盘"/>
  <br/><sub><b>工作台仪表盘</b> · 订单 / 告警 / 待办与图表统计</sub>
</td>
</tr>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/admin-order.png" alt="订单管理"/>
  <br/><sub><b>订单管理</b> · 状态筛选 / 分页列表（真实数据）</sub>
</td>
<td align="center" width="50%">
  <div style="border:2px dashed #d0d7de;border-radius:8px;padding:52px 12px;color:#8b949e;background:#fafbfc;">
    📷 待补充<br/><sub>调度管理 / 运单管理等业务页面<br/>（项目运行后截图替换）</sub>
  </div>
</td>
</tr>
</table>

### 8.2 移动端 App

<table>
<tr>
<td align="center" width="33%"><b>客户端 · 品达速运</b><br/><br/>
  <img src="docs/screenshots/mobile-customer.png" alt="客户端 H5" style="max-width:180px;border-radius:8px;border:1px solid #e5e7eb;"/>
  <br/><sub>寄件 / 查物流 · H5（:8084）</sub>
</td>
<td align="center" width="33%"><b>快递员端 · 品达快递员</b><br/><br/>
  <img src="docs/screenshots/mobile-courier.png" alt="快递员端 H5" style="max-width:180px;border-radius:8px;border:1px solid #e5e7eb;"/>
  <br/><sub>取派件任务 · H5（:8083）</sub>
</td>
<td align="center" width="33%"><b>司机端 · 品达司机宝</b><br/><br/>
  <img src="docs/screenshots/mobile-driver.png" alt="司机端 H5" style="max-width:180px;border-radius:8px;border:1px solid #e5e7eb;"/>
  <br/><sub>运输任务 / 位置上报 · H5（:8082）</sub>
</td>
</tr>
</table>

### 8.3 运维与中间件

<table>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-deploy-status.png" alt="部署状态总览"/>
  <br/><sub><b>部署状态总览</b> · 全部微服务健康监测</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-nacos.png" alt="Nacos 控制台"/>
  <br/><sub><b>Nacos 注册 / 配置中心</b> · 服务与配置统一管理</sub>
</td>
</tr>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-rabbitmq.png" alt="RabbitMQ 管理台"/>
  <br/><sub><b>RabbitMQ 管理台</b> · 消息队列运行监控</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-xxljob.png" alt="XXL-Job 调度中心"/>
  <br/><sub><b>XXL-Job 任务调度中心</b> · 定时任务与调度报表</sub>
</td>
</tr>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-docker.png" alt="Docker 容器总览"/>
  <br/><sub><b>Docker 容器总览</b> · 微服务与中间件容器健康状态</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-gitea-workflow.png" alt="Gitea CI/CD 工作流"/>
  <br/><sub><b>Gitea CI/CD 工作流</b> · deploy-backend 按序部署 + 逐服务健康检查（网关最后切）</sub>
</td>
</tr>
<tr>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-mysql.png" alt="MySQL 数据库"/>
  <br/><sub><b>MySQL 8.0 数据库</b> · 终端直连查询 · pinda_tms 全部 54 张表（InnoDB）</sub>
</td>
<td align="center" width="50%">
  <img src="docs/screenshots/ops-redis.png" alt="Redis 键管理"/>
  <br/><sub><b>Redis 7 键管理</b> · Sa-Token 会话缓存与 TTL</sub>
</td>
</tr>
</table>

---

<a id="sec-docs"></a>

## 九、📚 相关资料

以下文档均为仓库内本地文件，可直接跳转阅读。**根目录 `docs/` 与 `docs/需求文档/` 为两套独立编号体系，序号互不相关**：

**① 根目录文档**（`docs/`，部署 / 架构 / 技术栈 / 环境）：

| 文档 | 说明 |
| --- | --- |
| [docs/01-技术栈说明.md](docs/01-技术栈说明.md) | 技术栈说明与核对 |
| [docs/02-架构设计.md](docs/02-架构设计.md) | 系统架构、服务划分、调用关系与部署拓扑 |
| [docs/03-功能规划与需求说明.md](docs/03-功能规划与需求说明.md) | 功能清单与需求说明 |
| [docs/04-部署与二次开发指南.md](docs/04-部署与二次开发指南.md) | 部署、初始化与二次开发指引 |
| [docs/05-部署文档.md](docs/05-部署文档.md) | 中间件部署手册 |
| [docs/06-品达TMS-开发测试环境手册.md](docs/06-品达TMS-开发测试环境手册.md) | 开发 / 测试环境说明 |

**② 需求文档体系**（`docs/需求文档/`，编号独立，权威入口见 00-索引）：

| 文档 | 说明 |
| --- | --- |
| [docs/需求文档/00-索引.md](docs/需求文档/00-索引.md) | 需求文档总索引 |
| [docs/需求文档/01-业务需求全景.md](docs/需求文档/01-业务需求全景.md) | 业务需求全景 |

**设计图**（位于 `docs/`）：`system-arch.png`（系统架构）、`microservice-arch.png`（微服务架构）、`tech-arch.png`（技术架构）、`business-flow.png`（整体业务流程）、`软件架构体系.png`、`服务列表.png`。

---

<a id="sec-vm"></a>

## 十、📦 配套虚拟机（环境镜像）

> 本项目的学习与部署**依赖配套的虚拟机镜像**——镜像中已预置好运行所需的中间件、数据库与基础环境，省去自行搭建的繁琐。
> 该镜像通过**百度网盘**分发，**链接不公开**，需要请直接向作者索取最新网盘地址。

---

<a id="sec-contact"></a>

## 十一、🤝 商业服务与联系

<div align="center">

<table>
<tr>
<td align="center" width="50%">

### 11.1 🛠️ 付费技术服务

本项目 **Apache-2.0 完全开源免费**。同时作者提供商业支持，助你少走弯路：

- 🔧 **二次开发 / 功能定制**（对接平台、新增业务、UI 调整）
- 🚀 **私有化部署交付**（服务器、域名、环境一条龙，含配套虚拟机镜像）
- 🎓 **项目讲解 / 毕设答疑**（架构、源码、面试考点）
- 📦 **全量建库脚本 + 演示数据**
- 💬 日常技术答疑与长期维护支持

<b>👉 商务联系 QQ：<code>747011882</code></b>

</td>
<td align="center" width="50%">

### 11.2 💬 加入社区

<img src="docs/心飞为你飞.jpg" width="170" alt="微信公众号二维码" style="border-radius:8px;border:1px solid #e5e7eb;">

<sub>扫码关注微信公众号「心飞为你飞」</sub>

<br>

🔗 仓库：[gitee.com/itxinfei/pinda-tms](https://gitee.com/itxinfei/pinda-tms)
🏠 作者主页：[gitee.com/itxinfei](https://gitee.com/itxinfei)

</td>
</tr>
</table>

<br>

### 11.3 ⭐ 如果这个项目对你有帮助，欢迎点个 Star 支持，这是对我最大的鼓励！

</div>

---

<a id="sec-milestones"></a>

## 十二、🗓️ 里程碑

| 时间 | 进展 |
| --- | --- |
| 2026-09 | ✅ 项目立项：四端骨架 + 13 微服务 + 全链路业务闭环 |
| 2026-10 | ✅ 技术栈现代化：JDK 21 + SB 3.5.7 + Vue3 + Sa-Token 多租户 + XXL-Job |
| 2026-Q4 | 🔲 轨迹链路升级：Kafka + TDengine / 鹰眼 + ES 运单检索 |
| 2027-H1 | 🔲 智能排线 OR-Tools + MinIO 回单 + Doris 报表 |
| 2027-H2 | 🔲 Spring AI 智能客服 + 回单 OCR + 动态 ETA |

---

<a id="sec-faq"></a>

## 十三、❓ 常见问题（FAQ）

**Q: 能商用吗？**
A: Apache-2.0 协议，商用免费，无附加条件。

**Q: 技术栈为什么这么新（JDK 21 + Spring Boot 3.5）？**
A: 2026 年的新项目没有历史包袱，直接采用最新 LTS，避免日后被迫升级。

**Q: 有文档吗？**
A: 有，见「9. 相关资料」——架构 / 技术栈 / 功能规划 / 部署 / 开发环境手册齐全。

**Q: 移动端支持小程序吗？**
A: uni-app 三端架构（App / H5 / 小程序），当前以 H5 为主，小程序发布前需配置真实 appid。

---

<a id="sec-contrib"></a>

## 十四、🤝 参与贡献

<div align="center">

<img src="https://gitee.com/itxinfei/pinda-tms/badge/star.svg" alt="Gitee Stars"/>
<img src="https://gitee.com/itxinfei/pinda-tms/badge/fork.svg" alt="Gitee Forks"/>

</div>

欢迎提交 Pull Request！提交前请遵守以下约定：

- **代码规范**：后端遵循《阿里巴巴 Java 开发手册》，前端遵循 ESLint + Prettier + Stylelint 约束
- **提交格式**：`type(scope): description`（如 `feat(oms): 新增拆单接口` / `fix(dispatch): 修复路线规划空指针`）
- **不过度设计**：只做直接需要的事，不加未来"可能用到"的功能
- **禁止硬编码**：cron / 阈值 / 地图 Key / 状态数字一律外部化配置
- **所有请求走网关**：`http://<host>:8760/api` + 端前缀，禁止直连微服务

> 提交 PR 前建议先在 Issue 中发起讨论，确认方向一致后再动手。

---

<p align="center"><sub>TMS 开源 · 运输管理系统 · 物流系统 · 车队管理 · 调度系统 · 轨迹追踪 · Spring Cloud 微服务 · JDK 21 · Vue 3 · uni-app</sub></p>

<p align="center"><sub>品达物流 TMS · 开源运输管理系统 · 全流程 · 四端协同 · 微服务架构</sub></p>
