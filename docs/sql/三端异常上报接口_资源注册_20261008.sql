-- ============================================================================
-- 移动三端异常上报接口 · 网关资源注册（2026-10-08新增）
--
-- 背景：三端开工前必须补的两个 P0 接口（SRS §6.16 / 规划 M-B0）：
--   1. 快递员异常上报（D-41 · DIS-03 = 真P0）→ pd-web-courier
--      POST /api/web-courier/courier/exception/report
--   2. 司机在途异常上报（D-57）→ pd-web-driver
--      POST /api/web-driver/business/cargo/exception/report
--      ⚠️ 2026-10-09 订正：本脚本原写 '/cargo/exception/report'，与服务内真实路径
--      不符（CargoController 类映射为 @RequestMapping("business/cargo")），已改为
--      '/business/cargo/exception/report'，并补 §4 存量行订正 UPDATE。
-- 两者均转发至 pd-netty的 POST /alarm/report 落库 pd_alarm_record。
--
-- 🔴 为什么必须执行本脚本（红线第 4 条）：
--   新增对外接口若未注册进 pd_auth_resource 并绑定角色，网关 AccessFilter
--   判"未知请求"会**直接拒绝**，表现为 401/404——前端会误以为接口没写。
--
-- ⚠️ url 填法（务必照抄，已实测）：
--   填**服务内路径**，不含 "/api"、不含 "web-courier"/"web-driver" 等网关段。
--   正例 courier: '/courier/exception/report'
--   反例      '/api/web-courier/courier/exception/report'  ← 会永远失败
--
-- 执行前请确认：pd_alarm_record 表已存在（见 docs/sql/告警记录_建表.sql）。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. 快递员端：异常上报
-- ----------------------------------------------------------------------------
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (784920165000000001, 'courier:exception:report', '快递员-异常上报', NULL, 'POST',
   '/courier/exception/report',
   '快递员上报破损/拒收/地址错误（D-41 · DIS-03 P0；三要素：类型+照片+备注）',
   '3', NOW(), '3', NOW());

-- ----------------------------------------------------------------------------
-- 2. 司机端：在途异常上报
-- ----------------------------------------------------------------------------
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (784920165000000002, 'driver:exception:report', '司机-在途异常上报', NULL, 'POST',
   '/business/cargo/exception/report',
   '司机上报车辆故障/货物损失/延误（D-57；三要素：类型+照片+备注）',
   '3', NOW(), '3', NOW());

-- ----------------------------------------------------------------------------
-- 3. 角色绑定（沿用既有角色ID，勿新建角色）
-- ----------------------------------------------------------------------------
-- ⚠️ 必须先初始化 @ra（角色授权表主键 id 由此变量自增；漏掉会插入 NULL id）
SET @ra = 784920165000000100;

-- 快递员角色 690609420538744421
INSERT INTO pd_auth.pd_auth_role_authority
  (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744421, NOW(), '3'
FROM pd_auth.pd_auth_resource r WHERE r.code = 'courier:exception:report';

-- 司机角色 690609420538744420
INSERT INTO pd_auth.pd_auth_role_authority
  (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744420, NOW(), '3'
FROM pd_auth.pd_auth_resource r WHERE r.code = 'driver:exception:report';

-- ============================================================================
-- 4. 存量订正（幂等）：本脚本 2026-10-08 版曾把司机端 url 写成 '/cargo/exception/report'
--    真实服务内路径 = '/business/cargo/exception/report'（CargoController.java:68 类映射
--    "business/cargo" + :792 @PostMapping("exception/report")）。
--    AccessFilter.java:145 判定式为 equals || startsWith(resource + "/")，
--    旧值既不等于也不前缀命中 → 网关按"未知请求"fail-closed 拒绝（D-57 P0 不可用）。
--    若该错误行尚未执行，本节影响 0 行；已执行则一次订正，可安全重复运行。
-- ============================================================================
UPDATE pd_auth.pd_auth_resource
   SET url = '/business/cargo/exception/report', update_time = NOW()
 WHERE code = 'driver:exception:report' AND url = '/cargo/exception/report';

-- ============================================================================
-- 自检：期望返回 2 行（driver 端 url 应为 /business/cargo/exception/report）
-- ============================================================================
SELECT code, method, url, name FROM pd_auth.pd_auth_resource
WHERE code IN ('courier:exception:report', 'driver:exception:report');

-- 自检：期望返回 2 行（资源已绑角色）
SELECT r.code, ra.role_id FROM pd_auth.pd_auth_role_authority ra
JOIN pd_auth.pd_auth_resource r ON r.id = ra.authority_id
WHERE r.code IN ('courier:exception:report', 'driver:exception:report');

-- ============================================================================
-- 联调验证（服务重启后执行，应返回 400 而非 401/404——400 说明已放行到业务校验）
--   curl -X POST "http://192.168.20.130:8760/api/web-courier/courier/exception/report" \
--        -H "token: $TOKEN" -H "Content-Type: application/json" \
--        -d '{"exceptionType":"GOODS_DAMAGED","attachmentIds":["a1"],"remark":"测试"}'
--   curl -X POST "http://192.168.20.130:8760/api/web-driver/business/cargo/exception/report" \
--        -H "token: $TOKEN" -H "Content-Type: application/json" \
--        -d '{"exceptionType":"VEHICLE_BREAKDOWN","attachmentIds":["a1"],"remark":"测试"}'
-- ⚠️ 三要素缺一时应返回 400 并给出明确提示（如"必须上传照片凭证"），
--    而**不是** 401——若返回 401 说明资源未注册成功。
-- ============================================================================