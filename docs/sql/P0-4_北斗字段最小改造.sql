-- ============================================================
-- P0-4 北斗字段最小改造（R2-1 P0 级 · D-13）
-- 依据：合规清单 §4 关键路径 + 需求 v2.4 §8.8 + 一致性核查报告 §五
-- 约束：D-19 MySQL 5.7.44 锁定，禁止窗口函数 / JSON_TABLE / WITH RECURSIVE
-- 创建日期：2026-10-07
-- 执行顺序：自上而下，可重复执行（ALTER TABLE ... ADD COLUMN IF NOT EXISTS 不被 MySQL 5.7 支持，
--           故用 information_schema 判断列是否存在后决定是否执行 ALTER）
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- 0. 强制目标库（防止连接默认 schema 不对时，DATABASE() 判断全部不命中、脚本静默空跑）
--    库分布：pd_truck_location / pd_truck_location_archive 在 pd_oms；
--            pd_truck 在 pd_base。
--    跨库的表一律使用 库名.表名 全限定写法，不依赖当前默认库。
-- ────────────────────────────────────────────────────────────
USE `pd_oms`;

-- ────────────────────────────────────────────────────────────
-- 1. pd_truck_location 表：加 coord_system / source 字段
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS p_add_column_pd_truck_location_coord_system;
DELIMITER $$
CREATE PROCEDURE p_add_column_pd_truck_location_coord_system()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_oms'
          AND TABLE_NAME = 'pd_truck_location'
          AND COLUMN_NAME = 'coord_system'
    ) THEN
        ALTER TABLE `pd_oms`.`pd_truck_location`
            ADD COLUMN `coord_system` varchar(16) NOT NULL DEFAULT 'BD09'
                COMMENT '坐标系标识 WGS84/GCJ02/BD09/CGCS2000，默认 BD09（消费端统一转成BD09后落库）'
                AFTER `transport_task_id`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_pd_truck_location_coord_system();
DROP PROCEDURE p_add_column_pd_truck_location_coord_system;

DROP PROCEDURE IF EXISTS p_add_column_pd_truck_location_source;
DELIMITER $$
CREATE PROCEDURE p_add_column_pd_truck_location_source()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_oms'
          AND TABLE_NAME = 'pd_truck_location'
          AND COLUMN_NAME = 'source'
    ) THEN
        ALTER TABLE `pd_oms`.`pd_truck_location`
            ADD COLUMN `source` varchar(16) NOT NULL DEFAULT 'MOBILE'
                COMMENT '数据来源 MOBILE/PDA/JT808/NETTY_TCP/HTTP，默认 MOBILE'
                AFTER `coord_system`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_pd_truck_location_source();
DROP PROCEDURE p_add_column_pd_truck_location_source;

-- ────────────────────────────────────────────────────────────
-- 2. pd_truck_location_archive 表（pd_oms 库，全限定名）：同步加 coord_system / source
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS p_add_column_archive_coord_system;
DELIMITER $$
CREATE PROCEDURE p_add_column_archive_coord_system()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_oms'
          AND TABLE_NAME = 'pd_truck_location_archive'
          AND COLUMN_NAME = 'coord_system'
    ) THEN
        ALTER TABLE `pd_oms`.`pd_truck_location_archive`
            ADD COLUMN `coord_system` varchar(16) NOT NULL DEFAULT 'BD09'
                COMMENT '坐标系标识（归档表与 pd_truck_location 保持一致）'
                AFTER `archive_time`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_archive_coord_system();
DROP PROCEDURE p_add_column_archive_coord_system;

DROP PROCEDURE IF EXISTS p_add_column_archive_source;
DELIMITER $$
CREATE PROCEDURE p_add_column_archive_source()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_oms'
          AND TABLE_NAME = 'pd_truck_location_archive'
          AND COLUMN_NAME = 'source'
    ) THEN
        ALTER TABLE `pd_oms`.`pd_truck_location_archive`
            ADD COLUMN `source` varchar(16) NOT NULL DEFAULT 'MOBILE'
                COMMENT '数据来源（归档表与 pd_truck_location 保持一致）'
                AFTER `coord_system`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_archive_source();
DROP PROCEDURE p_add_column_archive_source;

