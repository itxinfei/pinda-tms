-- ============================================================
-- 品达TMS 开发测试环境 造数脚本
-- 目标: 4端(管理/司机/快递员/客户)可登录 + 核心业务数据 ≥10条
-- 幂等: 全部使用 INSERT IGNORE(主键冲突跳过), 可重复执行
-- 用法: docker exec -i mysql57 mysql -uroot -p123456 < testdata.sql
-- 生成: 2026-10-06 (表结构实测于 192.168.20.130)
-- ============================================================

-- ------------------------------------------------------------
-- 0) 统一重置所有用户密码 = 123456
--    登录校验为 MD5(AuthManager.md5Hex), 直接 MD5('123456') 生成
-- ------------------------------------------------------------
UPDATE pd_auth.pd_auth_user
SET password = MD5('123456')
WHERE account <> 'pinda';

-- ------------------------------------------------------------
-- 1) 后台管理员 (管理端) +2  —— 角色 PT_ADMIN(100)
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_auth.pd_auth_user
  (id, account, name, password, org_id, station_id, sex, status, work_describe, create_time)
VALUES
  (9001, 'admin01', '测试管理员01', MD5('123456'), 100, 100, 'M', 1, '测试管理员', NOW()),
  (9002, 'admin02', '测试管理员02', MD5('123456'), 100, 100, 'M', 1, '测试管理员', NOW());

INSERT IGNORE INTO pd_auth.pd_auth_user_role (id, role_id, user_id, create_user, create_time) VALUES
  (900101, 100, 9001, 3, NOW()),
  (900201, 100, 9002, 3, NOW());

-- ------------------------------------------------------------
-- 2) 司机 (司机端) +3  —— 账号 + 司机档案(pd_truck_driver)
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_auth.pd_auth_user
  (id, account, name, password, org_id, station_id, sex, status, work_describe, create_time)
VALUES
  (9101, 'driver01', '测试司机01', MD5('123456'), 102, 100, 'M', 1, '司机', NOW()),
  (9102, 'driver02', '测试司机02', MD5('123456'), 102, 100, 'M', 1, '司机', NOW()),
  (9103, 'driver03', '测试司机03', MD5('123456'), 102, 100, 'M', 1, '司机', NOW());

INSERT IGNORE INTO pd_auth.pd_auth_user_role (id, role_id, user_id, create_user, create_time) VALUES
  (910101, 643779012732130273, 9101, 3, NOW()),
  (910201, 643779012732130273, 9102, 3, NOW()),
  (910301, 643779012732130273, 9103, 3, NOW());

INSERT IGNORE INTO pd_base.pd_truck_driver (id, user_id, fleet_id, age, driving_age) VALUES
  ('9101', '9101', NULL, 35, 8),
  ('9102', '9102', NULL, 30, 5),
  ('9103', '9103', NULL, 42, 15);

-- ------------------------------------------------------------
-- 3) 快递员 (快递员端) +2  —— 账号 + 派送范围(pd_courier_scop)
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_auth.pd_auth_user
  (id, account, name, password, org_id, station_id, sex, status, work_describe, create_time)
VALUES
  (9201, 'courier01', '测试快递员01', MD5('123456'), 102, 100, 'M', 1, '快递员', NOW()),
  (9202, 'courier02', '测试快递员02', MD5('123456'), 102, 100, 'M', 1, '快递员', NOW());

INSERT IGNORE INTO pd_auth.pd_auth_user_role (id, role_id, user_id, create_user, create_time) VALUES
  (920101, 643779012732130273, 9201, 3, NOW()),
  (920201, 643779012732130273, 9202, 3, NOW());

INSERT IGNORE INTO pd_base.pd_courier_scop (id, user_id, area_id) VALUES
  ('9201', '9201', '100'),
  ('9202', '9202', '100');

-- ------------------------------------------------------------
-- 4) 客户/会员 (客户小程序) +3  —— 会员表(pd_member)
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_users.pd_member (id, auth_id, phone, id_card_no, id_card_no_verify) VALUES
  ('M9301', '9301', '13800000001', '420100199001010011', 1),
  ('M9302', '9302', '13800000002', '420100199001010022', 1),
  ('M9303', '9303', '13800000003', '420100199001010033', 1);

-- ------------------------------------------------------------
-- 5) 订单 (管理端订单列表) +3  —— 关联会员
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_oms.pd_order
  (id, order_type, pickup_type, create_time, member_id,
   sender_name, sender_phone, sender_province_id, sender_city_id, sender_county_id, sender_address,
   receiver_name, receiver_phone, receiver_province_id, receiver_city_id, receiver_county_id, receiver_address,
   payment_method, payment_status, amount, status)
VALUES
  ('PJ0001', 1, 1, NOW(), 'M9301', '张三', '13800000001', '42', '4201', '420102', '武汉市江岸区中山大道1号', '李四', '13900000001', '42', '4201', '420102', '武汉市江岸区解放大道2号', 1, 1, 88.00, 1),
  ('PJ0002', 1, 1, NOW(), 'M9302', '王五', '13800000002', '42', '4201', '420102', '武汉市江汉区青年路3号', '赵六', '13900000002', '42', '4201', '420102', '武汉市江汉区新华路4号', 1, 1, 120.50, 1),
  ('PJ0003', 1, 1, NOW(), 'M9303', '钱七', '13800000003', '42', '4201', '420102', '武汉市武昌区武珞路5号', '孙八', '13900000003', '42', '4201', '420102', '武汉市武昌区中南路6号', 1, 1, 66.00, 1);

-- ------------------------------------------------------------
-- 6) 运单 (运输订单, 关联订单) +3
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_work.pd_transport_order (id, order_id, status, scheduling_status, create_time) VALUES
  ('TY0001', 'PJ0001', 1, 1, NOW()),
  ('TY0002', 'PJ0002', 1, 1, NOW()),
  ('TY0003', 'PJ0003', 1, 1, NOW());

-- ------------------------------------------------------------
-- 7) 运输任务 (运输任务列表) +2
-- ------------------------------------------------------------
INSERT IGNORE INTO pd_work.pd_task_transport
  (id, start_agency_id, end_agency_id, status, assigned_status, loading_status, plan_departure_time, plan_arrival_time, create_time)
VALUES
  ('RW0001', '100', '101', 1, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW()),
  ('RW0002', '101', '102', 1, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), NOW());

-- ============================================================
-- 汇总检查
-- ============================================================
SELECT '== 账号 ==' AS section;
SELECT account, name, work_describe FROM pd_auth.pd_auth_user;
SELECT '== 司机 ==' AS section;
SELECT COUNT(*) AS driver_cnt FROM pd_base.pd_truck_driver;
SELECT '== 快递员 ==' AS section;
SELECT COUNT(*) AS courier_cnt FROM pd_base.pd_courier_scop;
SELECT '== 会员 ==' AS section;
SELECT COUNT(*) AS member_cnt FROM pd_users.pd_member;
SELECT '== 订单 ==' AS section;
SELECT COUNT(*) AS order_cnt FROM pd_oms.pd_order;
SELECT '== 运单 ==' AS section;
SELECT COUNT(*) AS transport_order_cnt FROM pd_work.pd_transport_order;
SELECT '== 运输任务 ==' AS section;
SELECT COUNT(*) AS task_cnt FROM pd_work.pd_task_transport;
