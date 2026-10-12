-- =====================================================================
-- 🔴 DEV-ONLY 演示种子：运营监控大屏 + 工作台仪表盘
-- 日期：2026-10-12
-- 用途：空库执行 09_seed.sql（租户1/根组织100/admin）后，本脚本制造
--       近 7 天订单/运单/运输任务、车辆、线路、GPS 点、告警的演示数据，
--       使 /dashboard/bigscreen 与工作台全部面板有真实数据可看。
-- 约束：
--   1) 仅可在 dev 环境（192.168.20.130 内网虚拟机 pinda_tms 库）执行；
--   2) 固定可读 ID（非雪花），专用 ID 段，执行前先按段 DELETE 旧种子，
--      可无限次重复执行；时间相对 CURDATE()/NOW() 生成，每次执行自动
--      刷新为"今天/近 7 天"，不会累积重复数据；
--   3) 与业务新增数据不冲突：业务 ID 走雪花（18~19 位长整型），
--      本脚本全部为短 ID（100~93999 段）；
--   4) 严禁在生产环境执行；严禁纳入自动迁移（Flyway/Liquibase）。
--
-- 执行（Git Bash 示例，密码 123456）：
--   mysql -h192.168.20.130 -uroot -p123456 pinda_tms < docs/sql/演示种子_大屏与仪表盘_20261012.sql
--
-- 数据规模与预期面板结果（执行当日，本种子贡献口径）：
--   今日订单 6，今日运单 6，今日运输任务 6，今日签收 2；
--   在途车辆 4（任务 status=2），待执行车辆 3（status=1/3），空闲车辆 5（共 12 台）；
--   运输任务五态：待执行2 / 进行中4 / 待确认1 / 已完成10 / 已取消2；
--   线路运量 Top（按车次归并到线路）：京沪5 / 京广5 / 沪汉3 / 粤蓉2 / 蓉陕2 / 京沈2；
--   未处理告警 4（共 6 条）；地图 12 个城市车辆点；近 7 天订单/签收曲线。
--   注：若库内已存在同租户其他数据，面板显示为叠加值（如 2026-10-11 晚
--   残留的 PJ0001~3 测试单与 9601/9602 测试任务，会使待处理订单/待执行
--   任务略增；它们不挂车次/车辆，不影响今日 KPI、车辆与运量口径）。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- 租户固定为 1（09_seed.sql：品达物流）
SET @tenant_id := 1;

-- ---------------------------------------------------------------------
-- 0. 清理旧种子（按固定 ID 段，均带 WHERE；先子表后父表）
-- ---------------------------------------------------------------------
DELETE FROM `iot_alarm_record`      WHERE `id` BETWEEN 93001 AND 93999;
DELETE FROM `iot_truck_location`    WHERE `id` BETWEEN 92001 AND 92999;
DELETE FROM `oms_order_cargo`       WHERE `id` BETWEEN 91001 AND 91999;
DELETE FROM `work_transport_order_task` WHERE `id` BETWEEN 8501 AND 8599;
DELETE FROM `work_transport_order`  WHERE `id` BETWEEN 801 AND 899;
DELETE FROM `work_task_transport`   WHERE `id` BETWEEN 701 AND 799;
DELETE FROM `base_truck`            WHERE `id` BETWEEN 501 AND 599;
DELETE FROM `base_fleet`            WHERE `id` = 610;
DELETE FROM `base_truck_type`       WHERE `id` = 601;
DELETE FROM `base_transport_trips`  WHERE `id` BETWEEN 401 AND 499;
DELETE FROM `base_transport_line`   WHERE `id` BETWEEN 301 AND 399;
DELETE FROM `sys_org`               WHERE `id` BETWEEN 201 AND 299;
DELETE FROM `oms_order`             WHERE `id` BETWEEN 90001 AND 90199;

