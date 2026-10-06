-- ============================================================================
-- 阻断 C 解除 + 三端账号闭环（自动执行版）
-- 生成：2026-10-06  |  依据：pd-web/pd-web-{driver,courier,customer} 源码提取的接口
--                      + AccessFilter 匹配逻辑（method+servicePath 前缀匹配）
--                      + pd_auth 全表实测（角色/用户/资源/授权现状）
-- 说明：
--   1. 所有账号密码统一为 MD5("123456")=e10adc3949ba59abbe56e057f20f883e（与 pinda/test/driver01 等现有账号一致）
--   2. pd_truck_driver.user_id 现 5 条 seed 数据为悬空(10001~10005)，回写至真实司机账号 9101~9105
--   3. 三端角色新建，接口按端授权，账号绑定角色，AccessFilter 两步校验均可通过
-- ============================================================================

-- ===================== §0 备份（DDL 自动提交，不可回滚但安全） =====================
CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_resource_bak_20261006b   AS SELECT * FROM pd_auth.pd_auth_resource;
CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_role_bak_20261006b       AS SELECT * FROM pd_auth.pd_auth_role;
CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_role_authority_bak_20261006b AS SELECT * FROM pd_auth.pd_auth_role_authority;
CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_user_bak_20261006b       AS SELECT * FROM pd_auth.pd_auth_user;
CREATE TABLE IF NOT EXISTS pd_auth.pd_auth_user_role_bak_20261006b   AS SELECT * FROM pd_auth.pd_auth_user_role;

-- ===================== 主事务（先 ROLLBACK 试跑，确认 COUNT 无误后改 COMMIT） =====================
START TRANSACTION;

-- ---------- §1 新建三端角色 ----------
INSERT INTO pd_auth.pd_auth_role (id, code, name, describe_, create_user, create_time, update_user, update_time)
VALUES
  (690609420538744420, 'DRIVER',   '司机端',   '司机 App 用户',          '3', NOW(), '3', NOW()),
  (690609420538744421, 'COURIER',  '快递员端', '快递员 App 用户',        '3', NOW(), '3', NOW()),
  (690609420538744422, 'CUSTOMER', '客户端',   '客户小程序/Web 用户',    '3', NOW(), '3', NOW());

-- ---------- §2 新建司机04/05、客户01 账号（密码 MD5(123456)，可直接登录） ----------
INSERT INTO pd_auth.pd_auth_user
  (id, account, name, org_id, station_id, email, mobile, sex, status, avatar, work_describe, password, create_user, create_time, update_user, update_time)
VALUES
  (9104, 'driver04',   '测试司机04', NULL, NULL, '', '13800000004', 'M', b'1', '', '司机',   'e10adc3949ba59abbe56e057f20f883e', '3', NOW(), '3', NOW()),
  (9105, 'driver05',   '测试司机05', NULL, NULL, '', '13800000005', 'M', b'1', '', '司机',   'e10adc3949ba59abbe56e057f20f883e', '3', NOW(), '3', NOW()),
  (9203, 'customer01', '测试客户01', NULL, NULL, '', '13900000001', 'N', b'1', '', '客户',   'e10adc3949ba59abbe56e057f20f883e', '3', NOW(), '3', NOW());

