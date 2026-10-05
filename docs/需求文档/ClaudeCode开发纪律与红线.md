# Claude Code 开发纪律与红线（品达物流 TMS）

> **用途**：每次启动编码任务前，先把本文档作为系统约束加载。违反红线的改动一律回退。
> **配套权威文档**：`开发规范与需求规格说明书.md`（以下简称《规格书》）。本文档是其执行级摘要。

---

## 0. 总纲
业务优先、解决实际问题、不做花架子。技术栈与现有后端**严格锁定**，任何偏离须经人工确认，不得自作主张。

## 1. 宪法级红线（违反即不合格）
1. **JDK 固定 1.8**，不升级；`pom.xml` 不出现 Java 9+ 语法 / module-info。
2. **不升级任何后端依赖**（Spring Boot/Cloud/Alibaba/MyBatis Plus/Druid/Shiro/Drools/Lombok/FastJSON 等）。
3. **安全 CVE 不处理**（FastJSON 1.2.47/1.2.62、Shiro 1.4.0 不主动修）。
4. **不引入新中间件**（MongoDB/HBase/Spark/Hive/ES/RocketMQ/XXL-JOB/Sentinel/SkyWalking 等是用户路线图，不是缺失 bug，禁止"顺手"补齐）。
5. **不重写/不重构** `pd-admin-ui`（管理端 Vue2）与其他既有文件，除非任务明确要求。
6. **不更换网关选型**：现状是 Zuul 1.x，不是 Spring Cloud Gateway、无 Sentinel。
7. **不跨模块写业务**：订单→pd-oms、调度→pd-dispatch、作业→pd-work、基础→pd-base、轨迹→pd-netty。

## 2. 开发行为纪律
8. **先读后写**：改动前必须 Read/搜索确认现状与调用关系，禁止凭记忆写代码。
9. **复用既有能力**：三端 REST API（`pd-web-customer/driver/courier`，见《规格书》§5）已存在，移动端直接调，**后端能不改则不改**。
10. **禁止过度设计（YAGNI）**：不引入当前用不到的抽象/模式/框架/缓存/MQ。
11. **禁止硬编码**：cron/阈值/地图 Key/坐标类型/上报间隔一律外部化到配置文件。
12. **配置零改动后端坐标**：后端存百度 BD-09；微信小程序用腾讯 GCJ-02，前端做 BD-09→GCJ-02 转换，**后端正则不动**。
13. **禁止裸请求**：前端所有 `uni.request` 必须过 `common/request.js` 拦截器（JWT 自动附加/401 刷新）。
14. **认证/计费规则放后端**：前端只调用不计算核心业务规则（运费、状态机、计费）。

## 3. 质量与交付
15. **编译保证**：每次改动后受影响模块 `mvn -o -q compile` 通过（本地 JDK 1.8）；前端 `npm run build` 通过。
16. **契约同步**：新增/变更接口必须更新《规格书》§5、§6；前后端字段一致。
17. **日志脱敏**：手机号/证件脱敏；禁止 `System.out.println`。
18. **无 N+1**：禁止循环内逐条查库，批量用 `in`/批量接口。
19. **文档同步**：改动同步到 `docs/` 相关文档；新增 API 补入 §5。

## 4. 本期范围（做）
- P0：三端移动前端（客户/快递员/司机）→ POD 电子签收 → 运输闭环看板 + 在途监控 → 统一登录。
- P1：承运商/外协运力、运单拆合单、回单归档核销联动计费、附加费与多维对账、司机结算、异常类型化+索赔、节点通知、客户 Share Link、绩效考核、Seata 配置补全、状态回调补偿、调度可视化。
- P1（**用户明确新增的真实需求，不得当花架子砍掉**）：
  - **北斗定位（BDS）**：`LocationRecord`/`LocationEntity` 补 `coordSystem`+`source`（当前**两字段均不存在**，前端仅预留）；统一坐标转换；车载终端 JT/T 808 接入（复用现有 Netty）。
  - **司机视频接入**：云厂商 RTC 或 GB/T 28181 / JT/T 1078 国标，**不自建 RTMP 集群**；后端当前无任何 rtc/stream 接口，前端入口先置灰占位。
- P1：**支付闭环**（到付 P0 / 在线支付 P1）。后端 `PUT /mailing/pay/{id}` 与 `Order.paymentMethod/paymentStatus` 已存在。

