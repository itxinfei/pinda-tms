# Nacos 配置整理说明（同名配置是什么 & 怎么清理）

> **日期**：2026-10-06
> **触发**：用户反馈"自己都搞不清楚同名配置是干嘛的"
> **核查方式**：逐服务读 `bootstrap*.yml` 的 group/namespace + Nacos OpenAPI 全量拉取 + **17 条逐一 MD5 比对** + 全项目搜索 `DEFAULT_GROUP` 引用
> **结论一句话**：`pinda-tms` 组 17 条 = **全部在用**；`DEFAULT_GROUP` 组 17 条 = **改动前的旧快照，无人读取，可安全删除**

---

## 一、先讲清楚：为什么会有两份同名配置

Nacos 的配置由 **三层坐标**唯一确定：

```
命名空间(namespace) + 分组(group) + 配置(dataId)
```

- **命名空间** `1cb93ce4-dc0e-4730-b759-d35fd7ed93c3`（pinda）：两个组**都在这个命名空间下**，是同一批项目配置。
- **分组(group)**：`pinda-tms` 和 `DEFAULT_GROUP` 是**两个互不相干的分组**，Nacos 默认组就叫 `DEFAULT_GROUP`。
- **dataId**：文件名，如 `pd-oms-prod.yml`。

**历史成因**：项目早期可能先用默认组push过一批配置，后来才统一改到 `pinda-tms` 组，但默认组那份**没删**，于是形成"同名两份"。Nacos 控制台按 group 分页展示，不注意就会以为是"一个配置显示了两遍"。

---

## 二、决定性证据：没有任何服务读 `DEFAULT_GROUP`

### 2.1 代码层（最硬的证据）

全项目搜索 `DEFAULT_GROUP`（覆盖 12 个模块的 yml/properties/java/xml）：

```
✅ 0 处引用
```

**所有服务都显式写了 `group: pinda-tms`**，例如：

```yaml
# pd-oms/src/main/resources/bootstrap-prod.yml
spring:
  cloud:
    nacos:
      discovery:
        group: pinda-tms              # ← 显式指定
        namespace: 1cb93ce4-dc0e-4730-b759-d35fd7ed93c3
      config:
        group: pinda-tms              # ← 显式指定
        file-extension: yml
```

> 关键点：**因为显式指定了 group，Nacos 就绝不会去读 `DEFAULT_GROUP`**。若不写 group 才会 fallback 到默认组——本项目全部显式写了，所以默认组是死配置。

### 2.2 数据层（17 条逐一 MD5 比对）

| 结果 | 数量 | 说明 |
|---|---|---|
| ✅ 两组**完全相同** | 14 | 纯冗余副本 |
| ⚠️ 两组**有差异** | **3** | `pd-gateway-test.yml`、`mysql.yml`、`pd-auth-server-test.yml` |

**有差异的 3 条恰恰证明 `DEFAULT_GROUP` 是"旧快照"**——它保留的是**修改前**的状态：

| 配置 | `pinda-tms`（现行·生效） | `DEFAULT_GROUP`（旧·失效） |
|---|---|---|
| `pd-gateway-test.yml` | `context-path: /api`、有 `authentication.user.header-name: token`、ignore 规则 `[/anno, /captcha]` | `context-path: ""`（空）、**无 authentication 段**、ignore 规则 `[/api/authority/anno]` |
| `pd-auth-server-test.yml` | `expire: 7200`、**有 `spring.redis` 192.168.20.130:6379**、`context-path` 未设 | `context-path: /authority`、**无 redis 段** |
| `mysql.yml` | 含完整 `spring.datasource.druid`（各服务库连接） | 仅头部注释，**数据源配置已迁到各服务自己的 dataId** |

> 换句话说：`DEFAULT_GROUP` 那份停留在"网关还没配 context-path、auth 还没配 Redis、数据源还在共享配置里"的历史阶段。**它记录的正是我们今天修掉的那几个问题。**

---

## 三、`pinda-tms` 组的 17 条配置是干什么的（这是你要的"整理"）

**结构 = 3 条共享 + 14 条服务专属**，一条不多一条不少：

### 3.1 共享配置（3 条，被多个服务引用）

| dataId | 作用 | 谁在用 |
|---|---|---|
| `common.yml` | Jackson 时区/日期格式、文件上传大小限制等公共项 | 全部服务 |
| `redis.yml` | Redis 连接与缓存参数 | 全部需要缓存的服务 |
| `mysql.yml` | MySQL 数据源与 Druid 连接池、MyBatis 类型别名 | 全部需要 DB 的服务 |

