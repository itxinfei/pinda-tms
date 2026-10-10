-- =====================================================================
-- 域五：调度 disp_ · 全部带 tenant_id
-- 来源：pd_dispatch（订单分类、算路缓存、定时任务、异常挂起；已排除 QRTZ_*）
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 定时任务定义（按组织，business_id = sys_org.id）
-- status 0暂停 1正常
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_schedule_job`;
CREATE TABLE `disp_schedule_job` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `business_id`     BIGINT UNSIGNED NOT NULL COMMENT '所属组织ID',
  `bean_name`       VARCHAR(200) NOT NULL COMMENT '执行器 Bean 名',
  `params`          VARCHAR(1000) DEFAULT NULL COMMENT '任务参数',
  `cron_expression` VARCHAR(100) NOT NULL COMMENT 'cron 表达式',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0暂停 1正常',
  `remark`          VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by`       BIGINT UNSIGNED DEFAULT NULL,
  `create_time`     DATETIME DEFAULT NULL,
  `update_by`       BIGINT UNSIGNED DEFAULT NULL,
  `update_time`     DATETIME DEFAULT NULL,
  `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_business` (`tenant_id`,`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务定义';

-- ---------------------------------------------------------------------
-- 定时任务执行日志（job_id 已与 job 主键统一为 BIGINT，修复旧类型不一致）
-- status 0失败 1成功
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_schedule_job_log`;
CREATE TABLE `disp_schedule_job_log` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `job_id`      BIGINT UNSIGNED NOT NULL COMMENT '任务ID',
  `bean_name`   VARCHAR(200) NOT NULL COMMENT '执行器 Bean 名',
  `params`      VARCHAR(2000) DEFAULT NULL COMMENT '任务参数',
  `status`      TINYINT UNSIGNED NOT NULL COMMENT '0失败 1成功',
  `error`       VARCHAR(5000) DEFAULT NULL COMMENT '失败信息',
  `times`       INT DEFAULT NULL COMMENT '耗时(ms)',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_job` (`tenant_id`,`job_id`),
  KEY `idx_tenant_create` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='定时任务日志';

-- ---------------------------------------------------------------------
-- 订单分类（一次调度的归组结果）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_order_classify`;
CREATE TABLE `disp_order_classify` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `job_id`        BIGINT UNSIGNED DEFAULT NULL COMMENT '定时任务ID',
  `job_log_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '任务日志ID',
  `start_org_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '组起始机构',
  `end_org_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '组目的机构',
  `classify`      VARCHAR(200) DEFAULT NULL COMMENT '分类串（多级#拼接）',
  `total`         INT NOT NULL DEFAULT 0 COMMENT '组内订单数',
  `create_time`   DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_log` (`tenant_id`,`job_log_id`),
  KEY `idx_tenant_create` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单分类';

-- ---------------------------------------------------------------------
-- 分类-订单（组内订单）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_classify_order`;
CREATE TABLE `disp_classify_order` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `classify_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_classify_order` (`tenant_id`,`classify_id`,`order_id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类-订单';

-- ---------------------------------------------------------------------
-- 分类-派配（车次/车辆/司机）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_classify_attach`;
CREATE TABLE `disp_classify_attach` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `classify_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  `trips_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '车次 base_transport_trips.id',
  `truck_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '车辆 base_truck.id',
  `driver_id`   BIGINT UNSIGNED DEFAULT NULL COMMENT '司机 base_truck_driver.id',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_classify` (`tenant_id`,`classify_id`),
  KEY `idx_truck` (`truck_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分类-派配';

-- ---------------------------------------------------------------------
-- 算路缓存（两机构间路线，多版本）
-- is_current 1当前版本 0历史
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_cache_line`;
CREATE TABLE `disp_cache_line` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `start_org_id`   BIGINT UNSIGNED NOT NULL COMMENT '起始机构',
  `end_org_id`     BIGINT UNSIGNED NOT NULL COMMENT '目的机构',
  `verify_key`     VARCHAR(64) DEFAULT NULL COMMENT '校验key（明细起点拼接md5）',
  `distance`       DECIMAL(10,2) DEFAULT NULL COMMENT '总距离(km)',
  `cost`           DECIMAL(10,2) DEFAULT NULL COMMENT '总成本(元)',
  `estimated_time` INT DEFAULT NULL COMMENT '预计耗时(分钟)',
  `transfer_count` INT NOT NULL DEFAULT 0 COMMENT '中转次数',
  `version`        INT NOT NULL DEFAULT 1 COMMENT '版本',
  `is_current`     TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1当前 0历史',
  `create_time`    DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_start` (`tenant_id`,`start_org_id`),
  KEY `idx_tenant_end` (`tenant_id`,`end_org_id`),
  KEY `idx_verify` (`verify_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算路缓存';

-- ---------------------------------------------------------------------
-- 算路缓存明细（各段实际线路）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_cache_line_detail`;
CREATE TABLE `disp_cache_line_detail` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `cache_line_id`    BIGINT UNSIGNED NOT NULL COMMENT '缓存路线ID',
  `sort`             TINYINT UNSIGNED NOT NULL COMMENT '段顺序',
  `start_org_id`     BIGINT UNSIGNED NOT NULL COMMENT '本段起始机构',
  `end_org_id`       BIGINT UNSIGNED NOT NULL COMMENT '本段目的机构',
  `transport_line_id` BIGINT UNSIGNED NOT NULL COMMENT '业务线路 base_transport_line.id',
  `create_time`      DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_cache` (`tenant_id`,`cache_line_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算路缓存明细';

-- ---------------------------------------------------------------------
-- 算路缓存-使用记录（缓存被哪次分类采用）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_cache_line_use`;
CREATE TABLE `disp_cache_line_use` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `cache_line_id` BIGINT UNSIGNED NOT NULL COMMENT '缓存路线ID',
  `classify_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
  `create_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_cache` (`tenant_id`,`cache_line_id`),
  KEY `idx_tenant_classify` (`tenant_id`,`classify_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='算路缓存使用记录';

-- ---------------------------------------------------------------------
-- 异常调度订单（无法调度，挂起处理）
-- status 0待处理 1已处理
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `disp_exception_order`;
CREATE TABLE `disp_exception_order` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`    BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `org_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '发生时所在机构',
  `reason`      VARCHAR(500) NOT NULL COMMENT '异常原因',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理',
  `remark`      VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `create_time` DATETIME DEFAULT NULL,
  `handle_time` DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='异常调度订单';
