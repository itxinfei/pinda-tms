# 品达TMS 开发测试环境手册

> **最新更新日期：2026-10-09**（中间件升级完成：MySQL 8.0 / Redis 7 / Nacos 2.3 / RabbitMQ 3.12）
> 环境类型：开发/测试环境（非生产）
> 维护人：（开发/测试环境由项目管理员维护）
> 最后更新：2026-10-07（维护人占位已收敛；账号口径与《需求文档》体系核对一致）
> 说明：本手册记录品达TMS 开发测试环境的服务器、中间件、应用系统**账号与凭据落点**，以及测试造数方案。均为开发测试用弱口令，严禁用于生产环境。
> 🔒 **凭据治理口径（2026-10-09）**：真实口令一律**不写入本仓库任何文件**（本仓库会推送到公开 Gitee）。正确落点见 §0；处置历史与待办见《凭据泄露处置方案.md》。

---

## 0. 凭据存放位置（不入库）

| 凭据 | 唯一真值位置 | 读取方式 |
|---|---|---|
| 中间件口令（MySQL root / Redis / RabbitMQ）+ Nacos 服务端鉴权变量 | 服务器 `/data/deploy/middleware/.env`（root 0600） | `docker compose --env-file` 自动注入；`deploy/middleware/docker-compose.infra.yml` 用 `${VAR:?...}` 强制取值，缺则报错。含 `MYSQL_ROOT_PASSWORD`、`REDIS_PASSWORD`、`RABBITMQ_DEFAULT_USER/PASS`、6 个 `NACOS_AUTH_*` |
| Nacos 客户端账号（微服务/CI 用） | 服务器 `/home/pdwl/pinda-build/deploy/apps/.env`（`NACOS_USERNAME` / `NACOS_PASSWORD` / `REDIS_PASSWORD`） | apps compose 的 `&nacos-env` / `&redis-env` 锚点注入 |
| Seata TC 连 Nacos 的凭据 | 服务器 `/opt/seata/seata-server-1.4.2/conf/registry.conf`（仓库外，非文件托管） | `registry.nacos.username/password` + `config.nacos.username/password` 两处都要；缺则 TC 注册 403 → 崩溃循环 |
| Gitea 管理员口令（部署状态页用） | 服务器 `/etc/pinda-gitea-cred`（root 0600） | `deploy/ci/deploy-info.sh` 读取 |
| JWT 签名密钥对（轮换后） | 服务器 `/data/pinda-jwt/` | 由 `/data/ci/deploy.env` 的 `PINDA_JWT_*_KEY_PATH` 指路，流程见《05-部署文档.md》§2.11 |
| SSH / sudo 口令 | 本机凭据管理器 / 你自己保管 | 不写进文档、脚本、remote URL |
| 键名模板（无值） | `deploy/middleware/.env.example` | 新环境 `cp .env.example .env` 后填值 |

> ⚠️ 反例（已于 2026-10-09 修复）：29 份 `bootstrap-*.yml` 里 `password: ${NACOS_PASSWORD:pinda}` 的默认值曾是实际生效值，因为 `deploy/apps/docker-compose.app.yml` 的 `&nacos-env` 只注入 `NACOS_IP/PORT/ID/SERVER_ADDR`、不设 `NACOS_PASSWORD`。现在 `&nacos-env` 已注入 `NACOS_USERNAME` / `NACOS_PASSWORD`，真值取服务器 `deploy/apps/.env`。另本机 `.git/config` 那条带明文口令的 gitea remote 仍待处理。

> 🔑 **口令取值口径（2026-10-09 决策）**：这台内网开发测试机上，中间件口令**统一为 `123456`**，方便开发测试不必维护"哪个库用哪个口令"的对照表；安全性由内网边界保证。2026-10-09 一度给 Redis/Nacos 配过 24/20 位随机强口令，按本决策已回退。**生产环境不适用本口径，上线必须逐套更换。**

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
| SSH 登录 | 192.168.20.130:22 | pdwl | 不入库——存于本机凭据文件，见 §0 |
| sudo 提权 | （同 SSH） | pdwl | 不入库——同 SSH |

> 所有 docker 命令需 sudo：`sudo -S docker ...`（口令从 stdin/交互输入，**不要把 `echo '口令' |` 写进文档或脚本**）。

---

## 3. 中间件账号密码总表（Docker 容器）

