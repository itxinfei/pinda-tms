# 品达物流-TMS（项目维护中...）
![输入图片说明](docs/pinda.jpg)
### 演示地址：

后台管理：http://pinda.itheima.net/#/login

### 一、关联仓库
- 品达物流-前端工程：https://gitee.com/itxinfei/pinda-front（版权问题不开放）
- 品达物流-通用权限：https://gitee.com/itxinfei/pinda-authority
- 品达物流-集信达：https://gitee.com/itxinfei/jixinda.git
### 二、项目介绍
本项目名称为品达物流TMS，TMS全称为：Transportation Management System，即运输管理系统，是对运输作业从运力资源准备到最终货物抵达目的地的全流程管理。
TMS系统适用于运输公司、各企业下面的运输队等，它主要包括订单管理、配载作业、调度分配、行车管理、GPS车辆定位系统、车辆管理、线路管理、车次管理、
人员管理、数据报表、基本信息维护等模块。该系统对车辆、驾驶员、线路等进行全面详细的统计考核，能大大提高运作效率，降低运输成本，使公司能够在激烈的市场竞争中处于领先地位。

本项目从用户层面可以分为四个端：TMS后台系统管理端、客户端App、快递员端App、司机端App。

TMS后台系统管理端：公司内部管理员用户使用，可以进行基础数据维护、订单管理、运单管理等
客户端App：App名称为品达速运，外部客户使用，可以寄件、查询物流信息等
快递员端App：App名称为品达快递员，公司内部的快递员使用，可以接收取派件任务等
司机端App：App名称为品达司机宝，公司内部的司机使用，可以接收运输任务、上报位置信息等

### 三、项目架构
#### 1、系统架构
![输入图片说明](docs/系统架构.png)
#### 2、微服务架构
![输入图片说明](docs/微服务架构.png)
#### 3、软件架构体系
![输入图片说明](docs/软件架构体系.png)
### 四、技术架构

![输入图片说明](docs/技术架构1.png)

![输入图片说明](docs/技术架构.png)

### 五、整体业务流程

![输入图片说明](docs/整体业务流程.png)

### 六、项目结构

pd_aggregation:存放聚合之后的数据，便于查询

pd_base:存放TMS的基础数据，例如：车队、车辆、线路等

pd_dispatch:存放定时任务相关数据

pd_oms:存放订单相关数据

pd_users:存放C端用户相关数据

pd_work:存放作业相关数据，例如快递员的取件作业、司机的运输作业等

### 数据库设计

| 数据库名       | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| pd_base        | 基础数据数据库，存储TMS项目中的基础数据，例如：货物类型、车辆类型、车队、线路、车次等基础数据 |
| pd_users       | 用户数据库，存储TMS项目中的C端用户相关数据，例如：用户信息、地址簿等数据 |
| pd_oms         | 订单数据库，存储TMS项目中的订单相关数据，例如：货物信息、订单信息、订单位置信息等数据 |
| pd_work        | 物流作业数据库，存储TMS项目中的运输作业相关数据，例如：快递员取派件任务、运输任务、司机作业单、运单等数据 |
| pd_aggregation | 数据聚合数据库，将其他业务数据库中的数据统一聚合到当前数据库，提供查询功能 |
| pd_dispatch    | 智能调度数据库，存储TMS项目中智能调度服务产生的相关数据，例如：订单分类、缓存路线等数据 |
| pd_auth        | 通用权限数据库，存储权限相关数据，例如：资源、菜单、权限、岗位、组织、角色等数据，此数据库存储的是企业内部员工用户相关数据 |
| customer_auth  | 通用C端用户数据库，存储C端登录用户相关数据，例如：用户认证信息、登录记录等数据 |

### 缺少jar问题

 maven工程中使用到了通用权限系统中的两个jar，对应的maven坐标如下： 

```
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
```

 这两个jar在maven中央仓库是没有的，我们自己搭建了maven私服来管理这两个jar，这就需要在本地maven的settings.xml中进行私服的配置： 

 详细配置参照settings.xml