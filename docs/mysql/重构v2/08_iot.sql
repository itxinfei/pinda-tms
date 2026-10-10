-- =====================================================================
-- 域八：轨迹 IoT iot_ · 全部带 tenant_id
-- 来源：pd_oms 物理库内的 pd_truck_location / pd_truck_location_archive /
--       pd_alarm_record（由 pd-netty 服务写入）
-- 说明：高写入量轨迹表。business_type 1车辆 2快递员；坐标统一 DECIMAL；
--       旧 report_time(varchar yyyyMMddHHmmss) 改为 DATETIME。
--       数据量上来后建议对 iot_truck_location 按月做 RANGE 分区（本期先建表）。
-- coord_system 1WGS84 2GCJ02 3BD09 4CGCS2000
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- GPS 轨迹明细
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `iot_truck_location`;
CREATE TABLE `iot_truck_location` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID（雪花）',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `business_id`      BIGINT UNSIGNED NOT NULL COMMENT '业务ID（车辆/快递员）',
  `business_type`    TINYINT UNSIGNED NOT NULL COMMENT '1车辆 2快递员',
  `name`             VARCHAR(50)  DEFAULT NULL COMMENT '名称',
  `phone`            VARCHAR(20)  DEFAULT NULL COMMENT '电话',
  `license_plate`    VARCHAR(20)  DEFAULT NULL COMMENT '车牌号',
  `longitude`        DECIMAL(10,6) NOT NULL COMMENT '经度',
  `latitude`         DECIMAL(9,6)  NOT NULL COMMENT '纬度',
  `coord_system`     TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '坐标系，默认BD09',
  `source`           VARCHAR(20)  DEFAULT NULL COMMENT '来源 MOBILE/PDA/JT808/NETTY_TCP/HTTP',
  `team`             VARCHAR(50)  DEFAULT NULL COMMENT '所属车队',
  `transport_task_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运输任务 work_task_transport.id',
  `report_time`      DATETIME NOT NULL COMMENT '设备上报时间',
  `create_time`      DATETIME NOT NULL COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_business_time` (`tenant_id`,`business_type`,`business_id`,`report_time`),
  KEY `idx_tenant_report` (`tenant_id`,`report_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GPS轨迹明细';

-- ---------------------------------------------------------------------
-- GPS 轨迹归档
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `iot_truck_location_archive`;
CREATE TABLE `iot_truck_location_archive` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `business_id`      BIGINT UNSIGNED NOT NULL COMMENT '业务ID',
  `business_type`    TINYINT UNSIGNED NOT NULL COMMENT '1车辆 2快递员',
  `name`             VARCHAR(50)  DEFAULT NULL,
  `phone`            VARCHAR(20)  DEFAULT NULL,
  `license_plate`    VARCHAR(20)  DEFAULT NULL,
  `longitude`        DECIMAL(10,6) NOT NULL,
  `latitude`         DECIMAL(9,6)  NOT NULL,
  `coord_system`     TINYINT UNSIGNED NOT NULL DEFAULT 3,
  `source`           VARCHAR(20)  DEFAULT NULL,
  `team`             VARCHAR(50)  DEFAULT NULL,
  `transport_task_id` BIGINT UNSIGNED DEFAULT NULL,
  `report_time`      DATETIME NOT NULL COMMENT '设备上报时间',
  `create_time`      DATETIME NOT NULL COMMENT '原始入库时间',
  `archive_time`     DATETIME NOT NULL COMMENT '归档时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_business_time` (`tenant_id`,`business_type`,`business_id`,`report_time`),
  KEY `idx_archive_time` (`archive_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GPS轨迹归档';

-- ---------------------------------------------------------------------
-- GPS 告警
-- alarm_type 1超速 2长时间停留 3偏离路线；status 0未处理 1已处理
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `iot_alarm_record`;
CREATE TABLE `iot_alarm_record` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `alarm_type`       TINYINT UNSIGNED NOT NULL COMMENT '告警类型 1超速 2长时停留 3偏离路线',
  `transport_task_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运输任务ID',
  `truck_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '车辆ID',
  `driver_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '司机/快递员ID',
  `longitude`        DECIMAL(10,6) DEFAULT NULL COMMENT '告警点经度',
  `latitude`         DECIMAL(9,6)  DEFAULT NULL COMMENT '告警点纬度',
  `alarm_content`    VARCHAR(255) DEFAULT NULL COMMENT '告警内容',
  `alarm_time`       DATETIME NOT NULL COMMENT '告警时间',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未处理 1已处理',
  `create_time`      DATETIME NOT NULL COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_truck` (`tenant_id`,`truck_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_alarm_time` (`tenant_id`,`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='GPS告警记录';
