# 品达TMS 中间件部署文档（Ubuntu 24.04）

本文档说明如何在一台 Ubuntu Server 24.04 (amd64) 虚拟机上部署品达TMS所需的全部中间件与数据库。中间件以 Docker 容器方式运行，Java 微服务在**宿主机**上运行，因此地址统一使用宿主机 IP，容器之间不依赖容器网络。

## 一、架构总览

### 1.1 中间件

| 中间件 | 推荐版本 | 端口 | 用途 | 是否必需 |
|---|---|---|---|---|
| MySQL | 5.7 | 3306 | 业务数据库（按微服务分库） | 必需 |
| Redis | 5.x | 6379 | J2Cache 二级缓存 | 必需 |
| Nacos | 1.4.1 | 8848 | 注册中心 + 配置中心 | 必需 |
| Kafka | 2.6（Scala 2.12） | 9092 | GPS 轨迹消息，topic：`tms_order_location`（pd-netty） | 必需 |
| RabbitMQ | 3.8（含 management） | 5672 / 15672 | 智能调度领域事件（pd-dispatch） | 必需 |
| Seata Server | 1.2.0 | 8091 | 分布式事务 TC（pd-dispatch 使用 `@GlobalTransactional`） | 可选* |

> \* 暂不需要分布式事务时可不部署，在 Nacos 配置中降级（见“第八节”）。

版本对应：Spring Boot `2.2.5` + Spring Cloud Alibaba `2.2.1`，内置 Nacos 客户端 1.x，故 Nacos 服务端用 **1.4.1**（勿用 2.x，否则需额外开放 gRPC 端口 9848）；Seata 由根 POM 指定 **1.2.0**。

### 1.2 微服务 → 数据库映射（重点）

本项目采用**按微服务分库**，共 7 个业务库：

| 微服务 (spring.application.name) | profile | 连接的库 | 端口 |
|---|---|---|---|
| pd-auth-server | test | `pd_auth` | 9000 |
| pd-base | prod | `pd_base` | 8185 |
| pd-oms | prod | `pd_oms` | 8186 |
| pd-work | dev | `pd_work` | 8187 |
| pd-user | prod | `pd_users` | 8189 |
| pd-dispatch | prod | `pd_dispatch` | 8190 |
| pd-aggregation | prod | `pd_aggregation` | 8191 |
| pd-netty | prod | `pd_oms`（存 `pd_truck_location` 轨迹表） | 8192 |
| pd-gateway | test | —（网关） | 8760 |
| pd-web-manager / driver / courier / customer | dev / prod | —（无数据源，走 Feign） | 8161~8164 |

## 二、统一规划

```bash
export HOST=192.168.20.130      # 宿主机 IP（按实际修改）
export DB_PASS=123456           # MySQL root 密码

# 以下两个值在各服务 bootstrap-<profile>.yml 中写死，请勿更改
export NS=1cb93ce4-dc0e-4730-b759-d35fd7ed93c3   # Nacos 命名空间 ID
export GROUP=pinda-tms                           # Nacos 分组
```

- 数据持久化目录：`/data/mysql`、`/data/redis`、`/data/nacos`、`/data/seata`。
- 启动顺序：Docker → MySQL（建库导数据）→ Redis → Nacos（建命名空间、推送配置）→ Kafka → RabbitMQ → Seata。

## 三、安装 Docker

```bash
# 1) 前置依赖
sudo apt-get update
sudo apt-get install -y ca-certificates curl gnupg

# 2) Docker 官方 GPG 密钥
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc >/dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc

# 3) 添加 APT 源（自动识别代号 noble）
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null

# 4) 安装（国内访问慢可把 download.docker.com 换成 mirrors.aliyun.com/docker-ce）
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 5) 镜像加速
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json >/dev/null <<'EOF'
{ "registry-mirrors": ["https://docker.m.daocloud.io"] }
EOF

# 6) 启动并开机自启
sudo systemctl enable --now docker
docker --version

# 7) 当前用户免 sudo（执行后重新登录，或 newgrp docker 生效）
sudo usermod -aG docker $USER

# 持久化目录
sudo mkdir -p /data/mysql /data/redis /data/nacos /data/seata
```

## 四、部署 MySQL 5.7

### 4.1 启动容器

```bash
docker run -d --name mysql57 \
  -p 3306:3306 \
  -v /data/mysql:/var/lib/mysql \
  -e MYSQL_ROOT_PASSWORD=${DB_PASS} \
  --restart=unless-stopped \
  mysql:5.7 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_general_ci

# 等待就绪
for i in $(seq 1 30); do
  docker exec mysql57 mysqladmin ping -uroot -p${DB_PASS} &>/dev/null && break
  sleep 2
done
```

### 4.2 创建 7 个业务库

```bash
docker exec -i mysql57 mysql -uroot -p${DB_PASS} <<'SQL'
CREATE DATABASE IF NOT EXISTS pd_auth        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_base        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_oms         DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_work        DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_users       DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_dispatch    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS pd_aggregation DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
SHOW DATABASES;
SQL
```

### 4.3 导入数据

