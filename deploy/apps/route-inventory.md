# 网关路由清单（T-SEC-03 影子 API 防控）

> 版本：v1.0 | 日期：2026-10-16 | 真源：`xf-gateway/src/main/resources/application.yml` routes 段
> 用途：《71-生产发布安全P0实施任务卡》T-SEC-03（A20 路由核对）——固化"路由清单 = 实际服务"的发布检查项。
> **每次发布前核对一次**：本清单与网关 yml 是否一致、清单与实际部署服务是否一致；发现清单外路由（影子 API）立即下线并登记《99》台账。

## 1. 路由清单（8 条）

| # | 路由 id | 匹配路径 | 目标服务 | 前缀剥离 | 说明 |
|---|---|---|---|---|---|
| 1 | xf-auth | `/api/auth/**` | lb://xf-auth (9000) | StripPrefix=2 | 认证服务；`/api/auth/anno/**` 为匿名白名单（登录/验证码，T-SEC-01 按 IP 严限） |
| 2 | xf-auth-v1 | `/api/v1/**` | lb://xf-auth (9000) | StripPrefix=2 | 系统管理 youlai 契约（controller.v1 包）。**⚠️ 当前 404 属 SYS 任务预留，勿当 bug 修**（见《71》附 B），清单标注"预留，待 SYS-01/02" |
| 3 | xf-web-manager | `/api/manager/**` | lb://xf-web-manager (8161) | StripPrefix=2 | 管理端 BFF |
| 4 | xf-web-driver | `/api/web-driver/**` | lb://xf-web-driver (8162) | StripPrefix=2 | 司机端 BFF（前端 H5 经容器 nginx rewrite /prod-api/web-* → /api/web-*） |
| 5 | xf-web-courier | `/api/web-courier/**` | lb://xf-web-courier (8163) | StripPrefix=2 | 快递员端 BFF |
| 6 | xf-web-customer | `/api/web-customer/**` | lb://xf-web-customer (8164) | StripPrefix=2 | 客户端 BFF |
| 7 | xf-netty | `/api/netty-service/**` | lb://xf-netty (8185) | StripPrefix=2 | 轨迹/定位服务 |
| 8 | xf-file-server | `/api/file/**` | lb://xf-file-server (9999) | StripPrefix=2 | 文件服务；`/api/file/view/**` 匿名放行（img src 无法带 token，URL 末段随机 uuid 不可枚举） |

## 2. 网关防护基线（与路由配合）

- `discovery.locator.enabled: false`（按服务名自动路由关闭，影子服务无法凭注册名被意外路由）——**发布检查项：确认保持 false**；
- 网关登录态鉴权（SaTokenConfigure）：除 `/api/auth/anno/**`、`/api/file/view/**`、`/actuator/health`、`/favicon.ico` 外一律要求登录；
- 同一 token 透传 + Same-Token 注入（ForwardSameTokenFilter）：下游七服务入站校验（T-SEC-04）；
- 全局过滤器链：RequestSanitizeFilter(-300，URL 过滤/头剥离/真实IP) → SecurityHeadersFilter(-260，安全响应头) → RateLimitFilter(-250，四档限流) → ForwardSameTokenFilter(-100) → RequestLogFilter(-50，traceId+访问日志)。

## 3. 敏感路径检查（发布前 curl 验证全 404/401）

| 路径 | 预期 | 依据 |
|---|---|---|
| `/actuator/env`、`/actuator/beans`、`/actuator/*`（除 health） | 404（网关仅暴露 health） | T-SEC-02/A6 |
| `/druid/*` | 404 | 无路由 |
| `/swagger-ui.html`、`/v3/api-docs` | 404（BFF swagger 默认关） | T-SEC-02/A6 |
| `/env`、`/nacos/*` | 404 | 无路由（Nacos 控制台不经网关） |
| 任意未注册服务名（如 `/api/xf-base/**`） | 404 | locator 关闭 + 无该前缀路由 |

## 4. 变更登记（改路由必改本清单，随仓库走）

| 日期 | 变更 | 提交 |
|---|---|---|
| 2026-10-16 | 初版：核对 8 条路由与实际服务一致 | — |