> 注意：authority 体系（网关 / auth-server）通过 `shared-dataids: common.yml,redis.yml,mysql.yml` 引用这三个（该写法在 Spring Cloud Alibaba 2.2.1 已失效，**实际不加载**，这也是 Redis 配置缺失的根因之一，详见《联调就绪检查清单》§2 前置①）。

### 3.2 服务专属配置（14 条，一服务一档）

命名规则：`{服务名}-{激活的profile}.yml`

| # | dataId | 对应服务 | 激活 profile | 主要内容 |
|---|---|---|---|---|
| 1 | `pd-oms-prod.yml` | pd-oms | prod | 订单/12级状态机/Drools 运费 |
| 2 | `pd-work-dev.yml` | pd-work | **dev** | 运单/运输任务闭环（⚠️ dev 而非 prod） |
| 3 | `pd-base-prod.yml` | pd-base | prod | 基础数据（车辆/司机/货物/网点） |
| 4 | `pd-netty-prod.yml` | pd-netty | prod | 轨迹接入 Netty/Kafka |
| 5 | `pd-user-prod.yml` | pd-user | prod | C 端用户 |
| 6 | `pd-dispatch-prod.yml` | pd-dispatch | prod | 调度/Quartz/VRP |
| 7 | `pd-aggregation-prod.yml` | pd-aggregation | prod | 聚合层 |
| 8 | `pd-druid-prod.yml` | pd-druid | prod | Druid 监控 |
| 9 | `pd-web-manager-dev.yml` | pd-web-manager | **dev** | 管理端后端（⚠️ dev 而非 prod） |
| 10 | `pd-web-driver-prod.yml` | pd-web-driver | prod | 司机端后端 |
| 11 | `pd-web-courier-prod.yml` | pd-web-courier | prod | 快递员端后端 |
| 12 | `pd-web-customer-prod.yml` | pd-web-customer | prod | 客户端后端 |
| 13 | `pd-gateway-test.yml` | pd-gateway | **test** | 网关路由/鉴权/Redis（今天改过） |
| 14 | `pd-auth-server-test.yml` | pd-auth-server | **test** | 登录/JWT 7200s/Redis（今天改过） |

> ⚠️ **profile 混用提示**（非故障，但值得知道）：14 个服务里 **11 个用 prod**、**2 个用 dev**（pd-work、pd-web-manager）、**2 个用 test**（网关、auth-server，authority 体系独立版本线）。所以 dataId 后缀不统一是**正常的历史现状**，不是配置错误。

### 3.3 另有一个非本项目配置

| dataId | group | 说明 |
|---|---|---|
| `service.vgroupMapping.pinda-tms-group` | `SEATA_GROUP` | Seata 事务分组映射（Seata 1.2 仅依赖未实际启用，遗留配置） |

---

## 四、清理方案

### 4.0 ✅ 执行记录（2026-10-06 16:12 已执行）

```bash
python .workbuddy/clean_nacos_default_group.py --execute --include-different
```

**结果**：

| 项 | 清理前 | 清理后 |
|---|---|---|
| `pinda-tms`（在用） | 17 条 | **17 条**（未受影响） |
| `DEFAULT_GROUP`（死配置） | 17 条 | **0 条** ✅ |
| `SEATA_GROUP`（Seata 遗留） | 1 条 | 1 条（未触碰） |

- 删除 17 条，成功 17 / 失败 0；**含 3 条差异配置**（`mysql.yml`、`pd-auth-server-test.yml`、`pd-gateway-test.yml` 的旧快照）。
- **备份目录**：`.workbuddy/nacos-backup/20261006-161223-before-cleanup/`（17 条 + 网关修正前备份，共 18 文件）。
- **独立复核**：`pinda-tms` 组 17 条 dataId 逐一核对完整、网关 13 条路由完好、`server.port: 8760` / `context-path: /api` / `header-name: token` 均在位。

> 🎁 **意外收获**：清理过程中发现网关 j2cache 配置指向不存在的文件（见下），顺手修正后**解除了长期阻塞的网关 500 故障**。

### 4.1 后续建议动作