把项目文件放到 Ubuntu（git clone 或 scp），假设项目根目录为 `/opt/pinda-tms`。SQL 脚本都不含 `CREATE DATABASE`，必须指定目标库导入。

```bash
cd /opt/pinda-tms
M="docker exec -i mysql57 mysql -uroot -p${DB_PASS} --default-character-set=utf8mb4"

# (1) 6 个微服务基础建库脚本（目录名含中文，注意引号）
DBS="mysql/TMS项目建库脚本"
for db in pd_base pd_oms pd_work pd_users pd_dispatch pd_aggregation; do
  echo "import $db"
  $M "$db" < "$DBS/$db.sql"
done

# (2) 权限库：表 + 初始数据（管理员账号、菜单、资源、角色）
$M pd_auth < pd-authority/pd_auth.sql

# (3) 货物信息（演示数据，无代码强引用，归入 pd_base）
$M pd_base < pd-authority/pd_goods_info.sql

# (4) GPS 轨迹表 pd_truck_location，建在 pd_oms（pd-netty 与 pd-oms 共用）
$M pd_oms < docs/sql/V1.1__create_truck_location_table.sql
```

### 4.4 Seata AT 回滚表

参与分布式事务的库需建 `undo_log`（本项目主要 pd-dispatch）：

```bash
docker exec -i mysql57 mysql -uroot -p${DB_PASS} pd_dispatch <<'SQL'
CREATE TABLE IF NOT EXISTS `undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB COMMENT='AT undo table';
SQL
```

验证：

```bash
docker exec mysql57 mysql -uroot -p${DB_PASS} -e "
SELECT table_schema, COUNT(*) FROM information_schema.tables
WHERE table_schema LIKE 'pd_%' GROUP BY table_schema;"
```

预期各库表数：pd_auth=11、pd_base=18、pd_oms=5、pd_work=5、pd_users=2、pd_dispatch=20、pd_aggregation=12。

## 五、部署 Redis 5

```bash
docker run -d --name redis \
  -p 6379:6379 \
  -v /data/redis:/data \
  --restart=unless-stopped \
  redis:5 redis-server --appendonly yes

docker exec redis redis-cli ping     # PONG
```

## 六、部署 Nacos 1.4.1

### 6.1 启动容器（单机，内置 Derby）

```bash
docker run -d --name nacos \
  -p 8848:8848 \
  -v /data/nacos:/home/nacos/logs \
  -e MODE=standalone \
  -e JVM_XMS=256m -e JVM_XMX=256m -e JVM_XMN=128m \
  --restart=unless-stopped \
  nacos/nacos-server:1.4.1

for i in $(seq 1 40); do
  curl -s "http://${HOST}:8848/nacos/v1/ns/operator/metrics" | grep -q '"status' && break
  sleep 2
done
```

### 6.2 创建命名空间（必须使用固定 ID）

服务的 `bootstrap-<profile>.yml` 写死了命名空间，因此要用该固定 ID 创建：

```bash
curl -s -X POST "http://${HOST}:8848/nacos/v1/console/namespaces" \
  --data-urlencode "customNamespaceId=${NS}" \
  --data-urlencode "namespaceName=pinda" \
  --data-urlencode "namespaceDesc=tms"; echo
```

控制台 `http://192.168.20.130:8848/nacos`（nacos/nacos），“命名空间”中应出现 `pinda`。

### 6.3 一键推送全部配置

仓库脚本 `scripts/push-nacos-config.sh` 已按多库配置好（命名空间、分组、各服务 dataId 与数据源 URL）。Nacos 就绪后在**项目根目录**执行：

```bash
cd /opt/pinda-tms
bash scripts/push-nacos-config.sh
```

脚本会推送：共享配置 `common.yml/redis.yml/mysql.yml`、各微服务 `<应用名>-<profile>.yml`（数据源指向对应库）、网关 `pd-gateway-test.yml`。每个文件返回 `[OK]` 即成功。

> 配置三要素（脚本与服务必须一致）：命名空间 `1cb93ce4…`、分组 `pinda-tms`、dataId `<应用名>-<profile>.yml`。

## 七、部署 Kafka 与 RabbitMQ

### 7.1 Kafka 2.6

```bash
docker run -d --name zookeeper -p 2181:218 --restart=unless-stopped wurstmeister/zookeeper:3.4.6

docker run -d --name kafka -p 9092:9092 \
  -e KAFKA_BROKER_ID=0 \
  -e KAFKA_ZOOKEEPER_CONNECT=${HOST}:2181 \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://${HOST}:9092 \
  -e KAFKA_AUTO_CREATE_TOPICS_ENABLE=true \
  --restart=unless-stopped \
  wurstmeister/kafka:2.12-2.6.0

docker exec kafka kafka-topics.sh --create --if-not-exists \
  --zookeeper ${HOST}:2181 --replication-factor 1 --partitions 3 \
  --topic tms_order_location
```

> `KAFKA_ADVERTISED_LISTENERS` 必须是宿主机 IP，否则客户端拿到容器内地址会连不上。

### 7.2 RabbitMQ 3.8

