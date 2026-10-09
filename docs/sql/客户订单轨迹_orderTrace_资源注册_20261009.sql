-- ============================================================================
-- 客户订单轨迹查询 orderTrace · 网关资源注册（2026-10-09 新增）
--
-- 背景：客户小程序"我的订单→在途轨迹"端点已落地，对外路径：
--   GET /api/web-customer/orderTrace/trace?orderId=<订单号>
--   源码：pd-web/pd-web-customer/.../controller/OrderTraceController.java
--         类映射 @RequestMapping("orderTrace") + 方法 @GetMapping("trace")
--   接口内部先做 memberId 归属校验（OwnershipAssert），只允许查本人订单。
--
-- 🔴 为什么必须执行本脚本（《12》§7 硬事实第4条）：
--   新增对外接口若未注册进 pd_auth_resource 并绑定角色，网关 AccessFilter
--   判"未知请求"会**直接拒绝**（401/500 pre:AccessFilter），前端会误以为接口没写。
--
-- ⚠️ url 填法（务必照抄，已据 AccessFilter 剥前缀逻辑核对）：
--   填**服务内路径**，不含 "/api"、不含 "web-customer" 等网关段；
--   query string（?orderId=）不参与匹配，无需写入。
--   请求 GET /api/web-customer/orderTrace/trace  →  permission = "GET/orderTrace/trace"
--   故 url = '/orderTrace/trace'，method='GET'，startsWith 前缀匹配命中。
--
-- 角色：绑定到已存在的 CUSTOMER 角色（id=690609420538744422，
--       由《阻断C_三端接口注册与账号闭环.sql》创建），本脚本不新建角色。
--
-- 🔁 幂等：资源与角色绑定均带 NOT EXISTS 判断，可安全重复执行。
-- 🛠 仅生成脚本，未执行任何写库操作。请在内网 dev 库（192.168.20.130）手动执行。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- §0 执行前备份（建议；InnoDB 可在线操作）
-- ----------------------------------------------------------------------------
-- CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_resource_bak_20261009     AS SELECT * FROM pd_auth.pd_auth_resource;
-- CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_role_authority_bak_20261009 AS SELECT * FROM pd_auth.pd_auth_role_authority;

-- 执行前确认起点未被别的批次占用（期望查不到 id=784920165000000003）：
-- SELECT id, code, url FROM pd_auth.pd_auth_resource WHERE id = 784920165000000003;
-- SELECT id, code, url FROM pd_auth.pd_auth_resource WHERE code = 'customer:orderTrace:trace' OR url = '/orderTrace/trace';

-- ----------------------------------------------------------------------------
-- §1 主事务（先跑，确认 SELECT 计数后再 COMMIT；不放心可先把 COMMIT 改 ROLLBACK）
-- ----------------------------------------------------------------------------
USE pd_auth;
START TRANSACTION;

-- 1) 注册资源（幂等：同 code 或同 url 已存在则跳过）
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
SELECT 784920165000000003, 'customer:orderTrace:trace', '客户-订单轨迹查询', NULL, 'GET',
       '/orderTrace/trace',
       '客户按订单号查在途轨迹（内部解析订单→运单→运输任务；含 memberId 归属校验，防水平越权）',
       '3', NOW(), '3', NOW()
  FROM DUAL
 WHERE NOT EXISTS (
       SELECT 1 FROM pd_auth.pd_auth_resource
        WHERE code = 'customer:orderTrace:trace' OR url = '/orderTrace/trace');

-- 2) 绑定到 CUSTOMER 角色（幂等：同一资源+角色已绑定则跳过）
SET @ra = 784920165000000200;
INSERT INTO pd_auth.pd_auth_role_authority
  (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744422, NOW(), '3'
  FROM pd_auth.pd_auth_resource r
 WHERE r.code = 'customer:orderTrace:trace'
   AND NOT EXISTS (
       SELECT 1 FROM pd_auth.pd_auth_role_authority x
        WHERE x.authority_id = r.id AND x.authority_type = 'RESOURCE'
          AND x.role_id = 690609420538744422);

-- ----------------------------------------------------------------------------
-- §2 校验（试跑时核对：资源 1 行、绑定 1 行）
-- ----------------------------------------------------------------------------
SELECT '资源(应=1):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_resource
 WHERE code = 'customer:orderTrace:trace' AND method = 'GET' AND url = '/orderTrace/trace';

SELECT 'CUSTOMER绑定(应=1):' AS chk, COUNT(*) AS v
  FROM pd_auth.pd_auth_role_authority ra
  JOIN pd_auth.pd_auth_resource r ON r.id = ra.authority_id
 WHERE r.code = 'customer:orderTrace:trace' AND ra.authority_type = 'RESOURCE'
   AND ra.role_id = 690609420538744422;

-- 确认无误 → COMMIT；异常 → ROLLBACK
COMMIT;
-- ROLLBACK;

-- ============================================================================
-- §3 生效与联调验证
-- ----------------------------------------------------------------------------
-- 步骤 1：清网关缓存（AccessFilter 用 j2cache 缓存鉴权清单，不清不生效），二选一：
--   a) 重启网关：docker restart <pd-gateway 容器名>
--   b) 在 Nacos 控制台 / Redis 清理缓存 key（RESOURCE_NEED_TO_CHECK）
--
-- 步骤 2：查 auth-server 实时清单（直连 9000，绕过网关）：
--   curl -s "http://192.168.20.130:9000/resource/list" | grep "orderTrace/trace"
--   期望：出现 "GET/orderTrace/trace"。
--
-- 步骤 3：用客户账号（如 customer01）登录拿 token，请求本人订单轨迹：
--   curl -s -H "token: $TOKEN" \
--        "http://192.168.20.130:8760/api/web-customer/orderTrace/trace?orderId=<本人订单号>"
--   期望：HTTP 200 且返回业务 JSON（data 内含 orderStatus/tracks）。
--
-- 反向与越权验证：
--   · 不带 token 或乱填路径        → 被网关拒绝（pre:AccessFilter），而非业务数据。
--   · orderId 换成他人订单号        → 返回归属校验错误"无权查看他人订单"（非 200 数据）。
--   · 不带 orderId                 → 业务返回 400 "orderId不能为空"。
--   若已注册却提示"用户没有访问xxx资源的权限" → 资源已建但角色绑定未成功，回 §1.2。
-- ============================================================================
