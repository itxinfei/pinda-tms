-- ============================================================
-- 品达TMS 开发测试环境 造数脚本（2026-10-11 重写为单库 pinda_tms 口径）
-- 目标: 4端(管理/司机/快递员/客户)可登录 + 核心业务数据 ≥10条
-- 幂等: 全部使用 INSERT IGNORE(主键冲突跳过), 可重复执行
-- 用法: 口令不写进脚本，从服务器 /data/deploy/middleware/.env 取 MYSQL_ROOT_PASSWORD
--       DB_PASS=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /data/deploy/middleware/.env) \
--         && docker exec -i mysql8 mysql -uroot -p"$DB_PASS" --default-character-set=utf8mb4 pinda_tms < testdata.sql
-- 说明: 旧脚本按 pd_auth./pd_base./pd_oms./pd_work. 多库前缀编写，与本库不符（业务表一直为 0 的根因）；
--       本版统一为 pinda_tms 单库 + 新表名（sys_user/base_*/oms_*/work_*/member_*），
--       id 全部数值型（新表主键 BIGINT），密码统一 BCrypt('123456')。
-- ============================================================

-- ------------------------------------------------------------
-- 1) 账号 (sys_user, 租户 pinda=1, 密码统一 123456)
--    管理端 9001/9002，司机端 9101-9103，快递员端 9201/9202，客户端 9301-9303
-- ------------------------------------------------------------
INSERT IGNORE INTO sys_user
  (id, tenant_id, account, name, password, org_id, station_id, sex, status, work_describe, create_time, deleted)