**删除 `DEFAULT_GROUP` 下的 17 条配置**。理由：
1. 代码层证明**零服务读取**；
2. 内容是**修改前的旧快照**，留着只会误导（有人可能改到它却不起作用）；
3. 14 条完全冗余，白占空间、干扰排查。

### 4.2 执行前必做

**备份**（虽然有 10-06 备份，但清理前再备一次更稳）：

```bash
mkdir -p .workbuddy/nacos-backup/20261006-before-cleanup
# 脚本会自动逐条导出 DEFAULT_GROUP 的 17 条
```

### 4.3 用脚本执行（推荐）
仓库已备好带预览与确认的脚本：

```bash
# 1) 先预览（只读，不删任何东西）
python .workbuddy/clean_nacos_default_group.py --dry-run

# 2) 确认清单无误后执行删除
python .workbuddy/clean_nacos_default_group.py --execute
```

脚本特点：
- 默认 dry-run，**必须显式加 `--execute` 才真删**；
- 执行前**自动重新比对**每条内容（若 `pinda-tms` 与 `DEFAULT_GROUP` 不一致会**跳过并警告**，防止误删有差异的配置）；
- 删除后**自动复核** `pinda-tms` 组仍为 17 条。

### 4.4 或手工在控制台删

Nacos 控制台 → 配置管理 → 左下角切换分组 `pinda-tms` → `DEFAULT_GROUP` → 全选删除。**切记先确认当前分组**，很容易在 `pinda-tms` 分组下误删真正在用的。

---

## 五、顺便回答：改配置后怎么保证不丢

| 建议 | 说明 |
|---|---|
| **改前备份** | 已形成惯例：`.workbuddy/nacos-backup/20261006/`（17 份 + 2 份 `.new`） |
| **改后立即验证** | 见下方验证清单 |
| **定期导出入 git** | 配置文件纳入版本管理，改动可追溯、可回滚 |
| **改 group/namespace 前先确认引用** | 本次就是靠"代码里 0 处引用 DEFAULT_GROUP"才敢判定可删 |

### 改动后的标准验证动作

```bash
NS="1cb93ce4-dc0e-4730-b759-d35fd7ed93c3"
# 1) 配置能读出来
curl -s "http://192.168.20.130:8848/nacos/v1/cs/configs?dataId=pd-gateway-test.yml&group=pinda-tms&tenant=$NS" | head -20

# 2) 配置数没变（应为 17）
curl -s "http://192.168.20.130:8848/nacos/v1/cs/configs?search=accurate&dataId=&group=pinda-tms&tenant=$NS&pageNo=1&pageSize=50" \
  | python -c "import sys,json;print(json.load(sys.stdin)['totalCount'])"

# 3) 功能真的生效（改网关配置后必做）
curl -s -o /dev/null -w "%{http_code}\n" "http://192.168.20.130:8760/api/authority/menu/router"   # 应 200 而非 500
```

⚠️ **改 Redis/连接池这类配置，必须重启进程才生效**（连接池启动时装配，Nacos 推送不重建）。这一点今天已在网关 500 上验证过。

---

## 六、一页速查表

| 问题 | 答案 |
|---|---|
| 两份同名配置哪份在用？ | **`pinda-tms` 组**。全部服务 `bootstrap*.yml` 显式写了 `group: pinda-tms` |
| `DEFAULT_GROUP` 那份是什么？ | 改动前的**旧快照**，14 条完全冗余 + 3 条过时（网关无 context-path、auth 无 Redis、mysql 数据源已迁走） |
| 能删吗？ | ✅ 能。全项目搜索 `DEFAULT_GROUP` = **0 处引用** |
| 怎么删？ | `python .workbuddy/clean_nacos_default_group.py --dry-run` 先预览，再 `--execute` |
| `text` 格式要改吗？ | ❌ 不用。只影响控制台语法高亮，不影响解析（详见《中间件配置持久化核查报告》） |
| 17 条配置够吗？ | 够。3 共享 + 14 服务专属，正好对应 14 个服务，无冗余无缺失 |
| 为什么 dataId 后缀 prod/dev/test 混用？ | 11 个 prod + 2 个 dev（pd-work、pd-web-manager）+ 2 个 test（网关、auth-server），是历史现状，非错误 |

---

> **建议先做**：执行 `--dry-run` 看一遍清单 → 确认后 `--execute` 清理默认组 → 然后重启网关解除 500 阻塞。
> 本次**未执行任何删除操作**，等你确认。
