# 品达TMS 开发测试环境手册

> 环境类型：开发/测试环境（非生产）
> 维护人：（开发/测试环境由项目管理员维护；密码统一 123456，详见本文 §账号）
> 最后更新：2026-10-07（维护人占位已收敛；账号口径与《需求文档》体系核对一致）
> 说明：本手册集中记录品达TMS 开发测试环境的服务器、中间件、应用系统账号密码与测试造数方案。密码均为开发测试用弱密码，严禁用于生产环境。

---

## 1. 环境总览

| 项目 | 值 |
|---|---|
| 服务器 IP | 192.168.20.130 |
| 操作系统 | Ubuntu（LVM 48G） |
| 部署方式 | Docker 容器（中间件 + 微服务 + 前端） |
| 代码仓库 | Gitea（192.168.20.130:3000）→ 本地 D:\MyCode\pinda-tms 为权威 |
| CI/CD | Gitea Actions（act-runner 宿主二进制，push master 自动构建部署） |
| 时区注意 | **服务器系统时区为 UTC**，比北京时间慢 8 小时（日志时间注意换算） |

---

## 2. 服务器登录

| 用途 | 地址 | 账号 | 密码 |
|---|---|---|---|
| SSH 登录 | 192.168.20.130:22 | pdwl | 123456 |
| sudo 提权 | （同 SSH） | pdwl | 123456 |

> 所有 docker 命令需 sudo：`echo '123456' | sudo -S docker ...`

---

## 3. 中间件账号密码总表（Docker 容器）

| 组件 | 容器名 | 访问地址 | 账号 | 密码 | 备注 |
|---|---|---|---|---|---|
| MySQL 5.7 | mysql57 | 192.168.20.130:3306 | root | 123456 | 业务库见 §3.1 |
| Redis 5 | redis | 192.168.20.130:6379 | （无） | 无密码 | 验证码/缓存/j2cache L2 |
| Nacos 1.4.1 | nacos | 192.168.20.130:8848 | pinda | pinda | 控制台登录 |
| RabbitMQ | rabbitmq | 192.168.20.130:15672（管理台） | pinda | 见机器上 /data/deploy/middleware/.env（不入库） | tags: administrator |
| Kafka | kafka | 192.168.20.130:9092 | （无） | 无认证 | 依赖 zookeeper:2181 |
| Gitea 1.21 | gitea | 192.168.20.130:3000 | pinda | 见内部凭据（不入库） | SSH 端口 2222 |
| Seata | （宿主 systemd） | 8091 | （无） | 无认证 | 非容器，/opt/seata |

### 3.1 Nacos 关键配置项

| 配置项 | 值 |
|---|---|
| 命名空间 | pinda（ID: `1cb93ce4-dc0e-4730-b759-d35fd7ed93c3`） |
| 配置分组 | pinda-tms |
| 关键 dataId | redis.yml、common.yml、mysql.yml |
| 配置查询 | `curl "http://192.168.20.130:8848/nacos/v1/cs/configs?dataId=redis.yml&group=pinda-tms&tenant=1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"` |

### 3.2 MySQL 业务数据库

| 数据库 | 所属服务 |
|---|---|
| pd_auth | 权限/认证（含 pd_auth_user 用户表） |
| pd_base | 基础数据（机构/网点/司机等） |
| pd_oms | 订单 |
| pd_work | 运输任务 |
| pd_users | 用户/会员 |
| pd_dispatch | 调度 |
| pd_aggregation | 聚合服务 |
| pinda_tms | （历史/初始化） |

---

## 4. 应用系统账号

### 4.1 后台管理系统（pd-admin-ui）

| 入口 | 账号 | 密码 | 角色 |
|---|---|---|---|
| http://192.168.20.130:8080/#/login | pinda | 123456 | 平台管理员（超级管理员，**已验证可登录**） |
| 同上 | admin01 / admin02 | 123456 | 测试管理员（**已验证可登录**） |

> 验证方式：`curl "http://192.168.20.130:8760/api/authority/anno/token?account=pinda&password=123456"` 返回 code:0 + token 即成功。

### 4.2 全部账号清单（2026-10-06 已统一重置密码为 123456）

> 登录密码校验为 MD5，重置命令：`UPDATE pd_auth.pd_auth_user SET password = MD5('123456') WHERE account <> 'pinda';`
> 造数脚本 `docs/sql/testdata.sql`（幂等可重跑，已导入服务器）。

| account | name | 用途/端 | 状态 |
|---|---|---|---|
| pinda | 平台管理员 | 管理端（PT_ADMIN） | ✅ 已验证登录 |
| admin01 / admin02 | 测试管理员01/02 | 管理端（PT_ADMIN） | ✅ 已验证登录 |
| driver01 ~ driver03 | 测试司机01~03 | 司机端（BASE_USER + 司机档案） | ✅ driver01 已验证 |
| courier01 / courier02 | 测试快递员01/02 | 快递员端（BASE_USER + 派送范围） | ✅ courier01 已验证 |
| test | 赵六 | 普通用户（DEPT_MANAGER） | ✅ 已验证登录 |
| manong / 11111 / test1 | （历史测试用户） | 普通用户 | ✅ 密码已重置 |

---

## 5. 微服务端口总表（容器绑定）