## 5. 本期不做（花架子，一律不启动）
- EDI/ERP/多式联运/冷链/BI/重型 WMS/多租户 SaaS/预约月台。
- 管理端 Vue3 重写、后端依赖升级、新中间件引入。

## 6. UI / 页面红线（大模型写页面最易翻车，必须遵守）
20. **只用组件库**：移动端 100% 用 uview-plus 组件，**禁止**用 `<div>`+`<style>` 替代布局/按钮/卡片。
21. **Token 全集中**：颜色/间距/字号/圆角/阴影只允许引用 `uni.scss` + `common/styles/variables.scss`，**禁止**页面内写死 hex/px/魔法数字。
22. **三端同视觉**：客户/快递员/司机共用同一套 Token 与组件，仅角色化强调（司机端按钮更大），**禁止**三端各一套。
23. **结构走模板**：六类页面（列表/详情/表单/地图/任务大厅/POD）套用《UI 设计规范与页面模板.md》§4 骨架，**禁止**自由布局。
24. **三态必做**：加载中/空/错误统一用组件库，缺一不可。
25. **禁止第二套 UI/图标库**、禁止 emoji 当图标、禁止缺三态、禁止绕过 `u-navbar`、禁止 `uni.request` 裸调（过拦截器）。
26. 写任何页面前先读《UI 设计规范与页面模板.md》。

## 7. 开工前必做
- 读《规格书》第 1 章 + 第 12 章 + 本文件 + 《UI 设计规范与页面模板.md》。
- 确认任务归属模块与对应 API（§5）。
- **七条硬事实（2026-10-06 实测，违反即失败）**：
  1. **所有请求必须走网关，且必须带端前缀**：
     `http://192.168.20.130:8760/api/web-customer`（客户）、`/api/web-courier`（快递员）、`/api/web-driver`（司机）、`/api/netty-service`（轨迹）、`/api/auth`（登录）。
     **直连 8161~8164 会被 `TokenAuthInterceptor` 401 拒绝**（端口仅供健康检查）。
  2. JWT 请求头字段是 **`token`**（不是 `Authorization`）；有效期 7200s，**无 refreshToken** → 401 清 token 重登。
     后端登录 **已存在** `POST /anno/login` + `GET /anno/captcha?key={uuid}`，**不得新建登录服务**。
  3. 轨迹查询只认 `businessId`+`type`（**无 `orderId`**）；`/trace/page` 才支持 `transportTaskId`。客户按订单查轨迹需后端改造，**不得前端伪造参数**。
  4. 🔴 **三端接口必须先注册进 `pd_auth.pd_auth_resource` 并绑定角色**，否则网关 `AccessFilter` 判"未知请求"**直接拒绝**。
     实测该表仅 48 条，全是后台权限接口（/org /role /resource /station /user /menu…），**无任何 driver/courier/customer 业务接口**，且**无自动注册机制**。
     ❌ **禁止**把三端前缀加入忽略鉴权绕过——跳过 token 校验会导致下游 `TokenAuthInterceptor` 401。
  5. 🔴 **Redis 必须已生效才能联调**。配置已于 2026-10-06 推入 Nacos，但需 `docker restart <pd-gateway> <pd-auth-server>` 才生效。
     验收：`curl -o /dev/null -w "%{http_code}" "http://192.168.20.130:8760/api/auth/anno/captcha?key=k1"` → **期望 200**。
     未生效时表现为：验证码 500、所有网关请求 `500 pre:AccessFilter`（**这不是你的代码 bug**）。
  6. **严禁新建/重写状态机**：`StateTransitionValidator` 已实现且在用（`TaskTransportServiceImpl:243/287/331`、`TransportOrderServiceImpl:74/84`）。真实缺口是**跨端同步桥**（司机发车未联动 Order 23005），不是校验器。
  7. **环境地址**：服务实跑 Docker 网 `172.21.0.x`，宿主机 `192.168.20.130`；Nacos `8848`（ns `1cb93ce4-dc0e-4730-b759-d35fd7ed93c3`，group `pinda-tms`）。**密码不入库，勿写进任何仓库文件。**

- **开工前必读顺序**：《文档与代码一致性核查报告.md》（**§6/§7 为最新环境事实**）→ 本文件 → 《UI 设计规范与页面模板.md》→ 《规格书》第 1/12 章。
  ⚠️ 《规格书》§5/§6 尚未同步 2026-10-06 的网关前缀与阻断 C，**环境类事实一律以核查报告为准**。

---
*任何与《规格书》冲突的"优化"默认无效，须先修订文档并经人工确认。*
