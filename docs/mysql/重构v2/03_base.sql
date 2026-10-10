-- =====================================================================
-- 域三：基础资料 base_ · 租户资产，全部带 tenant_id
-- 来源：pd_base（车队/车辆/司机/线路/车次/作业范围等）
-- 已剔除（被新表取代或无用，见文末映射）：
--   pd_agency→sys_org；d_global_user/d_tenant→sys_user/sys_tenant；
--   pd_area_boundaries（废弃临时表）；pd_goods_info（电商SKU表，TMS未使用）。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 车辆类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_truck_type`;
CREATE TABLE `base_truck_type` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`             VARCHAR(50) NOT NULL COMMENT '车辆类型名称',
  `allowable_load`   DECIMAL(10,2) DEFAULT NULL COMMENT '准载重量(kg)',
  `allowable_volume` DECIMAL(10,2) DEFAULT NULL COMMENT '准载体积(m³)',
  `measure_long`     DECIMAL(8,2)  DEFAULT NULL COMMENT '长(m)',
  `measure_width`    DECIMAL(8,2)  DEFAULT NULL COMMENT '宽(m)',
  `measure_high`     DECIMAL(8,2)  DEFAULT NULL COMMENT '高(m)',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `create_by`        BIGINT UNSIGNED DEFAULT NULL,
  `create_time`      DATETIME DEFAULT NULL,
  `update_by`        BIGINT UNSIGNED DEFAULT NULL,
  `update_time`      DATETIME DEFAULT NULL,
  `deleted`          TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆类型';

-- ---------------------------------------------------------------------
-- 货物类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_goods_type`;
CREATE TABLE `base_goods_type` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`           VARCHAR(50) NOT NULL COMMENT '货物类型名称',
  `default_weight` DECIMAL(10,3) DEFAULT NULL COMMENT '默认重量(kg)',
  `default_volume` DECIMAL(10,3) DEFAULT NULL COMMENT '默认体积(m³)',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '说明',
  `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `create_by`      BIGINT UNSIGNED DEFAULT NULL,
  `create_time`    DATETIME DEFAULT NULL,
  `update_by`      BIGINT UNSIGNED DEFAULT NULL,
  `update_time`    DATETIME DEFAULT NULL,
  `deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='货物类型';

