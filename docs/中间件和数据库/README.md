# 心飞物流 TMS 中间件与数据库（开源分发版）

本目录包含心飞物流 TMS 运行所需的全部**中间件配置**和**数据文件**，
供开源用户在新机器上用 Docker 快速还原完整运行环境。

> **导出时间**：2026-10-21
> **来源环境**：Ubuntu Server 24.04 / Docker 29.x
> **适用版本**：心飞物流 TMS 开源版

---

## 一、中间件版本清单

| 中间件 | 镜像版本 | 端口 | 用途 |
|---|---|---|---|
| MySQL | `mysql:8.0` (8.0.46) | 3306 | 业务数据库（单库 `xinfei_tms`） |
| Redis | `redis:7` | 6379 | 缓存 + 登录会话 |
| Nacos | `nacos/nacos-server:v2.3.2` | 8848 / 9848 / 9849 | 注册中心 + 配置中心 |
| RabbitMQ | `rabbitmq:3.12-management` | 5672 / 15672 | 消息队列 |
| XXL-Job | `xuxueli/xxl-job-admin:2.4.1` | 8090 | 分布式任务调度（库在 MySQL `xxl_job`） |

---

## 二、目录结构

```
中间件和数据库/
├── docker-compose.infra.yml    # 中间件编排文件（MySQL/Redis/Nacos/RabbitMQ）
├── .env.example                # 口令变量模板（开发环境默认 123456）
├── my.cnf                      # MySQL 运行参数
├── rabbitmq-20-xinfei.conf     # RabbitMQ 内存/磁盘水位配置
├── README.md                    # 本文件
└── data/                       # 数据文件（开源分发核心）
    ├── mysql-all-databases.sql.gz   # MySQL 全库备份（xinfei_tms + xxl_job）
    ├── redis-data.tar.gz             # Redis 数据目录（AOF/RDB）
    ├── nacos-data.tar.gz             # Nacos Derby 配置库（快速还原用）
    ├── rabbitmq-defs.json            # RabbitMQ 拓扑定义（交换机/队列/绑定）
    └── nacos-configs/               # Nacos 配置文件（yaml，逐条导入用）
        ├── MANIFEST.txt             # 配置清单
        ├── xfwl-gateway.yml.xinfei-tms.yaml
        ├── xfwl-auth.yaml.xinfei-tms.yaml
        ├── xfwl-base.yml.xinfei-tms.yaml
        ├── xfwl-oms.yml.xinfei-tms.yaml
        ├── xfwl-work.yml.xinfei-tms.yaml
        ├── xfwl-dispatch.yml.xinfei-tms.yaml
        ├── xfwl-user.yml.xinfei-tms.yaml
        ├── xfwl-netty.yml.xinfei-tms.yaml
        ├── xfwl-aggregation.yml.xinfei-tms.yaml
        ├── xfwl-file-server.yml.xinfei-tms.yaml
        ├── xfwl-web-manager.yml.xinfei-tms.yaml
        ├── xfwl-web-driver.yml.xinfei-tms.yaml
        ├── xfwl-web-courier.yml.xinfei-tms.yaml
        └── xfwl-web-customer.yml.xinfei-tms.yaml
```

---

## 三、数据文件说明

### 3.1 MySQL 全库备份

| 项目 | 值 |
|---|---|
| 文件 | `data/mysql-all-databases.sql.gz` |
| 体积 | ~8.4 MB |
| 内容 | `xinfei_tms` 库（55 张表）+ `xxl_job` 库（8 张表） |
| 导出方式 | `mysqldump --single-transaction --routines --triggers --events` |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci |

**包含的数据**：
- 系统管理（用户/角色/菜单/权限/字典）
- 基础数据（车辆/司机/线路/车队/区划）
- 订单/运单/作业
- 智能调度
- 轨迹 IoT
- 演示种子数据

### 3.2 Redis 数据

