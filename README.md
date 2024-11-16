# 品达物流-TMS

![品达物流-TMS](docs/pinda.jpg)

**欢迎加入我们的项目维护团队！**

### 演示地址

- **后台管理**: [访问链接](http://pinda.itheima.net/#/login)
- **QQ交流群**: 661543188

### 关联仓库

- **品达物流-通用权限**: [Gitee 仓库](https://gitee.com/itxinfei/pinda-authority)
- **品达物流-集信达**: [Gitee 仓库](https://gitee.com/itxinfei/jixinda.git)

### 项目简介

**品达物流TMS** (Transportation Management System) 是一个运输管理系统，覆盖了从运力准备到货物交付的整个流程。系统主要功能模块包括但不限于：

- 订单管理
- 配载作业
- 调度分配
- 行车管理
- GPS车辆定位
- 车辆管理
- 线路管理
- 车次管理
- 人员管理
- 数据报表
- 基础信息维护

系统旨在提高运营效率，降低成本，并通过全面详尽的统计与评估提升市场竞争力。

#### 用户端口

- **TMS后台系统管理端**: 供公司内部管理员进行基础数据维护及运营管理。
- **客户端App (品达速运)**: 客户用于寄件及追踪物流状态。
- **快递员端App (品达快递员)**: 快递员接收并处理收派件任务。
- **司机端App (品达司机宝)**: 司机接收运输指令并更新位置信息。

### 项目架构

#### 系统架构
![系统架构](docs/系统架构.png)

#### 微服务架构
![微服务架构](docs/微服务架构.png)

#### 软件架构体系
![软件架构体系](docs/软件架构体系.png)

### 技术架构

![技术架构1](docs/技术架构1.png)
![技术架构](docs/技术架构.png)

### 整体业务流程

![整体业务流程](docs/整体业务流程.png)

### 项目结构

- `pd_aggregation`: 存储聚合数据，便于查询。
- `pd_base`: TMS基础数据存储，如车队、车辆等。
- `pd_dispatch`: 定时任务数据。
- `pd_oms`: 订单相关数据。
- `pd_users`: C端用户数据。
- `pd_work`: 作业相关数据，比如快递员与司机的任务。

### 数据库设计

| 数据库名      | 描述                                                         |
| ------------- | ------------------------------------------------------------ |
| `pd_base`     | 存储TMS的基础数据。                                          |
| `pd_users`    | 存储C端用户的个人信息。                                      |
| `pd_oms`      | 订单信息存储。                                               |
| `pd_work`     | 物流作业数据。                                               |
| `pd_aggregation` | 聚合查询数据。                                              |
| `pd_dispatch` | 智能调度相关数据。                                           |
| `pd_auth`     | 企业内员工权限数据。                                         |
| `customer_auth` | C端用户认证信息。                                           |

### Maven依赖配置

项目中用到了两个自定义Maven依赖，其坐标如下：

```xml
<dependency>
  <groupId>com.itheima</groupId>
  <artifactId>pd-auth-entity</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>com.itheima</groupId>
  <artifactId>pd-auth-api</artifactId>
  <version>1.0.0</version>
</dependency>

这些依赖不在Maven中央仓库中，需要配置本地Maven私服来获取它们。请参考settings.xml文档进行配置。