| 服务 | 容器名 | 端口 | 服务 | 容器名 | 端口 |
|---|---|---|---|---|---|
| 认证 | pd-auth-server | 9000 | 网关 | pd-gateway | 8760 |
| 基础数据 | pd-base | 8185 | 管理端 | pd-web-manager | 8161 |
| 订单 | pd-oms | 8186 | 司机端 | pd-web-driver | 8162 |
| 运输任务 | pd-work | 8187 | 快递员端 | pd-web-courier | 8163 |
| 用户/会员 | pd-user | 8189 | 客户小程序 | pd-web-customer | 8164 |
| 调度 | pd-dispatch | 8190 | 前端管理台 | pd-admin-ui | 8080 → 80 |
| 聚合 | pd-aggregation | 8191 | 数据大屏 | pd-druid | 8193 |
| netty | pd-netty | 8192 | | | |

> 网关统一入口：`http://192.168.20.130:8760/api/<服务>/...`（如验证码 `GET /api/authority/anno/captcha?key=xxx`）

---

## 6. 测试数据造数方案（≥10 条，验证系统链路）

### 6.1 目的
打通「前端页面 → 网关 → 微服务 → MySQL/Redis」全链路，每个端（管理/司机/快递员/客户）都能用真实数据操作。

### 6.2 造数清单（2026-10-06 已全部导入服务器并验证登录）

| # | 数据 | 数量 | 写入位置 | 状态 |
|---|---|---|---|---|
| 1 | 后台管理员账号 | 2（admin01/02） | pd_auth.pd_auth_user + user_role | ✅ 已验证登录 |
| 2 | 司机账号 + 档案 | 3（driver01~03） | pd_auth.pd_auth_user + pd_base.pd_truck_driver | ✅ driver01 已验证登录 |
| 3 | 快递员账号 + 派送范围 | 2（courier01/02） | pd_auth.pd_auth_user + pd_base.pd_courier_scop | ✅ courier01 已验证登录 |
| 4 | 客户/会员 | 3（M9301~03） | pd_users.pd_member | ✅ 已导入 |
| 5 | 订单 | 3（PJ0001~03） | pd_oms.pd_order | ✅ 已导入 |
| 6 | 运单 | 3（TY0001~03） | pd_work.pd_transport_order | ✅ 已导入 |
| 7 | 运输任务 | 2（RW0001/02） | pd_work.pd_task_transport | ✅ 已导入 |

> 合计 12 账号 + 16 业务数据，覆盖全部 4 端登录 + 核心业务表。

### 6.3 造数方式（脚本已就绪，幂等可重跑）

```bash
# 本地脚本: docs/sql/testdata.sql
# 服务器执行方式（关键: 先用 docker cp 把文件拷进容器, 再 SOURCE）
echo '123456' | sudo -S docker cp /tmp/testdata.sql mysql57:/tmp/testdata.sql
echo '123456' | sudo -S docker exec mysql57 mysql -uroot -p123456 -e "SOURCE /tmp/testdata.sql"
```

> 注意：不要用 `docker exec -i mysql < /tmp/testdata.sql` 这种方式——宿主机文件重定向会劫持 sudo 密码输入，且容器内看不到宿主机文件。

造数脚本示例（管理员用户）：
```sql
-- 新增后台管理员（密码 BCrypt 为 123456，需从 pd_auth_user 中已存在的账号复制哈希）
INSERT INTO pd_auth.pd_auth_user (id, account, name, password, status)
VALUES (641577229343600001, 'testadmin01', '测试管理员01',
        (SELECT password FROM pd_auth.pd_auth_user WHERE account='pinda'), 1);
```

### 6.4 链路验证步骤
1. 管理端：`http://192.168.20.130:8080/#/login` 登录 pinda/123456 → 查看机构/网点/订单列表
2. 司机端：`http://192.168.20.130:8081/#/login`（或对应入口）用司机账号登录
3. 快递员端 / 客户小程序：同上
4. 数据库确认：`SELECT COUNT(*) FROM pd_oms.订单表;` 与页面显示一致
5. Redis 确认：验证码/缓存正常（`docker exec redis redis-cli -n 0 KEYS '*'`）

---

## 7. 常用运维命令速查

```bash
# 容器状态
echo '123456' | sudo -S docker ps -a

# 查看服务日志（如 auth）
echo '123456' | sudo -S docker logs pd-auth-server --tail 100

# 重启服务
echo '123456' | sudo -S docker restart pd-auth-server

# 全量部署（CI 产物）
cd /opt/pinda-tms/deploy/apps && echo '123456' | sudo -S env TAG=<git-hash> docker compose -f docker-compose.app.yml up -d --remove-orphans

# Nacos 服务注册检查（期望 count:14）
curl "http://192.168.20.130:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=100&groupName=pinda-tms&namespaceId=1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"

# 磁盘清理
echo '123456' | sudo -S docker image prune -f
echo '123456' | sudo -S docker builder prune -f

# CI runner 日志
echo '123456' | sudo -S journalctl -u act-runner --no-pager -n 50
```

---

## 8. 注意事项
1. 以上全部为开发测试环境凭据，**请勿用于生产**。
2. 服务器时区为 UTC，查看日志时间注意 +8h 换算。
3. Gitea 仓库远程地址（带凭据）：`http://<用户>:<口令>.168.20.130:3000/pinda/pinda-tms.git`
4. 密码修改后请同步更新 Nacos 配置与本文档。