-- ---------------------------------------------------------------------
-- 1. 机构：7 个一级转运中心（sys_org，org_type=2，挂根组织 100）
-- ---------------------------------------------------------------------
INSERT INTO `sys_org`
  (`id`,`tenant_id`,`name`,`abbreviation`,`parent_id`,`org_type`,
   `province_id`,`city_id`,`county_id`,`address`,`contract_number`,`manager_id`,
   `tree_path`,`sort_value`,`status`,`longitude`,`latitude`,`business_hours`,`description`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (201,@tenant_id,'北京转运中心','北京中心',100,2,NULL,NULL,NULL,'北京市大兴区黄徐路物流园','010-58001001',NULL,',100,',10,1,116.403000,39.915000,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (202,@tenant_id,'上海转运中心','上海中心',100,2,NULL,NULL,NULL,'上海市青浦区华新镇物流大道','021-59801002',NULL,',100,',20,1,121.473700,31.230400,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (203,@tenant_id,'广州转运中心','广州中心',100,2,NULL,NULL,NULL,'广州市白云区石井物流园','020-86001003',NULL,',100,',30,1,113.264400,23.129100,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (204,@tenant_id,'成都转运中心','成都中心',100,2,NULL,NULL,NULL,'成都市新都区物流大道','028-83001004',NULL,',100,',40,1,104.066800,30.572800,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (205,@tenant_id,'武汉转运中心','武汉中心',100,2,NULL,NULL,NULL,'武汉市东西湖区吴家山物流园','027-83001005',NULL,',100,',50,1,114.305500,30.592800,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (206,@tenant_id,'西安转运中心','西安中心',100,2,NULL,NULL,NULL,'西安市灞桥区国际港务区','029-83001006',NULL,',100,',60,1,108.939800,34.341600,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0),
  (207,@tenant_id,'沈阳转运中心','沈阳中心',100,2,NULL,NULL,NULL,'沈阳市于洪区沈北物流园','024-86001007',NULL,',100,',70,1,123.432800,41.805700,'00:00-24:00','演示种子-一级转运中心',NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 2. 车型 / 车队
-- ---------------------------------------------------------------------
INSERT INTO `base_truck_type`
  (`id`,`tenant_id`,`name`,`allowable_load`,`allowable_volume`,
   `measure_long`,`measure_width`,`measure_high`,`status`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (601,@tenant_id,'中型厢式货车',5000.00,30.00,6.80,2.40,2.60,1,NULL,NOW(),NULL,NOW(),0);

INSERT INTO `base_fleet`
  (`id`,`tenant_id`,`name`,`fleet_number`,`org_id`,`manager`,`status`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (610,@tenant_id,'品达自营车队','FLEET01',100,'王建国',1,NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 3. 线路（6 条干线）
-- ---------------------------------------------------------------------
INSERT INTO `base_transport_line`
  (`id`,`tenant_id`,`name`,`line_number`,`org_id`,`transport_line_type_id`,
   `start_org_id`,`end_org_id`,`distance`,`cost`,`estimated_time`,`status`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (301,@tenant_id,'京沪干线','L-BJ-SH',100,NULL,201,202,1213.00,4200.00,900,1,NULL,NOW(),NULL,NOW(),0),
  (302,@tenant_id,'京广干线','L-BJ-GZ',100,NULL,201,203,2120.00,7200.00,1500,1,NULL,NOW(),NULL,NOW(),0),
  (303,@tenant_id,'沪汉干线','L-SH-WH',100,NULL,202,205,830.00,2900.00,660,1,NULL,NOW(),NULL,NOW(),0),
  (304,@tenant_id,'粤蓉干线','L-GZ-CD',100,NULL,203,204,1650.00,5600.00,1200,1,NULL,NOW(),NULL,NOW(),0),
  (305,@tenant_id,'蓉陕干线','L-CD-XA',100,NULL,204,206,710.00,2500.00,560,1,NULL,NOW(),NULL,NOW(),0),
  (306,@tenant_id,'京沈干线','L-BJ-SY',100,NULL,201,207,685.00,2400.00,540,1,NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 4. 车次（每条线路 1 个固定班次，period=1 每天）
-- ---------------------------------------------------------------------
INSERT INTO `base_transport_trips`
  (`id`,`tenant_id`,`name`,`departure_time`,`transport_line_id`,`period`,`status`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (401,@tenant_id,'京沪早班','08:00',301,1,1,NULL,NOW(),NULL,NOW(),0),
  (402,@tenant_id,'京广早班','07:30',302,1,1,NULL,NOW(),NULL,NOW(),0),
  (403,@tenant_id,'沪汉午班','12:00',303,1,1,NULL,NOW(),NULL,NOW(),0),
  (404,@tenant_id,'粤蓉早班','08:30',304,1,1,NULL,NOW(),NULL,NOW(),0),
  (405,@tenant_id,'蓉陕午班','13:00',305,1,1,NULL,NOW(),NULL,NOW(),0),
  (406,@tenant_id,'京沈早班','09:00',306,1,1,NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 5. 车辆（12 台，全 status=1 正常；其中在途4/待执行3/空闲5）
-- ---------------------------------------------------------------------
INSERT INTO `base_truck`
  (`id`,`tenant_id`,`truck_type_id`,`fleet_id`,`brand`,`license_plate`,`device_gps_id`,
   `allowable_load`,`allowable_volume`,`truck_license_id`,`status`,
   `online_status`,`last_heartbeat_time`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (501,@tenant_id,601,610,'东风天锦','京AF5012','GPS501',5000.00,30.00,NULL,1,1,NOW(),NULL,NOW(),NULL,NOW(),0),
  (502,@tenant_id,601,610,'解放J6','京AF5036','GPS502',5000.00,30.00,NULL,1,1,NOW(),NULL,NOW(),NULL,NOW(),0),
  (503,@tenant_id,601,610,'东风天龙','沪BD3188','GPS503',5000.00,30.00,NULL,1,1,NOW(),NULL,NOW(),NULL,NOW(),0),
  (504,@tenant_id,601,610,'福田欧曼','京AF5266','GPS504',5000.00,30.00,NULL,1,1,NOW(),NULL,NOW(),NULL,NOW(),0),
  (505,@tenant_id,601,610,'东风天锦','京AF5510','GPS505',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (506,@tenant_id,601,610,'解放J6','沪BD3266','GPS506',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (507,@tenant_id,601,610,'福田欧曼','粤A7K026','GPS507',5000.00,30.00,NULL,1,1,NOW(),NULL,NOW(),NULL,NOW(),0),
  (508,@tenant_id,601,610,'东风天龙','川A6D088','GPS508',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (509,@tenant_id,601,610,'解放J6','鄂A8K316','GPS509',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (510,@tenant_id,601,610,'福田欧曼','陕A3F662','GPS510',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (511,@tenant_id,601,610,'东风天锦','辽A5D908','GPS511',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0),
  (512,@tenant_id,601,610,'解放J6','浙A2K773','GPS512',5000.00,30.00,NULL,1,0,NULL,NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 6. 运输任务 work_task_transport（19 条，覆盖五态与近 7 天）
--    status 1待执行 2进行中 3待确认 4已完成 5已取消
--    assigned_status 1未分配 2已分配；loading_status 1半载 2满载 3空载
-- ---------------------------------------------------------------------
INSERT INTO `work_task_transport`
  (`id`,`tenant_id`,`trips_id`,`start_org_id`,`end_org_id`,`status`,
   `assigned_status`,`loading_status`,`truck_id`,
   `plan_departure_time`,`actual_departure_time`,`plan_arrival_time`,
   `actual_arrival_time`,`actual_delivery_time`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  -- 今日：4 条进行中（在途车辆 501-504）
  (701,@tenant_id,401,201,202,2,2,1,501,
   CURDATE()+INTERVAL 8 HOUR, CURDATE()+INTERVAL 8 HOUR+INTERVAL 15 MINUTE,
   CURDATE()+INTERVAL 18 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 7 HOUR+INTERVAL 50 MINUTE, NULL, NOW(), 0),
  (702,@tenant_id,402,201,203,2,2,2,502,
   CURDATE()+INTERVAL 7 HOUR+INTERVAL 30 MINUTE, CURDATE()+INTERVAL 7 HOUR+INTERVAL 40 MINUTE,
   DATE_ADD(CURDATE(), INTERVAL 1 DAY)+INTERVAL 22 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 7 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (703,@tenant_id,403,202,205,2,2,1,503,
   CURDATE()+INTERVAL 12 HOUR, CURDATE()+INTERVAL 12 HOUR+INTERVAL 10 MINUTE,
   CURDATE()+INTERVAL 23 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 11 HOUR+INTERVAL 30 MINUTE, NULL, NOW(), 0),
  (704,@tenant_id,401,201,202,2,2,2,504,
   CURDATE()+INTERVAL 9 HOUR, CURDATE()+INTERVAL 9 HOUR+INTERVAL 5 MINUTE,
   CURDATE()+INTERVAL 19 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 8 HOUR+INTERVAL 40 MINUTE, NULL, NOW(), 0),
  -- 今日：2 条待执行（505/506），计划明日班次
  (705,@tenant_id,401,201,202,1,1,3,505,
   DATE_ADD(CURDATE(), INTERVAL 1 DAY)+INTERVAL 8 HOUR, NULL,
   DATE_ADD(CURDATE(), INTERVAL 1 DAY)+INTERVAL 18 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 9 HOUR, NULL, NOW(), 0),
  (706,@tenant_id,406,201,207,1,1,3,506,
   DATE_ADD(CURDATE(), INTERVAL 1 DAY)+INTERVAL 9 HOUR, NULL,
   DATE_ADD(CURDATE(), INTERVAL 1 DAY)+INTERVAL 18 HOUR, NULL, NULL,
   NULL, CURDATE()+INTERVAL 9 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  -- 昨日：1 条待确认（507，已到场待回单确认）
  (707,@tenant_id,402,201,203,3,2,1,507,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 22 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 21 HOUR+INTERVAL 50 MINUTE, NULL,
   NULL, DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  -- 近 6 天：10 条已完成（trips 分布制造柱状梯度：401×2 / 402×3 / 403×2 / 404×1 / 405×1 / 406×1）
  (708,@tenant_id,401,201,202,4,2,2,501,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 8 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 8 HOUR+INTERVAL 15 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 18 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 17 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 17 HOUR+INTERVAL 45 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 50 MINUTE, NULL, NOW(), 0),
  (709,@tenant_id,401,201,202,4,2,2,504,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 8 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 8 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 18 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 17 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 17 HOUR+INTERVAL 35 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 7 HOUR+INTERVAL 50 MINUTE, NULL, NOW(), 0),
  (710,@tenant_id,402,201,203,4,2,1,502,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 22 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 21 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 21 HOUR+INTERVAL 40 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 7 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (711,@tenant_id,402,201,203,4,2,2,507,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 7 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 7 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 22 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 21 HOUR+INTERVAL 20 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 21 HOUR+INTERVAL 30 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 7 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (712,@tenant_id,402,201,203,4,2,1,502,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 7 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 7 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 22 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 21 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 21 HOUR+INTERVAL 20 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 7 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (713,@tenant_id,403,202,205,4,2,2,503,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 12 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 12 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 23 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 22 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 22 HOUR+INTERVAL 40 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 11 HOUR+INTERVAL 30 MINUTE, NULL, NOW(), 0),
  (714,@tenant_id,403,202,205,4,2,1,509,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 12 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 12 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 23 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 22 HOUR+INTERVAL 20 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 22 HOUR+INTERVAL 30 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 11 HOUR+INTERVAL 30 MINUTE, NULL, NOW(), 0),
  (715,@tenant_id,404,203,204,4,2,2,508,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 8 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 8 HOUR+INTERVAL 40 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 20 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 19 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 19 HOUR+INTERVAL 40 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 8 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (716,@tenant_id,405,204,206,4,2,1,510,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 13 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 13 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 22 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 21 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 21 HOUR+INTERVAL 40 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 12 HOUR+INTERVAL 30 MINUTE, NULL, NOW(), 0),
  (717,@tenant_id,406,201,207,4,2,2,511,
   DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 9 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 9 HOUR+INTERVAL 10 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 18 HOUR,
   DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 17 HOUR+INTERVAL 30 MINUTE,
   DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 17 HOUR+INTERVAL 40 MINUTE,
   NULL, DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 8 HOUR+INTERVAL 50 MINUTE, NULL, NOW(), 0),
  -- 已取消 2 条（无车）
  (718,@tenant_id,404,203,204,5,1,3,NULL,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 8 HOUR+INTERVAL 30 MINUTE, NULL,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 20 HOUR, NULL, NULL,
   NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 8 HOUR+INTERVAL 20 MINUTE, NULL, NOW(), 0),
  (719,@tenant_id,405,204,206,5,1,3,NULL,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 13 HOUR, NULL,
   DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 22 HOUR, NULL, NULL,
   NULL, DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 12 HOUR+INTERVAL 30 MINUTE, NULL, NOW(), 0);

-- ---------------------------------------------------------------------
-- 7. 订单 oms_order（共 32 单：历史 24 + 今日 8）
--    status: 1待取件 2已取件 3网点自寄 4网点入库 5待装车 6运输中
--            7网点出库 8待派送 9派送中 10已签收 11拒收 12已取消
--    下单量按 create_time 归属日；签收量按 update_time 当日 + status=10
-- ---------------------------------------------------------------------
-- 7.1 近 6 天历史订单（90001-90024）
INSERT INTO `oms_order`
  (`id`,`tenant_id`,`order_no`,`order_type`,`pickup_type`,`status`,`member_id`,
   `sender_name`,`sender_phone`,`sender_address`,
   `receiver_name`,`receiver_phone`,`receiver_address`,`current_org_id`,
   `payment_method`,`payment_status`,`amount`,`distance`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  -- n=1 昨天：签收2 / 运输中1 / 已取件1
  (90001,@tenant_id,CONCAT('PD',90001),2,2,10,NULL,'张晓明','13900010001','北京市海淀区中关村大街1号','李华','13900020001','上海市浦东新区张江路100号',202,1,2,86.00,1213.00,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 16 HOUR,0),
  (90002,@tenant_id,CONCAT('PD',90002),1,2,10,NULL,'王芳','13900010002','北京市朝阳区建国路88号','赵静','13900020002','北京市西城区西单北大街10号',201,2,2,20.00,12.00,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 10 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 17 HOUR,0),
  (90003,@tenant_id,CONCAT('PD',90003),2,2,6,NULL,'陈强','13900010003','上海市闵行区莘庄镇','刘洋','13900020003','武汉市武昌区中南路',205,1,2,128.00,830.00,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 11 HOUR,NULL,NOW(),0),
  (90004,@tenant_id,CONCAT('PD',90004),1,2,2,NULL,'孙丽','13900010004','上海市徐汇区漕溪北路','周杰','13900020004','上海市静安区南京西路',202,1,2,32.00,18.00,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 14 HOUR,NULL,NOW(),0),
  -- n=2：签收1 / 派送中1 / 待取件1（另 1 单今日签收 90106 也在这天下单）
  (90005,@tenant_id,CONCAT('PD',90005),2,2,10,NULL,'吴敏','13900010005','广州市天河区天河路200号','郑爽','13900020005','北京市丰台区丰台北路',201,1,2,168.00,2120.00,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 16 HOUR,0),
  (90006,@tenant_id,CONCAT('PD',90006),2,2,10,NULL,'朱军','13900010006','广州市越秀区中山五路','冯磊','13900020006','北京市通州区运河大街',201,2,2,152.00,2100.00,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 10 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 17 HOUR,0),
  (90007,@tenant_id,CONCAT('PD',90007),2,2,9,NULL,'何静','13900010007','上海市嘉定区城北路','许晴','13900020007','武汉市洪山区珞瑜路',205,1,2,98.00,820.00,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 11 HOUR,NULL,NOW(),0),
  (90008,@tenant_id,CONCAT('PD',90008),1,1,1,NULL,'高翔','13900010008','广州市白云区机场路','林青','13900020008','广州市海珠区新港中路',203,2,1,20.00,25.00,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 15 HOUR,NULL,NOW(),0),
  -- n=3：签收3 / 网点出库1 / 待装车1
  (90009,@tenant_id,CONCAT('PD',90009),2,2,10,NULL,'罗琳','13900010009','成都市武侯区人民南路','马东','13900020009','广州市天河区体育西路',203,1,2,142.00,1650.00,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 16 HOUR,0),
  (90010,@tenant_id,CONCAT('PD',90010),2,2,10,NULL,'谢楠','13900010010','成都市锦江区春熙路','黄磊','13900020010','广州市越秀区环市东路',203,2,2,135.00,1640.00,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 10 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 17 HOUR,0),
  (90011,@tenant_id,CONCAT('PD',90011),2,2,10,NULL,'韩雪','13900010011','北京市西城区金融街','袁泉','13900020011','广州市番禺区市桥街',203,1,2,178.00,2130.00,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 11 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 18 HOUR,0),
  (90012,@tenant_id,CONCAT('PD',90012),2,2,7,NULL,'曹颖','13900010012','武汉市江汉区解放大道','董洁','13900020012','上海市黄浦区人民大道',202,1,2,88.00,830.00,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 13 HOUR,NULL,NOW(),0),
  (90013,@tenant_id,CONCAT('PD',90013),1,2,5,NULL,'彭于','13900010013','成都市成华区双林路','徐帆', '13900020013','成都市青羊区宽窄巷子',204,2,1,26.00,15.00,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 15 HOUR,NULL,NOW(),0),
  -- n=4：签收1 / 运输中1 / 网点自寄1 / 已取消1
  (90014,@tenant_id,CONCAT('PD',90014),2,2,10,NULL,'苏有','13900010014','西安市雁塔区小寨东路','邓超','13900020014','成都市高新区天府大道',204,1,2,76.00,710.00,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 16 HOUR,0),
  (90015,@tenant_id,CONCAT('PD',90015),2,2,6,NULL,'雷佳','13900010015','广州市黄埔区科学大道','沈腾','13900020015','成都市双流区机场路',204,2,2,130.00,1660.00,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 10 HOUR,NULL,NOW(),0),
  (90016,@tenant_id,CONCAT('PD',90016),1,1,3,NULL,'黄渤','13900010016','北京市丰台区花乡物流园','吴京','13900020016','北京市大兴区亦庄',201,2,1,20.00,30.00,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 11 HOUR,NULL,NOW(),0),
  (90017,@tenant_id,CONCAT('PD',90017),1,2,12,NULL,'汤唯','13900010017','西安市碑林区南大街','陈坤','13900020017','西安市莲湖区北大街',206,1,2,20.00,8.00,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 14 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 4 DAY)+INTERVAL 15 HOUR,0),
  -- n=5：签收2 / 待派送1 / 网点入库1
  (90018,@tenant_id,CONCAT('PD',90018),2,2,10,NULL,'周迅','13900010018','上海市长宁区虹桥路','孙俪','13900020018','武汉市江岸区沿江大道',205,1,2,92.00,840.00,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 16 HOUR,0),
  (90019,@tenant_id,CONCAT('PD',90019),2,2,10,NULL,'赵薇','13900010019','武汉市汉阳区汉阳大道','姚晨','13900020019','上海市长宁区天山路',202,2,2,96.00,835.00,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 10 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 17 HOUR,0),
  (90020,@tenant_id,CONCAT('PD',90020),2,2,8,NULL,'徐峥','13900010020','沈阳市和平区太原街','王迅','13900020020','北京市朝阳区望京',201,1,2,110.00,685.00,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 11 HOUR,NULL,NOW(),0),
  (90021,@tenant_id,CONCAT('PD',90021),1,1,4,NULL,'王宝强','13900010021','上海市普陀区真北路','张艺兴','13900020021','上海市宝山区沪太路',202,2,1,30.00,22.00,NULL,DATE_SUB(CURDATE(), INTERVAL 5 DAY)+INTERVAL 15 HOUR,NULL,NOW(),0),
  -- n=6：签收2 / 运输中1
  (90022,@tenant_id,CONCAT('PD',90022),2,2,10,NULL,'李现','13900010022','沈阳市铁西区建设大路','杨紫','13900020022','北京市东城区东直门外',201,1,2,105.00,690.00,NULL,DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 9 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 16 HOUR,0),
  (90023,@tenant_id,CONCAT('PD',90023),2,2,10,NULL,'肖战','13900010023','北京市石景山区鲁谷路','王一博','13900020023','沈阳市皇姑区长江街',207,2,2,108.00,680.00,NULL,DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 10 HOUR,NULL,DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 17 HOUR,0),
  (90024,@tenant_id,CONCAT('PD',90024),2,2,6,NULL,'朱一龙','13900010024','西安市未央区凤城八路','赵丽颖','13900020024','成都市金牛区荷花池',204,1,2,80.00,715.00,NULL,DATE_SUB(CURDATE(), INTERVAL 6 DAY)+INTERVAL 11 HOUR,NULL,NOW(),0);

-- 7.2 今日订单（90101-90108，其中两单签收为近两天下单今日签收）
INSERT INTO `oms_order`
  (`id`,`tenant_id`,`order_no`,`order_type`,`pickup_type`,`status`,`member_id`,
   `sender_name`,`sender_phone`,`sender_address`,
   `receiver_name`,`receiver_phone`,`receiver_address`,`current_org_id`,
   `payment_method`,`payment_status`,`amount`,`distance`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (90101,@tenant_id,CONCAT('PD',90101),1,2,1,NULL,'梅林','13900030001','北京市东城区安定门外大街','陶红','13900040001','北京市朝阳区酒仙桥路',201,2,1,20.00,14.00,NULL,NOW()-INTERVAL 7 HOUR,NULL,NOW(),0),
  (90102,@tenant_id,CONCAT('PD',90102),1,2,2,NULL,'段奕','13900030002','北京市西城区德胜门外大街','秦海','13900040002','北京市海淀区上地十街',201,1,2,28.00,19.00,NULL,NOW()-INTERVAL 6 HOUR,NULL,NOW(),0),
  (90103,@tenant_id,CONCAT('PD',90103),2,2,5,NULL,'陈数','13900030003','上海市浦东新区世纪大道','袁弘','13900040003','上海市青浦区盈港路',202,1,2,78.00,45.00,NULL,NOW()-INTERVAL 5 HOUR,NULL,NOW(),0),
  (90104,@tenant_id,CONCAT('PD',90104),2,2,6,NULL,'刘敏','13900030004','北京市通州区新华大街','胡歌','13900040004','上海市闵行区莘建路',202,1,2,118.00,1210.00,NULL,NOW()-INTERVAL 4 HOUR,NULL,NOW(),0),
  (90105,@tenant_id,CONCAT('PD',90105),2,2,9,NULL,'雷音','13900030005','武汉市洪山区关山大道','靳东','13900040005','武汉市江夏区文华路',205,2,2,46.00,28.00,NULL,NOW()-INTERVAL 3 HOUR,NULL,NOW(),0),
  (90106,@tenant_id,CONCAT('PD',90106),2,2,10,NULL,'王凯','13900030006','广州市白云区齐心路','张嘉','13900040006','北京市顺义区府前东街',201,2,2,160.00,2110.00,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 9 HOUR,NULL,NOW()-INTERVAL 1 HOUR,0),
  (90107,@tenant_id,CONCAT('PD',90107),2,2,10,NULL,'吴磊','13900030007','上海市静安区共和新路','刘诗','13900040007','武汉市江汉区常青路',205,1,2,84.00,825.00,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 10 HOUR,NULL,NOW()-INTERVAL 2 HOUR,0),
  (90108,@tenant_id,CONCAT('PD',90108),1,2,12,NULL,'白宇','13900030008','北京市房山区良乡西路','朱一','13900040008','北京市门头沟区新桥大街',201,1,2,20.00,35.00,NULL,NOW()-INTERVAL 8 HOUR,NULL,NOW()-INTERVAL 7 HOUR,0);

-- ---------------------------------------------------------------------
-- 8. 订单货物明细（今日 8 单各一条）
-- ---------------------------------------------------------------------
INSERT INTO `oms_order_cargo`
  (`id`,`tenant_id`,`order_id`,`transport_order_id`,`goods_type_id`,
   `name`,`unit`,`cargo_value`,`cargo_barcode`,`quantity`,
   `volume`,`weight`,`total_volume`,`total_weight`,`remark`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (91001,@tenant_id,90101,NULL,NULL,'商务文件','份',200.00,NULL,1,0.0020,0.500,0.0020,0.500,'加急件',NULL,NOW(),NULL,NOW(),0),
  (91002,@tenant_id,90102,NULL,NULL,'电子产品配件','箱',2000.00,NULL,1,0.0300,3.000,0.0300,3.000,'防潮',NULL,NOW(),NULL,NOW(),0),
  (91003,@tenant_id,90103,NULL,NULL,'服装百货','袋',800.00,NULL,2,0.0600,6.000,0.1200,12.000,NULL,NULL,NOW(),NULL,NOW(),0),
  (91004,@tenant_id,90104,NULL,NULL,'精密仪器','箱',15000.00,NULL,1,0.0800,8.000,0.0800,8.000,'易碎轻放',NULL,NOW(),NULL,NOW(),0),
  (91005,@tenant_id,90105,NULL,NULL,'生鲜食品','箱',600.00,NULL,3,0.0400,5.000,0.1200,15.000,'冷链时效',NULL,NOW(),NULL,NOW(),0),
  (91006,@tenant_id,90106,NULL,NULL,'汽配零件','箱',3000.00,NULL,2,0.1000,18.000,0.2000,36.000,NULL,NULL,NOW(),NULL,NOW(),0),
  (91007,@tenant_id,90107,NULL,NULL,'家居用品','件',1200.00,NULL,1,0.1500,12.000,0.1500,12.000,NULL,NULL,NOW(),NULL,NOW(),0),
  (91008,@tenant_id,90108,NULL,NULL,'书籍资料','箱',300.00,NULL,1,0.0200,4.000,0.0200,4.000,'客户取消',NULL,NOW(),NULL,NOW(),0);

-- ---------------------------------------------------------------------
-- 9. 运单 work_transport_order（12 条；今日新增 801-805、808 共 6 条）
--    status 1新建 2已装车 3到达 4到达终端网点 5已签收 6拒收
--    scheduling_status 1待调度 2未匹配线路 3已调度
-- ---------------------------------------------------------------------
INSERT INTO `work_transport_order`
  (`id`,`tenant_id`,`order_id`,`status`,`scheduling_status`,
   `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
  (801,@tenant_id,90101,1,1,NULL,NOW(),NULL,NOW(),0),
  (802,@tenant_id,90102,1,3,NULL,NOW(),NULL,NOW(),0),
  (803,@tenant_id,90103,1,1,NULL,NOW(),NULL,NOW(),0),
  (804,@tenant_id,90104,2,3,NULL,NOW(),NULL,NOW(),0),
  (805,@tenant_id,90105,4,3,NULL,NOW(),NULL,NOW(),0),
  (806,@tenant_id,90106,5,3,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 9 HOUR+INTERVAL 30 MINUTE,NULL,NOW()-INTERVAL 1 HOUR,0),
  (807,@tenant_id,90107,5,3,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 10 HOUR+INTERVAL 30 MINUTE,NULL,NOW()-INTERVAL 2 HOUR,0),
  (808,@tenant_id,90108,1,1,NULL,NOW(),NULL,NOW(),0),
  (809,@tenant_id,90001,5,3,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 9 HOUR+INTERVAL 30 MINUTE,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 16 HOUR,0),
  (810,@tenant_id,90005,5,3,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 9 HOUR+INTERVAL 30 MINUTE,NULL,DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 16 HOUR,0),
  (811,@tenant_id,90009,5,3,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 9 HOUR+INTERVAL 30 MINUTE,NULL,DATE_SUB(CURDATE(), INTERVAL 3 DAY)+INTERVAL 16 HOUR,0),
  (812,@tenant_id,90004,2,3,NULL,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 14 HOUR+INTERVAL 30 MINUTE,NULL,NOW(),0);

-- 9.1 运单-运输任务关联（6 条演示）
INSERT INTO `work_transport_order_task`
  (`id`,`tenant_id`,`transport_order_id`,`task_id`)
VALUES
  (8501,@tenant_id,804,701),
  (8502,@tenant_id,805,702),
  (8503,@tenant_id,806,708),
  (8504,@tenant_id,807,710),
  (8505,@tenant_id,803,705),
  (8506,@tenant_id,801,706);

-- ---------------------------------------------------------------------
-- 10. GPS 定位 iot_truck_location（12 台车辆各 1 个最新点，BD09 城市级）
--     business_type=1 车辆；大屏 /trace/page?type=truck 按车去重取最新点
-- ---------------------------------------------------------------------
INSERT INTO `iot_truck_location`
  (`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,
   `longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,
   `report_time`,`create_time`)
VALUES
  (92001,@tenant_id,501,1,'王建国','13800050001','京AF5012',117.201000,39.084200,3,'MOBILE','品达自营车队',701,NOW()-INTERVAL 4 MINUTE,NOW()),
  (92002,@tenant_id,502,1,'李志强','13800050002','京AF5036',113.625350,34.746610,3,'MOBILE','品达自营车队',702,NOW()-INTERVAL 3 MINUTE,NOW()),
  (92003,@tenant_id,503,1,'张海涛','13800050003','沪BD3188',117.227200,31.820580,3,'MOBILE','品达自营车队',703,NOW()-INTERVAL 2 MINUTE,NOW()),
  (92004,@tenant_id,504,1,'刘鹏','13800050004','京AF5266',116.857460,38.310580,3,'MOBILE','品达自营车队',704,NOW()-INTERVAL 5 MINUTE,NOW()),
  (92005,@tenant_id,505,1,'陈伟','13800050005','京AF5510',116.403000,39.915000,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 30 MINUTE,NOW()),
  (92006,@tenant_id,506,1,'赵磊','13800050006','沪BD3266',121.473700,31.230400,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 28 MINUTE,NOW()),
  (92007,@tenant_id,507,1,'孙浩','13800050007','粤A7K026',113.264400,23.129100,3,'MOBILE','品达自营车队',707,NOW()-INTERVAL 12 MINUTE,NOW()),
  (92008,@tenant_id,508,1,'周勇','13800050008','川A6D088',104.066800,30.572800,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 45 MINUTE,NOW()),
  (92009,@tenant_id,509,1,'吴刚','13800050009','鄂A8K316',114.305500,30.592800,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 50 MINUTE,NOW()),
  (92010,@tenant_id,510,1,'郑斌','13800050010','陕A3F662',108.939800,34.341600,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 60 MINUTE,NOW()),
  (92011,@tenant_id,511,1,'马超','13800050011','辽A5D908',123.432800,41.805700,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 70 MINUTE,NOW()),
  (92012,@tenant_id,512,1,'黄勇','13800050012','浙A2K773',120.155100,30.274100,3,'MOBILE','品达自营车队',NULL,NOW()-INTERVAL 80 MINUTE,NOW());

-- ---------------------------------------------------------------------
-- 11. 告警 iot_alarm_record（6 条：4 未处理 + 2 已处理）
--     alarm_type 1超速 2长停留 3偏离 4离线；13车辆故障 15延误（人工）
-- ---------------------------------------------------------------------
INSERT INTO `iot_alarm_record`
  (`id`,`tenant_id`,`alarm_type`,`transport_task_id`,`truck_id`,`driver_id`,
   `longitude`,`latitude`,`alarm_content`,`alarm_time`,`status`,`create_time`)
VALUES
  (93001,@tenant_id,1,702,502,NULL,113.625350,34.746610,'行驶速度 118km/h，超过路段限速 100km/h',
   CURDATE()+INTERVAL 9 HOUR+INTERVAL 12 MINUTE,0,CURDATE()+INTERVAL 9 HOUR+INTERVAL 12 MINUTE),
  (93002,@tenant_id,2,703,503,NULL,117.227200,31.820580,'车辆在合肥中转场停留超过 90 分钟，疑似异常滞留',
   CURDATE()+INTERVAL 10 HOUR+INTERVAL 5 MINUTE,0,CURDATE()+INTERVAL 10 HOUR+INTERVAL 5 MINUTE),
  (93003,@tenant_id,3,701,501,NULL,116.857460,38.310580,'车辆偏离京沪规划路线约 2.3 公里，请确认绕行原因',
   CURDATE()+INTERVAL 8 HOUR+INTERVAL 40 MINUTE,0,CURDATE()+INTERVAL 8 HOUR+INTERVAL 40 MINUTE),
  (93004,@tenant_id,13,NULL,508,NULL,104.066800,30.572800,'车辆制动系统故障，司机已停靠成都维修站等待处理',
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 16 HOUR+INTERVAL 20 MINUTE,0,
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 16 HOUR+INTERVAL 20 MINUTE),
  (93005,@tenant_id,4,NULL,510,NULL,108.939800,34.341600,'GPS 设备离线超过 60 分钟，最后位置西安国际港务区',
   DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 14 HOUR,1,DATE_SUB(CURDATE(), INTERVAL 1 DAY)+INTERVAL 14 HOUR),
  (93006,@tenant_id,15,717,511,NULL,123.432800,41.805700,'京沈干线 717 车次延误 45 分钟到达沈阳转运中心',
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 20 HOUR+INTERVAL 5 MINUTE,1,
   DATE_SUB(CURDATE(), INTERVAL 2 DAY)+INTERVAL 20 HOUR+INTERVAL 5 MINUTE);

-- =====================================================================
-- 种子结束。验证（人工执行，预期非零）：
--   SELECT
--     (SELECT COUNT(*) FROM oms_order WHERE create_time >= CURDATE())              AS 今日订单,
--     (SELECT COUNT(DISTINCT order_id) FROM work_transport_order
--        WHERE create_time >= CURDATE())                                          AS 今日运单,
--     (SELECT COUNT(*) FROM work_task_transport WHERE create_time >= CURDATE())    AS 今日任务,
--     (SELECT COUNT(*) FROM work_task_transport WHERE status=2
--        AND truck_id IS NOT NULL)                                                AS 在途任务,
--     (SELECT COUNT(DISTINCT truck_id) FROM work_task_transport
--        WHERE status=2 AND truck_id IS NOT NULL)                                 AS 在途车辆,
--     (SELECT COUNT(*) FROM iot_alarm_record WHERE status=0)                       AS 未处理告警,
--     (SELECT COUNT(*) FROM iot_truck_location WHERE business_type=1)              AS 定位车辆点;
-- =====================================================================
