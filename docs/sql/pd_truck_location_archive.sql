-- ============================================================
-- GPS 轨迹表建表脚本（pd_oms 库）
--
-- 背景 1：实体 LocationRecord / LocationRecordArchive 与归档逻辑
--   （LocationRecordServiceImpl.archiveBatch）早已存在，但仓库里从来没有这两张表的
--   建表脚本，现网 pd_truck_location_archive 也一直缺失——归档轮次每次都 catch 异常，
--   历史轨迹清不掉。2026-10-07 补全。
-- 背景 2：上报时间列原为 current_time，那是 MySQL 保留字：手写 SQL 不加反引号时
--   `select current_time from pd_truck_location` 返回的是**当前时钟**而不是列值
--   （压测时真实踩到过），且批量 INSERT 会直接语法报错。列名统一改为 report_time，
--   Java 字段仍是 currentTime（@TableField 指定列名），调用方无需改动。
--
-- 执行（口令不写在脚本里，取服务器 .env）：
--   DB_PASS=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /data/deploy/middleware/.env)
--   docker exec -i mysql57 mysql -uroot -p"$DB_PASS" < docs/sql/pd_truck_location_archive.sql
-- ============================================================

-- 存储过程必须有库上下文，先选中，否则报 "No database selected"
USE `pd_oms`;

-- 1. 主表（新环境建表用；已有环境跳过）
CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_truck_location` (
  `id` varchar(64) NOT NULL COMMENT '主键(businessId#type#currentTime)',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务id: 车辆id 或 快递员id',
  `name` varchar(64) DEFAULT NULL COMMENT '司机/快递员名称',
  `phone` varchar(32) DEFAULT NULL COMMENT '司机/快递员电话',
  `license_plate` varchar(32) DEFAULT NULL COMMENT '车牌号',
  `type` varchar(16) DEFAULT NULL COMMENT '类型: truck-车辆 courier-快递员',
  `lng` varchar(32) DEFAULT NULL COMMENT '经度',
  `lat` varchar(32) DEFAULT NULL COMMENT '纬度',
  `report_time` varchar(32) DEFAULT NULL COMMENT '设备上报时间 yyyyMMddHHmmss',
  `team` varchar(64) DEFAULT NULL COMMENT '所属车队',
  `transport_task_id` varchar(64) DEFAULT NULL COMMENT '运输任务id',
  `create_time` datetime DEFAULT NULL COMMENT '入库时间',
  `coord_system` varchar(16) DEFAULT NULL COMMENT '坐标系 WGS84/GCJ02/BD09/CGCS2000',
  `source` varchar(16) DEFAULT NULL COMMENT '定位来源 GPS/北斗',
  PRIMARY KEY (`id`),
  KEY `idx_business_id` (`business_id`),
  KEY `idx_report_time` (`report_time`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS轨迹明细表';

-- 2. 归档表
CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_truck_location_archive` (
  `id` varchar(64) NOT NULL COMMENT '主键(与主表一致)',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务id',
  `name` varchar(64) DEFAULT NULL COMMENT '司机/快递员名称',
  `phone` varchar(32) DEFAULT NULL COMMENT '司机/快递员电话',
  `license_plate` varchar(32) DEFAULT NULL COMMENT '车牌号',
  `type` varchar(16) DEFAULT NULL COMMENT '类型: truck-车辆 courier-快递员',
  `lng` varchar(32) DEFAULT NULL COMMENT '经度',
  `lat` varchar(32) DEFAULT NULL COMMENT '纬度',
  `report_time` varchar(32) DEFAULT NULL COMMENT '设备上报时间 yyyyMMddHHmmss',
  `team` varchar(64) DEFAULT NULL COMMENT '所属车队',
  `transport_task_id` varchar(64) DEFAULT NULL COMMENT '运输任务id',
  `create_time` datetime DEFAULT NULL COMMENT '入库时间',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `coord_system` varchar(16) DEFAULT NULL COMMENT '坐标系 WGS84/GCJ02/BD09/CGCS2000',
  `source` varchar(16) DEFAULT NULL COMMENT '定位来源 GPS/北斗',
  PRIMARY KEY (`id`),
  KEY `idx_archive_time` (`archive_time`),
  KEY `idx_business_id` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS轨迹归档表';

-- 3. 老环境的列名迁移：只要还存在 current_time 就改名为 report_time（幂等）
DROP PROCEDURE IF EXISTS p_fix_location_report_time;
DELIMITER $$
CREATE PROCEDURE p_fix_location_report_time()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE t VARCHAR(64);
  DECLARE cur CURSOR FOR
    SELECT table_name FROM information_schema.columns
     WHERE table_schema='pd_oms'
       AND table_name IN ('pd_truck_location','pd_truck_location_archive')
       AND column_name='current_time';
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO t;
    IF done = 1 THEN LEAVE read_loop; END IF;
    SET @sql = CONCAT('ALTER TABLE `pd_oms`.`', t,
      '` CHANGE COLUMN `current_time` `report_time` varchar(32) DEFAULT NULL COMMENT ''设备上报时间 yyyyMMddHHmmss''');
    PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    IF EXISTS (SELECT 1 FROM information_schema.statistics
                WHERE table_schema='pd_oms' AND table_name=t AND index_name='idx_current_time') THEN
      SET @sql = CONCAT('ALTER TABLE `pd_oms`.`', t, '` RENAME INDEX `idx_current_time` TO `idx_report_time`');
      PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;
CALL p_fix_location_report_time();
DROP PROCEDURE p_fix_location_report_time;

-- 4. 北斗字段（P0-4 · D-13）：实体早有 coordSystem/source，库里缺列时真机一上报
--    整批 saveBatch 就会 Unknown column 失败。空值不进 INSERT，所以历史上没暴露。
DROP PROCEDURE IF EXISTS p_fix_location_bds_columns;
DELIMITER $$
CREATE PROCEDURE p_fix_location_bds_columns()
BEGIN
  DECLARE t VARCHAR(64);
  DECLARE done INT DEFAULT 0;
  DECLARE cur CURSOR FOR SELECT table_name FROM information_schema.tables
                          WHERE table_schema='pd_oms'
                            AND table_name IN ('pd_truck_location','pd_truck_location_archive');
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  OPEN cur;
  read_loop: LOOP
    FETCH cur INTO t;
    IF done = 1 THEN LEAVE read_loop; END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='pd_oms'
                     AND table_name=t AND column_name='coord_system') THEN
      SET @sql = CONCAT('ALTER TABLE `pd_oms`.`', t, '` ADD COLUMN `coord_system` varchar(16) DEFAULT NULL COMMENT ''坐标系 WGS84/GCJ02/BD09/CGCS2000''');
      PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='pd_oms'
                     AND table_name=t AND column_name='source') THEN
      SET @sql = CONCAT('ALTER TABLE `pd_oms`.`', t, '` ADD COLUMN `source` varchar(16) DEFAULT NULL COMMENT ''定位来源 GPS/北斗''');
      PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;
CALL p_fix_location_bds_columns();
DROP PROCEDURE p_fix_location_bds_columns;

-- 5. 自检：期望两张表都有 report_time / coord_system / source，且不再有 current_time
SELECT table_name, column_name FROM information_schema.columns
 WHERE table_schema='pd_oms'
   AND table_name IN ('pd_truck_location','pd_truck_location_archive')
   AND column_name IN ('report_time','current_time','coord_system','source')
 ORDER BY table_name, column_name;