| 组件 | 容器名 | 访问地址 | 账号 | 密码 | 备注 |
|---|---|---|---|---|---|
| MySQL 8.0 | mysql8 | 192.168.20.130:3306 | root | **123456** | 业务库见 §3.1 |
| Redis 7 | redis | 192.168.20.130:6379 | （无用户名） | **123456** | `requirepass` 已启用；验证码/缓存/j2cache L2 |
| Nacos 2.3.2 | nacos | 192.168.20.130:8848 | nacos（超管）/ pinda（业务） | **123456** / **123456** | 服务端鉴权已开启；pinda 绑角色 `ns-pinda-tms`，对命名空间 pinda 有 read+write |
| RabbitMQ 3.12 | rabbitmq | 192.168.20.130:15672（管理台） | pinda | **123456** | tags: administrator；口令用 `rabbitmqctl change_password` 维护，`RABBITMQ_DEFAULT_PASS` 只在用户不存在时生效 |
| Gitea 1.21 | gitea | 192.168.20.130:3000 | pinda | 见内部凭据（不入库） | SSH 端口 2222 |
| Seata | （宿主 systemd） | 8091 | （无） | 无认证 | 非容器，/opt/seata |

> ✅ **2026-10-09 中间件升级完成**：MySQL 5.7→8.0、Redis 5→7、Nacos 1.4→2.3、RabbitMQ 3.8→3.12） |
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
> 🟢 这里的 `123456` 是**应用内业务测试账号口令**（由 `docs/sql/testdata.sql` 的 `MD5('123456')` 造出，可随时重置），与 §2 服务器登录、§3 中间件那两类**真实凭据**不同，**有意保留在文档里**——删掉会让重构时无法复现造数。

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
# 口令一律从机器上的 .env 取，不写在文档/脚本/命令行历史里
sudo docker cp /tmp/testdata.sql mysql8:/tmp/testdata.sql
sudo sh -c 'DB_PASS=$(sed -n "s/^MYSQL_ROOT_PASSWORD=//p" /data/deploy/middleware/.env) \
  && docker exec mysql8 mysql -uroot -p"$DB_PASS" -e "SOURCE /tmp/testdata.sql"'
```

> 注意：不要用 `docker exec -i mysql < /tmp/testdata.sql` 这种方式——宿主机文件重定向会劫持 sudo 密码输入，且容器内看不到宿主机文件。
> ⚠️ 容器名 2026-10-09 起是 `mysql8`（升级前为 `mysql57`）；旧文档/脚本里写死 `mysql57` 的地方会失败，复核入口：《07-项目现状基线_代码与服务器实测.md》。

造数脚本示例（管理员用户）：
```sql
-- 新增后台管理员（密码哈希为 MD5('123456')，需从 pd_auth_user 中已存在的账号复制哈希）
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

> 口令不进文档：以下 `sudo` 会交互式提示输密码；连续执行多条时先 `sudo -v` 缓存一次凭据即可。

```bash
# 容器状态
sudo docker ps -a

# 查看服务日志（如 auth）
sudo docker logs pd-auth-server --tail 100

# 重启服务
sudo docker restart pd-auth-server

# 全量部署（CI 产物）
cd /opt/pinda-tms/deploy/apps && sudo env TAG=<git-hash> docker compose -f docker-compose.app.yml up -d --remove-orphans

# Nacos 服务注册检查（期望 count:14）
curl "http://192.168.20.130:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=100&groupName=pinda-tms&namespaceId=1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"

# 磁盘清理
sudo docker image prune -f
sudo docker builder prune -f

# CI runner 日志
sudo journalctl -u act-runner --no-pager -n 50
```

---

## 8. 注意事项
1. 本文 §2/§3 是**服务器与中间件真实凭据**，按 §0 只记账号不记口令；§4/§6 的 `123456` 是**业务测试账号**。两类都严禁用于生产。
2. 服务器时区为 UTC，查看日志时间注意 +8h 换算。
3. Gitea 远程地址**不要把凭据写进 URL**（本机 `.git/config` 现存一条带明文口令的 remote，待处理）：用 `http://192.168.20.130:3000/pinda/pinda-tms.git` + git credential helper。
4. 口令变更后同步更新：机器上 `/data/deploy/middleware/.env`、Nacos 中引用它的 dataId、`/etc/pinda-gitea-cred`，本文档只改"落点"不改动值。