VALUES
  (9001, 1, 'admin01',   '测试管理员01', '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '测试管理员', NOW(), 0),
  (9002, 1, 'admin02',   '测试管理员02', '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '测试管理员', NOW(), 0),
  (9101, 1, 'driver01',  '测试司机01',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '司机',      NOW(), 0),
  (9102, 1, 'driver02',  '测试司机02',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '司机',      NOW(), 0),
  (9103, 1, 'driver03',  '测试司机03',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '司机',      NOW(), 0),
  (9201, 1, 'courier01', '测试快递员01', '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '快递员',    NOW(), 0),
  (9202, 1, 'courier02', '测试快递员02', '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '快递员',    NOW(), 0),
  (9301, 1, 'customer01','测试客户01',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '客户',      NOW(), 0),
  (9302, 1, 'customer02','测试客户02',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '客户',      NOW(), 0),
  (9303, 1, 'customer03','测试客户03',   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES', 100, NULL, 1, 1, '客户',      NOW(), 0);

-- 角色：当前 sys_role 仅有 超级管理员(1)，测试账号全部挂上（开发环境口径）
INSERT IGNORE INTO sys_user_role (id, tenant_id, user_id, role_id) VALUES
  (900101, 1, 9001, 1),
  (900201, 1, 9002, 1),
  (910101, 1, 9101, 1),
  (910201, 1, 9102, 1),
  (910301, 1, 9103, 1),
  (920101, 1, 9201, 1),
  (920201, 1, 9202, 1),
  (930101, 1, 9301, 1),
  (930201, 1, 9302, 1),
  (930301, 1, 9303, 1);

-- ------------------------------------------------------------
-- 2) 司机档案 (base_truck_driver, id/user_id 均数值)
-- ------------------------------------------------------------
INSERT IGNORE INTO base_truck_driver (id, tenant_id, user_id, fleet_id, age, driving_age, deleted) VALUES
  (9101, 1, 9101, NULL, 35,  8, 0),
  (9102, 1, 9102, NULL, 30,  5, 0),
  (9103, 1, 9103, NULL, 42, 15, 0);

-- ------------------------------------------------------------
-- 3) 快递员派送范围 (base_courier_scope: courier_id + area_id=dict_area.id)
--    area_id 用真实行政区划 420102（武汉市江岸区）
-- ------------------------------------------------------------
INSERT IGNORE INTO base_courier_scope (id, tenant_id, courier_id, area_id, polygon_points) VALUES
  (9201, 1, 9201, 420102, NULL),
  (9202, 1, 9202, 420102, NULL);

-- ------------------------------------------------------------
-- 4) 会员 (member_info, 客户端登录账号 9301-9303 已建)
-- ------------------------------------------------------------
INSERT IGNORE INTO member_info (id, tenant_id, phone, nickname, id_card_no, id_card_verify, status, deleted) VALUES
  (9301, 1, '13800000001', '测试客户01', '420100199001010011', 1, 1, 0),
  (9302, 1, '13800000002', '测试客户02', '420100199001010022', 1, 1, 0),
  (9303, 1, '13800000003', '测试客户03', '420100199001010033', 1, 1, 0);

-- ------------------------------------------------------------
-- 5) 订单 (oms_order: id 数值 + order_no 保留人工单号；省市区引用 dict_area 真实 id)
--    湖北 420000 / 武汉 420100 / 江岸区 420102
-- ------------------------------------------------------------
INSERT IGNORE INTO oms_order
  (id, tenant_id, order_no, order_type, pickup_type, status, member_id,
   sender_name, sender_phone, sender_province_id, sender_city_id, sender_county_id, sender_address,
   receiver_name, receiver_phone, receiver_province_id, receiver_city_id, receiver_county_id, receiver_address,
   payment_method, payment_status, amount, create_time, deleted)
VALUES
  (9401, 1, 'PJ0001', 1, 1, 1, 9301, '张三', '13800000001', 420000, 420100, 420102, '武汉市江岸区中山大道1号', '李四', '13900000001', 420000, 420100, 420102, '武汉市江岸区解放大道2号', 1, 1, 88.00,  NOW(), 0),
  (9402, 1, 'PJ0002', 1, 1, 1, 9302, '王五', '13800000002', 420000, 420100, 420102, '武汉市江汉区青年路3号',   '赵六', '13900000002', 420000, 420100, 420102, '武汉市江汉区新华路4号',   1, 1, 120.50, NOW(), 0),
  (9403, 1, 'PJ0003', 1, 1, 1, 9303, '钱七', '13800000003', 420000, 420100, 420102, '武汉市武昌区武珞路5号',   '孙八', '13900000003', 420000, 420100, 420102, '武汉市武昌区中南路6号',   1, 1, 66.00,  NOW(), 0);

-- ------------------------------------------------------------
-- 6) 运单 (work_transport_order: order_id -> oms_order.id)
-- ------------------------------------------------------------
INSERT IGNORE INTO work_transport_order (id, tenant_id, order_id, status, scheduling_status, create_time, deleted) VALUES
  (9501, 1, 9401, 1, 1, NOW(), 0),
  (9502, 1, 9402, 1, 1, NOW(), 0),
  (9503, 1, 9403, 1, 1, NOW(), 0);

-- ------------------------------------------------------------
-- 7) 干线运输任务 (work_task_transport: start/end_org_id -> sys_org.id)
-- ------------------------------------------------------------
INSERT IGNORE INTO work_task_transport
  (id, tenant_id, start_org_id, end_org_id, status, assigned_status, loading_status, plan_departure_time, plan_arrival_time, create_time, deleted)
VALUES
  (9601, 1, 100, 101, 1, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 2 HOUR), NOW(), 0),
  (9602, 1, 101, 102, 1, 1, 1, NOW(), DATE_ADD(NOW(), INTERVAL 3 HOUR), NOW(), 0);

-- ============================================================
-- 汇总检查
-- ============================================================
SELECT '== 账号 ==' AS section;
SELECT account, name, work_describe FROM sys_user WHERE id >= 9000;
SELECT '== 司机 ==' AS section;
SELECT COUNT(*) AS driver_cnt FROM base_truck_driver;
SELECT '== 快递员范围 ==' AS section;
SELECT COUNT(*) AS courier_scope_cnt FROM base_courier_scope;
SELECT '== 会员 ==' AS section;
SELECT COUNT(*) AS member_cnt FROM member_info;
SELECT '== 订单 ==' AS section;
SELECT id, order_no, member_id, amount, status FROM oms_order;
SELECT '== 运单 ==' AS section;
SELECT id, order_id, status FROM work_transport_order;
SELECT '== 运输任务 ==' AS section;
SELECT id, start_org_id, end_org_id, status FROM work_task_transport;