-- ---------- §3 注册三端接口资源（42 条，id 258~299） ----------
-- url 填写服务内路径（不含 /api、不含 web-driver 等网关段），与 AccessFilter 剥前缀后的 permission 对齐
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  -- ===== DRIVER (10) =====
  (684539815017848258, 'driver:attachment:upload',      '司机-附件上传',     NULL, 'POST', '/attachment/upload',        '司机端附件上传',           '3', NOW(), '3', NOW()),
  (684539815017848259, 'driver:car:info',               '司机-车辆信息',     NULL, 'GET',  '/business/car/info',         '司机端车辆信息',          '3', NOW(), '3', NOW()),
  (684539815017848260, 'driver:cargo:wait',             '司机-待接任务',     NULL, 'GET',  '/business/cargo/wait',        '司机端待接运输任务',       '3', NOW(), '3', NOW()),
  (684539815017848261, 'driver:cargo:history',          '司机-历史任务',     NULL, 'GET',  '/business/cargo/history',     '司机端历史任务',          '3', NOW(), '3', NOW()),
  (684539815017848262, 'driver:cargo:onTheWay',         '司机-运输中任务',   NULL, 'GET',  '/business/cargo/onTheWay',    '司机端运输中任务',        '3', NOW(), '3', NOW()),
  (684539815017848263, 'driver:cargo:detail',           '司机-任务详情',     NULL, 'GET',  '/business/cargo/detail',      '司机端任务详情',          '3', NOW(), '3', NOW()),
  (684539815017848264, 'driver:cargo:orders',           '司机-订单列表',     NULL, 'GET',  '/business/cargo/orders',      '司机端订单列表',          '3', NOW(), '3', NOW()),
  (684539815017848265, 'driver:cargo:pickUp',           '司机-提货',         NULL, 'PUT',  '/business/cargo/pickUp',      '司机端提货',              '3', NOW(), '3', NOW()),
  (684539815017848266, 'driver:cargo:finish',           '司机-完成运输',     NULL, 'PUT',  '/business/cargo/finish',      '司机端完成运输',          '3', NOW(), '3', NOW()),
  (684539815017848267, 'driver:profile',                '司机-个人资料',     NULL, 'GET',  '/user/profile',              '司机端个人资料',          '3', NOW(), '3', NOW()),
  -- ===== COURIER (13) =====
  (684539815017848268, 'courier:common:area:simple',    '快递员-简单区域',   NULL, 'GET',  '/common/area/simple',         '快递员端区域简表',        '3', NOW(), '3', NOW()),
  (684539815017848269, 'courier:pickupDispatch',        '快递员-取派任务',   NULL, 'GET',  '/courier/pickupDispatch',     '快递员端取派任务',        '3', NOW(), '3', NOW()),
  (684539815017848270, 'courier:count',                 '快递员-任务计数',   NULL, 'GET',  '/courier/count',              '快递员端任务计数',        '3', NOW(), '3', NOW()),
  (684539815017848271, 'courier:detail',                '快递员-任务详情',   NULL, 'GET',  '/courier/detail',             '快递员端任务详情',        '3', NOW(), '3', NOW()),
  (684539815017848272, 'courier:detail:update',         '快递员-更新任务',   NULL, 'PUT',  '/courier/detail/{id}',        '快递员端更新任务',        '3', NOW(), '3', NOW()),
  (684539815017848273, 'courier:warehousing',           '快递员-入库',       NULL, 'PUT',  '/courier/warehousing/{tranOrderId}', '快递员端入库',       '3', NOW(), '3', NOW()),
  (684539815017848274, 'courier:handover',              '快递员-交接',       NULL, 'PUT',  '/courier/handover/{tranOrderId}',  '快递员端交接',         '3', NOW(), '3', NOW()),
  (684539815017848275, 'courier:delivered',             '快递员-妥投',       NULL, 'PUT',  '/courier/delivered/{tranOrderId}/{status}', '快递员端妥投', '3', NOW(), '3', NOW()),
  (684539815017848276, 'courier:verifyIdCard',          '快递员-核验证件',   NULL, 'GET',  '/courier/verifyIdCard',       '快递员端核验证件',        '3', NOW(), '3', NOW()),
  (684539815017848277, 'courier:route',                 '快递员-路线',       NULL, 'GET',  '/courier/route',              '快递员端路线',            '3', NOW(), '3', NOW()),
  (684539815017848278, 'courier:totalPrice',            '快递员-总价',       NULL, 'POST', '/courier/totalPrice',         '快递员端总价计算',        '3', NOW(), '3', NOW()),
  (684539815017848279, 'courier:goodsType:all',         '快递员-货物类型',   NULL, 'GET',  '/goodsType/all',              '快递员端货物类型全量',     '3', NOW(), '3', NOW()),
  (684539815017848280, 'courier:profile',               '快递员-个人资料',   NULL, 'GET',  '/user/profile',              '快递员端个人资料',        '3', NOW(), '3', NOW()),
  -- ===== CUSTOMER (19) =====
  (684539815017848281, 'customer:address:page',         '客户-地址分页',     NULL, 'GET',  '/address/page',               '客户端地址分页',          '3', NOW(), '3', NOW()),
  (684539815017848282, 'customer:address:create',       '客户-新增地址',     NULL, 'POST', '/address',                   '客户端新增地址',          '3', NOW(), '3', NOW()),
  (684539815017848283, 'customer:address:update',       '客户-改地址',       NULL, 'PUT',  '/address/{id}',               '客户端修改地址',          '3', NOW(), '3', NOW()),
  (684539815017848284, 'customer:address:delete',       '客户-删地址',       NULL, 'DELETE','/address/{id}',               '客户端删除地址',          '3', NOW(), '3', NOW()),
  (684539815017848285, 'customer:address:detail',       '客户-地址详情',     NULL, 'GET',  '/address/detail/{id}',        '客户端地址详情',          '3', NOW(), '3', NOW()),
  (684539815017848286, 'customer:agency:page',          '客户-机构分页',     NULL, 'GET',  '/agency/page',                '客户端机构分页',          '3', NOW(), '3', NOW()),
  (684539815017848287, 'customer:attachment:upload',    '客户-附件上传',     NULL, 'POST', '/attachment/upload',          '客户端附件上传',          '3', NOW(), '3', NOW()),
  (684539815017848288, 'customer:common:area:simple',   '客户-简单区域',     NULL, 'GET',  '/common/area/simple',         '客户端区域简表',          '3', NOW(), '3', NOW()),
  (684539815017848289, 'customer:goodsType:all',        '客户-货物类型',     NULL, 'GET',  '/goodsType/all',              '客户端货物类型全量',       '3', NOW(), '3', NOW()),
  (684539815017848290, 'customer:mailing:totalPrice',   '客户-寄件计价',     NULL, 'POST', '/mailing/totalPrice',         '客户端寄件计价',          '3', NOW(), '3', NOW()),
  (684539815017848291, 'customer:mailing:create',       '客户-寄件下单',     NULL, 'POST', '/mailing',                    '客户端寄件下单',          '3', NOW(), '3', NOW()),
  (684539815017848292, 'customer:mailing:update',       '客户-改寄件',       NULL, 'PUT',  '/mailing/{id}',               '客户端修改寄件',          '3', NOW(), '3', NOW()),
  (684539815017848293, 'customer:mailing:pay',          '客户-支付',         NULL, 'PUT',  '/mailing/pay/{id}',           '客户端支付寄件',          '3', NOW(), '3', NOW()),
  (684539815017848294, 'customer:mailing:cancel',       '客户-取消寄件',     NULL, 'PUT',  '/mailing/cancel/{id}',        '客户端取消寄件',          '3', NOW(), '3', NOW()),
  (684539815017848295, 'customer:mailing:count',        '客户-寄件计数',     NULL, 'GET',  '/mailing/count',              '客户端寄件计数',          '3', NOW(), '3', NOW()),
  (684539815017848296, 'customer:mailing:page',         '客户-寄件分页',     NULL, 'GET',  '/mailing/page',               '客户端寄件分页',          '3', NOW(), '3', NOW()),
  (684539815017848297, 'customer:mailing:detail',       '客户-寄件详情',     NULL, 'GET',  '/mailing/detail',             '客户端寄件详情',          '3', NOW(), '3', NOW()),
  (684539815017848298, 'customer:mailing:route',        '客户-寄件路线',     NULL, 'GET',  '/mailing/route',              '客户端寄件路线',          '3', NOW(), '3', NOW()),
  (684539815017848299, 'customer:profile',              '客户-个人资料',     NULL, 'GET',  '/user/profile',               '客户端个人资料',          '3', NOW(), '3', NOW());

