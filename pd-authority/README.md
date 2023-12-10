# 品达通用权限系统

## 一、项目概述

对于企业中的项目绝大多数都需要进行用户权限管理、认证、鉴权、加密、解密、XSS防跨站攻击等。这些功能整体实现思路基本一致，但是大部分项目都需要实现一次，这无形中就形成了巨大的资源浪费。本项目就是针对这个问题，提供了一套通用的权限解决方案----品达通用权限系统。

品达通用权限系统基于SpringCloud(Hoxton.SR1)  +SpringBoot(2.2.2.RELEASE) 的微服务框架，具备通用的用户管理、资源权限管理、网关统一鉴权、XSS防跨站攻击等多个模块，支持多业务系统并行开发，支持多服务并行开发，可以作为后端服务的开发脚手架。核心技术采用SpringBoot、Zuul、Nacos、Fegin、Ribbon、Hystrix、JWT Token、Mybatis Plus等主要框架和中间件。

本项目具有两个主要功能特性：

- 用户权限管理

  具有用户、部门、岗位、角色、菜单管理，并通过网关进行统一的权限认证

- 微服务开发框架

  本项目同时也是一个微服务开发框架，集成了基础的公共组件，包括数据库、缓存、日志、表单验证、对象转换、防注入和接口文档管理等工具。

### 二、业务架构

![](img/1581494294533.png)

### 三、技术架构

![](img/1581494316483.png)

### 四、环境要求

- JDK ： 1.8 +

- Maven： 3.3 +

  http://maven.apache.org/download.cgi


- Mysql： 5.6.0 +

  https://downloads.mysql.com/archives/community

- Redis： 4.0 +

  https://redis.io/download

- Nacos： 1.1.4

  https://github.com/alibaba/nacos/releases

- Node： 11.3+（集成npm）

  https://nodejs.org/en/download