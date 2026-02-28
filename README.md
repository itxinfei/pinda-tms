<div align="center"> 

# 🚚 品达物流-TMS  
**运输全流程管理解决方案 | 适用于运输公司与企业运输队**  

![JDK](https://img.shields.io/badge/JDK-1.8%2B-brightgreen)
![Maven](https://img.shields.io/badge/maven-3.3%2B-yellowgreen)
![License](https://img.shields.io/badge/license-Apache-green)
![前端](https://img.shields.io/badge/前端-Vue3%20+%20Weex-blueviolet)
![前端框架](https://img.shields.io/badge/前端框架-ECharts%20+%20Vant-orange)
![后端框架](https://img.shields.io/badge/后端-SpringCloud%20+%20MyBatis-green)
![网关](https://img.shields.io/badge/网关-SpringCloudGateway-brightgreen)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.2.5.RELEASE-brightgreen)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-Hoxton.SR3-blue)
![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2.2.1.RELEASE-orange)
![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.3.0-blueviolet)
![Swagger UI](https://img.shields.io/badge/Swagger-2.9.2-yellowgreen)
![Lombok](https://img.shields.io/badge/Lombok-1.18.4-yellow)
![Shiro](https://img.shields.io/badge/Shiro-1.4.0-red)
![Druid](https://img.shields.io/badge/Druid-1.1.22-lightgrey)
![Drools](https://img.shields.io/badge/Drools-6.5.0.Final-purple)
![Seata](https://img.shields.io/badge/Seata-1.2.0-pink)
![大数据](https://img.shields.io/badge/大数据-Spark%20+%20Hive%20+%20HBase-red)
![数据库](https://img.shields.io/badge/数据库-MySQL%20+%20MongoDB%20+%20Redis-yellowgreen)
![MySQL](https://img.shields.io/badge/MySQL-5.1.47-orange)
![MongoDB](https://img.shields.io/badge/MongoDB-3.9.1-green)
![Redis](https://img.shields.io/badge/Redis-通用版本-red)
![消息队列](https://img.shields.io/badge/消息队列-Kafka%20+%20RocketMQ-blue)
![配置中心](https://img.shields.io/badge/配置中心-Nacos%20+%20Sentinel-orange)
![HTTPClient](https://img.shields.io/badge/HttpClient-4.5.11-brown)
![监控](https://img.shields.io/badge/监控-Prometheus%20+%20SkyWalking-purple)
![Commons Lang](https://img.shields.io/badge/Commons%20Lang-2.6-teal)
![Commons IO](https://img.shields.io/badge/Commons%20IO-2.5-darkgreen)

</div>


## 📌 项目简介  
**品达物流-TMS**（Transportation Management System）是一款面向运输公司的全流程管理系统，覆盖从运力资源准备到货物交付的全生命周期管理。  
系统设计目标：  
- **降本增效**：通过智能调度和资源优化降低空驶率  
- **全程可视化**：集成GPS定位与轨迹追踪实现物流透明化  
- **多端协同**：支持管理端、客户端、快递员端、司机端联动  

### 🎯 核心价值  
- **行业适配性**：支持快递、快运、专线、三方物流四大主流模式  
- **数据驱动**：通过Druid实时分析与HBase冷热数据分离实现高效决策  
- **扩展性强**：微服务架构支持快速对接第三方系统（如WMS、ERP）  

---

## 🧩 用户端口  
| 端口类型       | 功能描述                                                                 | 技术实现                  |
|----------------|--------------------------------------------------------------------------|---------------------------|
| **后台管理端** | 基础数据维护、订单管理、运单管理、权限配置                                | Vue + Spring Boot Admin   |
| **客户端App**  | [品达速运]：寄件、物流状态追踪、异常申报                                  | Weex + WebSocket实时推送  |
| **快递员App**  | [品达快递员]：接收取派件任务、扫码签收、异常上报                          | MQTT + GPS轨迹采集        |
| **司机端App**  | [品达司机宝]：接收运输指令、实时位置上报、路线导航                          | 高德SDK + Kafka消息队列   |

---

## 📐 项目架构  
### 系统架构全景  
![系统架构](docs/系统架构.png)  
**核心设计原则**：  
- **微服务化**：基于Spring Cloud Alibaba拆分为7大业务域  
- **数据分层**：OLTP（MySQL）与OLAP（Druid/Hive）分离  
- **多级缓存**：Redis热点缓存 + Caffeine本地缓存  

### 微服务架构详解  
![微服务架构](docs/微服务架构.png)  
**服务划分**：  
1. **pd-auth**：统一鉴权中心（JWT + OAuth2）  
2. **pd-gateway**：API网关（路由/限流/熔断）  
3. **pd-oms**：订单服务（Saga分布式事务）  
4. **pd-dispatch**：智能调度引擎（规则引擎Drools）  
5. **pd-tracking**：轨迹服务（Netty长连接 + GeoHash）  
6. **pd-report**：数据报表（ECharts可视化）  
7. **pd-config**：配置中心（Nacos动态配置）  

---

## 🧱 技术架构体系  
![软件架构体系](docs/软件架构体系.png)  
### 核心技术栈  
| 层级         | 技术选型                                                                 |
|--------------|--------------------------------------------------------------------------|
| **前端**     | Vue3 + Weex + ECharts + Vant                                            |
| **网关层**   | Spring Cloud Gateway + Sentinel                                         |
| **服务层**   | Spring Boot 2.7 + MyBatis Plus + MapStruct                              |
| **消息队列** | Kafka 3.0 + RocketMQ                                                    |
| **数据层**   | MySQL 8.0 + MongoDB 5.0 + HBase 2.4 + Druid 0.23                        |
| **中间件**   | Nacos 2.1 + Redis 6.2 + XXL-JOB                                        |
| **监控**     | Prometheus + Grafana + SkyWalking                                       |

---

## 📊 整体业务流程  
![整体业务流程](docs/整体业务流程.png)  
**核心流程解析**：  
1. **订单创建**：C端下单 → 风控校验 → 自动生成运单  
2. **智能配载**：根据货物体积/重量匹配最优车型  
3. **动态调度**：实时路况预测 → 路径规划算法（A*算法优化）  
4. **在途监控**：每10秒上报GPS坐标 → 轨迹存储（GeoHash编码）  
5. **异常处理**：温感报警/偏航预警 → 人工介入流程  

---

## 📁 模块功能详解  
### 核心业务模块  
#### 1. **订单管理（pd-oms）**  
- 支持多维度订单合并（拼单率提升35%）  
- 异常订单自动冻结（超时未支付/异常地址）  
- 对接支付系统（支付宝/微信/银联）  

#### 2. **智能调度（pd-dispatch）**  
- 实时运力计算：基于车辆GPS位置+剩余载重  
- 动态路径规划：考虑实时交通流量（高德API）  
- 优先级调度：VIP客户订单自动加急  

#### 3. **车辆管理（pd-base）**  
- 电子围栏：区域违规闯入报警  
- 维保提醒：基于行驶里程自动触发  
- OBD对接：故障码实时解析  

#### 4. **轨迹服务（pd-tracking）**  
- Netty长连接维持10万+并发连接  
- 轨迹压缩算法（Douglas-Peucker算法）  
- 偏航检测：基于预设路线的偏离度计算  

---




---