```bash
docker run -d --name rabbitmq \
  -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=pinda \
  -e RABBITMQ_DEFAULT_PASS=pinda123 \
  --restart=unless-stopped \
  rabbitmq:3.8-management
```

管理界面 `http://192.168.20.130:15672`（pinda/pinda123）。交换机/队列由 pd-dispatch 启动时自动声明，无需手工创建。

## 八、部署 Seata Server 1.2.0（可选）

```bash
sudo mkdir -p /data/seata/config
cat > /data/seata/config/registry.conf <<EOF
registry { type = "nacos"
  nacos { serverAddr = "${HOST}:8848" namespace = "${NS}" cluster = "default" group = "SEATA_GROUP" } }
config { type = "nacos"
  nacos { serverAddr = "${HOST}:8848" namespace = "${NS}" group = "SEATA_GROUP" } }
EOF
```

Nacos 中新建配置：命名空间 pinda，Group=`SEATA_GROUP`，Data ID=`service.vgroupMapping.pinda-tms-group`，内容 `default`。

```bash
docker run -d --name seata-server -p 8091:8091 \
  -v /data/seata/config:/seata-server/resources \
  --restart=unless-stopped seataio/seata-server:1.2.0
```

> 不用 Seata 时：在各服务配置中设置 `seata.enabled=false`、`seata.service.enable-degrade=true`，可先不启动。

## 九、防火墙（ufw）

Ubuntu 的 ufw 默认未启用，同网段通常无需配置：

```bash
sudo ufw status     # inactive 可跳过
```

若已启用：

```bash
sudo ufw allow proto tcp from any to any port 3306,6379,8848,9092,5672,15672,8091
sudo ufw reload
```

> Docker 发布的端口由 Docker 直接写 iptables，ufw 对其外部访问限制可能不生效。

## 十、部署结果核对清单

| 检查项 | 命令 / 地址 | 期望结果 |
|---|---|---|
| MySQL 多库 | `docker exec mysql57 mysql -uroot -p${DB_PASS} -e "SELECT table_schema,COUNT(*) FROM information_schema.tables WHERE table_schema LIKE 'pd_%' GROUP BY table_schema;"` | 7 个库，表数 11/18/5/5/2/20/12 |
| 权限账号 | `docker exec mysql57 mysql -uroot -p${DB_PASS} -e "SELECT account FROM pd_auth.pd_auth_user;"` | pinda、test 等 5 个账号 |
| Redis | `docker exec redis redis-cli ping` | PONG |
| Nacos | `http://192.168.20.130:8848/nacos` | 可登录，含 pinda 命名空间 |
| 配置 | Nacos 配置列表（命名空间 pinda / 分组 pinda-tms） | 13 个 dataId 全部存在 |
| Kafka | `docker exec kafka kafka-topics.sh --zookeeper ${HOST}:2181 --list` | tms_order_location |
| RabbitMQ | `http://192.168.20.130:15672` | 可登录 |
| Seata | Nacos 服务列表 | seata-server |

## 十一、启动微服务时的注意事项

各服务 `bootstrap-<profile>.yml` 默认 Nacos 账号为 `pinda/pinda`，但 Nacos 默认账号是 `nacos/nacos`。启动前导出环境变量统一（无需在 Nacos 额外建用户）：

```bash
export NACOS_USERNAME=nacos NACOS_PASSWORD=nacos
# Nacos 地址已在配置中默认指向 192.168.20.130，如需改：
# export NACOS_SERVER_ADDR=192.168.20.130:8848
```

## 十二、常用运维命令

```bash
docker ps
docker logs -f nacos
docker restart mysql57
docker stop mysql57 redis nacos kafka zookeeper rabbitmq seata-server
docker rm -f mysql57 redis nacos kafka zookeeper rabbitmq seata-server   # 数据仍在 /data
```

## 十三、常见问题

| 现象 | 原因与处理 |
|---|---|
| `docker pull` 超时 / TLS handshake | 更换 `/etc/docker/daemon.json` 加速器后 `sudo systemctl restart docker` |
| `permission denied ... docker.sock` | `newgrp docker` / 重新登录，或临时加 sudo |
| 服务报 `NacosException / wait server ready` | Nacos 未就绪；确认 `NACOS_SERVER_ADDR` 与 8848 可通 |
| `get config from nacos error` / 空配置 | 命名空间/分组/dataId 未对齐；重新执行 `scripts/push-nacos-config.sh` |
| 报表不存在 (`Table ... doesn't exist`) | 服务连错库；核对“微服务→库映射”，数据源 URL 库名是否正确 |
| Kafka 连不上、超时 | `KAFKA_ADVERTISED_LISTENERS` 必须是宿主机 IP |
| MyBatis `Invalid bound statement` | mapper-locations：业务服务为 `/mapper/**`，pd-auth 为 `/mapper_authority/**` |
| RabbitMQ 资源/账号失败 | 管理界面确认 pinda/pinda123；队列由 pd-dispatch 自动声明 |
| Seata 事务失败 | 确认 Nacos 有 seata-server、vgroupMapping=default、pd_dispatch 已建 undo_log |