-- ---------- §4 按端授权给对应角色（用户变量写法，5.7.44 兼容） ----------
SET @ra = 690609420538744417;
INSERT INTO pd_auth.pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744420, NOW(), '3'
FROM pd_auth.pd_auth_resource r WHERE r.code LIKE 'driver:%';

INSERT INTO pd_auth.pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744421, NOW(), '3'
FROM pd_auth.pd_auth_resource r WHERE r.code LIKE 'courier:%';

INSERT INTO pd_auth.pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT (@ra := @ra + 1), r.id, 'RESOURCE', 690609420538744422, NOW(), '3'
FROM pd_auth.pd_auth_resource r WHERE r.code LIKE 'customer:%';

-- ---------- §5 账号绑定到角色 ----------
INSERT INTO pd_auth.pd_auth_user_role (id, role_id, user_id, create_user, create_time)
VALUES
  (690609420538744500, 690609420538744420, 9101, '3', NOW()),
  (690609420538744501, 690609420538744420, 9102, '3', NOW()),
  (690609420538744502, 690609420538744420, 9103, '3', NOW()),
  (690609420538744503, 690609420538744420, 9104, '3', NOW()),
  (690609420538744504, 690609420538744420, 9105, '3', NOW()),
  (690609420538744505, 690609420538744421, 9201, '3', NOW()),
  (690609420538744506, 690609420538744421, 9202, '3', NOW()),
  (690609420538744507, 690609420538744422, 9203, '3', NOW());