-- ---------------------------------------------------------------------
-- 车辆类型-货物类型 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_truck_type_goods`;
CREATE TABLE `base_truck_type_goods` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `truck_type_id` BIGINT UNSIGNED NOT NULL COMMENT '车辆类型ID',
  `goods_type_id` BIGINT UNSIGNED NOT NULL COMMENT '货物类型ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_truck_goods` (`tenant_id`,`truck_type_id`,`goods_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆类型-货物类型';

-- ---------------------------------------------------------------------
-- 车队
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_fleet`;
CREATE TABLE `base_fleet` (
  `id`         BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`       VARCHAR(50) NOT NULL COMMENT '车队名称',
  `fleet_number` VARCHAR(50) DEFAULT NULL COMMENT '车队编号',
  `org_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属组织ID',
  `manager`    VARCHAR(50) DEFAULT NULL COMMENT '负责人',
  `status`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_by`  BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_by`  BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车队';

-- ---------------------------------------------------------------------
-- 车辆
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_truck`;
CREATE TABLE `base_truck` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `truck_type_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '车辆类型ID',
  `fleet_id`         BIGINT UNSIGNED DEFAULT NULL COMMENT '所属车队ID',
  `brand`            VARCHAR(50)  DEFAULT NULL COMMENT '品牌',
  `license_plate`    VARCHAR(20)  DEFAULT NULL COMMENT '车牌号',
  `device_gps_id`    VARCHAR(50)  DEFAULT NULL COMMENT 'GPS设备ID',
  `allowable_load`   DECIMAL(10,2) DEFAULT NULL COMMENT '准载重量(kg)',
  `allowable_volume` DECIMAL(10,2) DEFAULT NULL COMMENT '准载体积(m³)',
  `truck_license_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '车辆行驶证ID',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `online_status`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '在线状态 1在线 0离线（pd-netty 心跳维护，VEHICLE_OFFLINE 告警依赖）',
  `last_heartbeat_time` DATETIME DEFAULT NULL COMMENT '最后心跳时间',
  `create_by`        BIGINT UNSIGNED DEFAULT NULL,
  `create_time`      DATETIME DEFAULT NULL,
  `update_by`        BIGINT UNSIGNED DEFAULT NULL,
  `update_time`      DATETIME DEFAULT NULL,
  `deleted`          TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_plate` (`tenant_id`,`license_plate`),
  KEY `idx_tenant_fleet` (`tenant_id`,`fleet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆';

-- ---------------------------------------------------------------------
-- 司机（员工资料，登录账号为 sys_user）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_truck_driver`;
CREATE TABLE `base_truck_driver` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '关联账号 sys_user.id',
  `fleet_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '所属车队ID',
  `age`         INT DEFAULT NULL COMMENT '年龄',
  `driving_age` INT DEFAULT NULL COMMENT '驾龄',
  `picture`     VARCHAR(255) DEFAULT NULL COMMENT '照片',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user` (`tenant_id`,`user_id`),
  KEY `idx_tenant_fleet` (`tenant_id`,`fleet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='司机';

-- ---------------------------------------------------------------------
-- 司机驾驶证
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_driver_license`;
CREATE TABLE `base_driver_license` (
  `id`                        BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`                 BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `driver_id`                 BIGINT UNSIGNED NOT NULL COMMENT '司机 base_truck_driver.id',
  `allowable_type`            VARCHAR(100) DEFAULT NULL COMMENT '准驾车型',
  `initial_certificate_date`  DATE DEFAULT NULL COMMENT '初次领证日期',
  `valid_period`              VARCHAR(50) DEFAULT NULL COMMENT '有效期限',
  `license_number`            VARCHAR(50) DEFAULT NULL COMMENT '驾驶证号',
  `driver_age`                INT DEFAULT NULL COMMENT '驾龄',
  `license_type`              VARCHAR(50) DEFAULT NULL COMMENT '驾驶证类型',
  `qualification_certificate` VARCHAR(255) DEFAULT NULL COMMENT '从业资格证',
  `pass_certificate`          VARCHAR(255) DEFAULT NULL COMMENT '入场证',
  `picture`                   VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `create_by`                 BIGINT UNSIGNED DEFAULT NULL,
  `create_time`               DATETIME DEFAULT NULL,
  `update_by`                 BIGINT UNSIGNED DEFAULT NULL,
  `update_time`               DATETIME DEFAULT NULL,
  `deleted`                   TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_driver` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='司机驾驶证';

-- ---------------------------------------------------------------------
-- 车辆行驶证
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_truck_license`;
CREATE TABLE `base_truck_license` (
  `id`                          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`                   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `truck_id`                    BIGINT UNSIGNED NOT NULL COMMENT '车辆 base_truck.id',
  `engine_number`               VARCHAR(50) DEFAULT NULL COMMENT '发动机编号',
  `registration_date`           DATE DEFAULT NULL COMMENT '注册日期',
  `mandatory_scrap`             DATE DEFAULT NULL COMMENT '强制报废日期',
  `expiration_date`             DATE DEFAULT NULL COMMENT '检验有效期',
  `overall_quality`             DECIMAL(10,2) DEFAULT NULL COMMENT '整备质量(kg)',
  `allowable_weight`            DECIMAL(10,2) DEFAULT NULL COMMENT '核定载质量(kg)',
  `outside_dimensions`          VARCHAR(100) DEFAULT NULL COMMENT '外廓尺寸',
  `validity_period`             DATE DEFAULT NULL COMMENT '行驶证有效期',
  `transport_certificate_number` VARCHAR(50) DEFAULT NULL COMMENT '道路运输证号',
  `picture`                     VARCHAR(255) DEFAULT NULL COMMENT '图片',
  `create_by`                   BIGINT UNSIGNED DEFAULT NULL,
  `create_time`                 DATETIME DEFAULT NULL,
  `update_by`                   BIGINT UNSIGNED DEFAULT NULL,
  `update_time`                 DATETIME DEFAULT NULL,
  `deleted`                     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_truck` (`truck_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆行驶证';

-- ---------------------------------------------------------------------
-- 线路类型
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_transport_line_type`;
CREATE TABLE `base_transport_line_type` (
  `id`                BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`              VARCHAR(50) NOT NULL COMMENT '线路类型名称',
  `type_number`       VARCHAR(50) DEFAULT NULL COMMENT '编号',
  `start_agency_type` TINYINT UNSIGNED DEFAULT NULL COMMENT '起始机构类型（对齐 sys_org.org_type）',
  `end_agency_type`   TINYINT UNSIGNED DEFAULT NULL COMMENT '目的机构类型',
  `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `create_by`         BIGINT UNSIGNED DEFAULT NULL,
  `create_time`       DATETIME DEFAULT NULL,
  `update_by`         BIGINT UNSIGNED DEFAULT NULL,
  `update_time`       DATETIME DEFAULT NULL,
  `deleted`           TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='线路类型';

-- ---------------------------------------------------------------------
-- 业务线路
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_transport_line`;
CREATE TABLE `base_transport_line` (
  `id`                    BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`             BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`                  VARCHAR(100) NOT NULL COMMENT '线路名称',
  `line_number`           VARCHAR(50) DEFAULT NULL COMMENT '线路编号',
  `org_id`                BIGINT UNSIGNED NOT NULL COMMENT '所属组织ID',
  `transport_line_type_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '线路类型ID',
  `start_org_id`          BIGINT UNSIGNED DEFAULT NULL COMMENT '起始机构ID',
  `end_org_id`            BIGINT UNSIGNED DEFAULT NULL COMMENT '目的机构ID',
  `distance`              DECIMAL(10,2) DEFAULT NULL COMMENT '距离(km)',
  `cost`                  DECIMAL(10,2) DEFAULT NULL COMMENT '成本(元)',
  `estimated_time`        INT DEFAULT NULL COMMENT '预计时间(分钟)',
  `status`                TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_by`             BIGINT UNSIGNED DEFAULT NULL,
  `create_time`           DATETIME DEFAULT NULL,
  `update_by`             BIGINT UNSIGNED DEFAULT NULL,
  `update_time`           DATETIME DEFAULT NULL,
  `deleted`               TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`),
  KEY `idx_tenant_endpoints` (`tenant_id`,`start_org_id`,`end_org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务线路';

-- ---------------------------------------------------------------------
-- 车次（运输班次）
-- period 1天 2周 3月
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_transport_trips`;
CREATE TABLE `base_transport_trips` (
  `id`                BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`              VARCHAR(50) NOT NULL COMMENT '车次名称',
  `departure_time`    VARCHAR(20) DEFAULT NULL COMMENT '发车时间 HH:mm',
  `transport_line_id` BIGINT UNSIGNED NOT NULL COMMENT '所属线路ID',
  `period`            TINYINT UNSIGNED NOT NULL COMMENT '周期 1天 2周 3月',
  `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_by`         BIGINT UNSIGNED DEFAULT NULL,
  `create_time`       DATETIME DEFAULT NULL,
  `update_by`         BIGINT UNSIGNED DEFAULT NULL,
  `update_time`       DATETIME DEFAULT NULL,
  `deleted`           TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_line` (`tenant_id`,`transport_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车次';

-- ---------------------------------------------------------------------
-- 车次-车辆-司机 关联
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_trips_truck_driver`;
CREATE TABLE `base_trips_truck_driver` (
  `id`         BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`  BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `trips_id`   BIGINT UNSIGNED NOT NULL COMMENT '车次ID',
  `truck_id`   BIGINT UNSIGNED NOT NULL COMMENT '车辆ID',
  `driver_id`  BIGINT UNSIGNED NOT NULL COMMENT '司机 base_truck_driver.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_trips_truck_driver` (`tenant_id`,`trips_id`,`truck_id`,`driver_id`),
  KEY `idx_truck` (`truck_id`),
  KEY `idx_driver` (`driver_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车次-车辆-司机';

-- ---------------------------------------------------------------------
-- 机构作业范围（电子围栏）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_org_scope`;
CREATE TABLE `base_org_scope` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `org_id`      BIGINT UNSIGNED NOT NULL COMMENT '机构 sys_org.id',
  `area_id`     INT DEFAULT NULL COMMENT '行政区划 dict_area.id',
  `polygon_points` TEXT COMMENT '围栏多边形点串（JSON）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`),
  KEY `idx_area` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='机构作业范围';

-- ---------------------------------------------------------------------
-- 快递员作业范围（电子围栏）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `base_courier_scope`;
CREATE TABLE `base_courier_scope` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `courier_id`     BIGINT UNSIGNED NOT NULL COMMENT '快递员账号 sys_user.id',
  `area_id`        INT DEFAULT NULL COMMENT '行政区划 dict_area.id',
  `polygon_points` TEXT COMMENT '围栏多边形点串（JSON）',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_courier` (`tenant_id`,`courier_id`),
  KEY `idx_area` (`area_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快递员作业范围';