| 项目 | 值 |
|---|---|
| 文件 | `data/redis-data.tar.gz` |
| 体积 | ~210 KB |
| 内容 | `/data/redis` 目录全量（AOF + RDB） |

> **注意**：Redis 是缓存，不导入也不影响系统运行（会自动重建）。
> 导入是为了恢复演示数据和登录会话。

### 3.3 Nacos 配置

两种还原方式，**二选一**：

**方式 A：yaml 文件逐条导入**（推荐，更灵活）
- 目录：`data/nacos-configs/`
- 14 条配置，group 统一为 `xinfei-tms`
- 命名空间：`xinfei`（ID 见下方说明）

**方式 B：Derby 库直接还原**（快速）
- 文件：`data/nacos-data.tar.gz`
- 直接覆盖 `/data/nacos` 目录后重启 Nacos

**关键参数**：
```
命名空间名：xinfei
命名空间 ID：1cb93ce4-dc0e-4730-b759-d35fd7ed93c3
配置分组：xinfei-tms
```

### 3.4 RabbitMQ 拓扑

| 项目 | 值 |
|---|---|
| 文件 | `data/rabbitmq-defs.json` |
| 体积 | ~2.8 KB |
| 内容 | 交换机 / 队列 / 绑定定义 |

**交换机**：
- `xinfei.domain.event.exchange`（topic，领域事件）
- `xinfei.gps.exchange`（topic，GPS 轨迹）

**队列**（6 个持久队列）：
- `xinfei.domain.event.queue.order.confirmed`
- `xinfei.domain.event.queue.order.delivered`
- `xinfei.domain.event.queue.pickup.completed`
- `xinfei.oms.queue.order.delivered`
- `xinfei.gps.queue`
- `xinfei.domain.event.queue.dead.letter`（死信队列）

---

## 四、快速还原（3 步）

> 前提：已按 `docker-compose.infra.yml` 启动好中间件容器，且数据目录挂载在 `/data/` 下。

### 第 1 步：导入 MySQL

```bash
cd docs/中间件和数据库/data/

# 解压并导入（自动建库建表 + 写入数据）
gunzip -c mysql-all-databases.sql.gz | docker exec -i mysql8 mysql -uroot -p123456

# 验证
docker exec mysql8 mysql -uroot -p123456 -e "SHOW DATABASES; USE xinfei_tms; SHOW TABLES;"
```

**预期结果**：
- `xinfei_tms` 库：55 张表
- `xxl_job` 库：8 张表

### 第 2 步：导入 Nacos 配置

```bash
# 方式 A：yaml 逐条导入（推荐）
# 先在 Nacos 控制台创建命名空间 xinfei，ID 设为 1cb93ce4-dc0e-4730-b759-d35fd7ed93c3
# 然后运行导入脚本（见部署文档）

# 方式 B：直接还原 Derby 库（快速）
tar xzf nacos-data.tar.gz -C /data
docker restart nacos
```

### 第 3 步：导入 RabbitMQ 拓扑

```bash
docker exec -i rabbitmq rabbitmqctl import_definitions - < rabbitmq-defs.json

# 验证
docker exec rabbitmq rabbitmqctl list_queues name messages
```

---

## 五、开发环境默认口令

> **仅用于开发/测试环境**，生产环境请务必修改！

| 服务 | 账号 | 口令 |
|---|---|---|
| MySQL | root | 123456 |
| Redis | - | 123456 |
| Nacos 控制台 | nacos | 123456 |
| Nacos 应用账号 | xinfei | 123456 |
| RabbitMQ 管理台 | xinfei | 123456 |
| XXL-Job 控制台 | admin | 123456 |
| 系统登录 | admin | 123456 |

---

## 六、完整部署指南

见同目录下的 `docs/Docker部署指南.md`，包含：
- Docker 安装
- 中间件启动
- 数据导入
- 应用启动
- 验证与排障