-- ---------- §6 回写 pd_truck_driver.user_id（悬空 10001~10005 → 真实账号 9101~9105） ----------
UPDATE pd_base.pd_truck_driver SET user_id='9101' WHERE id='2400000000000000001';
UPDATE pd_base.pd_truck_driver SET user_id='9102' WHERE id='2400000000000000002';
UPDATE pd_base.pd_truck_driver SET user_id='9103' WHERE id='2400000000000000003';
UPDATE pd_base.pd_truck_driver SET user_id='9104' WHERE id='2400000000000000004';
UPDATE pd_base.pd_truck_driver SET user_id='9105' WHERE id='2400000000000000005';

-- ===================== §7 校验（试跑时务必核对，全部符合再 COMMIT） =====================
SELECT '新增资源数(应=42):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_resource WHERE id BETWEEN 684539815017848258 AND 684539815017848299;
SELECT '新增角色数(应=3):'  AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_role WHERE id BETWEEN 690609420538744420 AND 690609420538744422;
SELECT '新增账号数(应=3):'  AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_user WHERE id IN (9104,9105,9203);
SELECT '新增授权数(应=42):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_role_authority WHERE id BETWEEN 690609420538744418 AND 690609420538744459;
SELECT '新增绑定数(应=8):'  AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_user_role WHERE id BETWEEN 690609420538744500 AND 690609420538744507;
SELECT '司机悬空引用(应=0):' AS chk, COUNT(*) AS v FROM pd_base.pd_truck_driver WHERE user_id NOT IN (SELECT id FROM pd_auth.pd_auth_user);
SELECT 'DRIVER角色接口授权数(应=10):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_role_authority WHERE role_id=690609420538744420;
SELECT 'COURIER角色接口授权数(应=13):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_role_authority WHERE role_id=690609420538744421;
SELECT 'CUSTOMER角色接口授权数(应=19):' AS chk, COUNT(*) AS v FROM pd_auth.pd_auth_role_authority WHERE role_id=690609420538744422;

-- ===================== 已校验通过，正式提交 =====================
-- ROLLBACK;
COMMIT;