-- ────────────────────────────────────────────────────────────
-- 3. pd_truck 表（pd_base 库，全限定名）：加 online_status / last_heartbeat_time
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS p_add_column_pd_truck_online_status;
DELIMITER $$
CREATE PROCEDURE p_add_column_pd_truck_online_status()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_base'
          AND TABLE_NAME = 'pd_truck'
          AND COLUMN_NAME = 'online_status'
    ) THEN
        ALTER TABLE `pd_base`.`pd_truck`
            ADD COLUMN `online_status` tinyint(1) NOT NULL DEFAULT 0
                COMMENT '在线状态 0-离线 1-在线；由 GpsTraceConsumer 上报时置 1，@Scheduled 扫描超时置 0'
                AFTER `status`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_pd_truck_online_status();
DROP PROCEDURE p_add_column_pd_truck_online_status;

DROP PROCEDURE IF EXISTS p_add_column_pd_truck_last_heartbeat_time;
DELIMITER $$
CREATE PROCEDURE p_add_column_pd_truck_last_heartbeat_time()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = 'pd_base'
          AND TABLE_NAME = 'pd_truck'
          AND COLUMN_NAME = 'last_heartbeat_time'
    ) THEN
        ALTER TABLE `pd_base`.`pd_truck`
            ADD COLUMN `last_heartbeat_time` datetime NULL
                COMMENT '最后心跳时间（最近一次 GPS 上报时由 pd-netty 服务端生成的时间）'
                AFTER `online_status`;
    END IF;
END$$
DELIMITER ;
CALL p_add_column_pd_truck_last_heartbeat_time();
DROP PROCEDURE p_add_column_pd_truck_last_heartbeat_time;

-- ────────────────────────────────────────────────────────────
-- 4. 历史数据回填：所有现存轨迹默认 BD09 / MOBILE；所有车辆默认离线
-- ────────────────────────────────────────────────────────────
-- pd_truck_location 历史轨迹：coord_system/source 因 NOT NULL DEFAULT 已自动填充，无需回填
-- pd_truck 历史车辆：online_status=0 离线，last_heartbeat_time=NULL
-- 因 ALTER 时已设默认值，新字段已有合理默认，无需额外 UPDATE

-- ────────────────────────────────────────────────────────────
-- 5. 索引优化：心跳超时扫描需要 online_status + last_heartbeat_time 联合索引
-- ────────────────────────────────────────────────────────────
DROP PROCEDURE IF EXISTS p_add_index_truck_heartbeat;
DELIMITER $$
CREATE PROCEDURE p_add_index_truck_heartbeat()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = 'pd_base'
          AND TABLE_NAME = 'pd_truck'
          AND INDEX_NAME = 'idx_online_status_heartbeat'
    ) THEN
        ALTER TABLE `pd_base`.`pd_truck`
            ADD INDEX `idx_online_status_heartbeat` (`online_status`, `last_heartbeat_time`)
                COMMENT '心跳超时扫描索引';
    END IF;
END$$
DELIMITER ;
CALL p_add_index_truck_heartbeat();
DROP PROCEDURE p_add_index_truck_heartbeat;

-- ────────────────────────────────────────────────────────────
-- 6. 验证查询（脚本结尾保留，执行后应看到：两轨迹表含 coord_system/source，
--    pd_truck 含 online_status/last_heartbeat_time 且 idx_online_status_heartbeat 存在）
-- ────────────────────────────────────────────────────────────
DESC `pd_oms`.`pd_truck_location`;
DESC `pd_oms`.`pd_truck_location_archive`;
DESC `pd_base`.`pd_truck`;
SHOW INDEX FROM `pd_base`.`pd_truck` WHERE Key_name = 'idx_online_status_heartbeat';

-- ============================================================
-- 改造完成确认清单
-- 1. pd_oms.pd_truck_location 表新增 coord_system/source 字段，默认 BD09/MOBILE
-- 2. pd_oms.pd_truck_location_archive 表同步新增
-- 3. pd_base.pd_truck 表新增 online_status/last_heartbeat_time 字段
-- 4. 心跳超时扫描索引 idx_online_status_heartbeat 已建
-- 5. 历史数据无需回填（ALTER 默认值已就位）
-- ============================================================
