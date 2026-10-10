-- =====================================================================
-- 域六：作业 work_ · 全部带 tenant_id
-- 来源：pd_work（运单、运输任务、司机作业单、取派任务、状态流转历史）
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 运单
-- status 1新建 2已装车 3到达 4到达终端网点 5已签收 6拒收
-- scheduling_status 1待调度 2未匹配线路 3已调度
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_transport_order`;
CREATE TABLE `work_transport_order` (
  `id`                BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`         BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`          BIGINT UNSIGNED NOT NULL COMMENT '来源订单 oms_order.id',
  `status`            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '运单状态',
  `scheduling_status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '调度状态',
  `create_by`         BIGINT UNSIGNED DEFAULT NULL,
  `create_time`       DATETIME DEFAULT NULL,
  `update_by`         BIGINT UNSIGNED DEFAULT NULL,
  `update_time`       DATETIME DEFAULT NULL,
  `deleted`           TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`),
  KEY `idx_tenant_scheduling` (`tenant_id`,`scheduling_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运单';

-- ---------------------------------------------------------------------
-- 运单-运输任务（多段中转）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_transport_order_task`;
CREATE TABLE `work_transport_order_task` (
  `id`                 BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`          BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `transport_order_id` BIGINT UNSIGNED NOT NULL COMMENT '运单ID',
  `task_id`            BIGINT UNSIGNED NOT NULL COMMENT '运输任务 work_task_transport.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_task` (`tenant_id`,`transport_order_id`,`task_id`),
  KEY `idx_tenant_task` (`tenant_id`,`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运单-运输任务';

-- ---------------------------------------------------------------------
-- 干线运输任务（两机构间一段，绑定车次/车辆）
-- status 1待执行 2进行中 3待确认 4已完成 5已取消
-- assigned_status 1未分配 2已分配 3待人工分配；loading_status 1半载 2满载 3空载
-- 注：actual_pick_up_time 不再带 ON UPDATE（修复旧坑，避免被无关更新刷新）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_task_transport`;
CREATE TABLE `work_task_transport` (
  `id`                    BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`             BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `trips_id`              BIGINT UNSIGNED DEFAULT NULL COMMENT '车次 base_transport_trips.id',
  `start_org_id`          BIGINT UNSIGNED NOT NULL COMMENT '起始机构',
  `end_org_id`            BIGINT UNSIGNED NOT NULL COMMENT '目的机构',
  `status`                TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '任务状态',
  `assigned_status`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '分配状态',
  `loading_status`        TINYINT UNSIGNED NOT NULL DEFAULT 3 COMMENT '满载状态',
  `truck_id`              BIGINT UNSIGNED DEFAULT NULL COMMENT '车辆 base_truck.id',
  `pickup_picture`        VARCHAR(255) DEFAULT NULL COMMENT '提货凭证',
  `cargo_picture`         VARCHAR(255) DEFAULT NULL COMMENT '货物照片',
  `certificate_picture`   VARCHAR(255) DEFAULT NULL COMMENT '回单凭证',
  `deliver_picture`       VARCHAR(255) DEFAULT NULL COMMENT '交付照片',
  `pickup_longitude`      DECIMAL(10,6) DEFAULT NULL COMMENT '提货经度',
  `pickup_latitude`       DECIMAL(9,6)  DEFAULT NULL COMMENT '提货纬度',
  `deliver_longitude`     DECIMAL(10,6) DEFAULT NULL COMMENT '交付经度',
  `deliver_latitude`      DECIMAL(9,6)  DEFAULT NULL COMMENT '交付纬度',
  `plan_departure_time`   DATETIME DEFAULT NULL COMMENT '计划发车',
  `actual_departure_time` DATETIME DEFAULT NULL COMMENT '实际发车',
  `plan_arrival_time`     DATETIME DEFAULT NULL COMMENT '计划到达',
  `actual_arrival_time`   DATETIME DEFAULT NULL COMMENT '实际到达',
  `plan_pick_up_time`     DATETIME DEFAULT NULL COMMENT '计划提货',
  `actual_pick_up_time`   DATETIME DEFAULT NULL COMMENT '实际提货',
  `plan_delivery_time`    DATETIME DEFAULT NULL COMMENT '计划交付',
  `actual_delivery_time`  DATETIME DEFAULT NULL COMMENT '实际交付',
  `create_by`             BIGINT UNSIGNED DEFAULT NULL,
  `create_time`           DATETIME DEFAULT NULL,
  `update_by`             BIGINT UNSIGNED DEFAULT NULL,
  `update_time`           DATETIME DEFAULT NULL,
  `deleted`               TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_truck` (`tenant_id`,`truck_id`),
  KEY `idx_tenant_trips` (`tenant_id`,`trips_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='干线运输任务';

-- ---------------------------------------------------------------------
-- 司机作业单（司机视角，改派产生新单 status=3）
-- status 1待执行 2进行中 3改派 4已完成 5已作废
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_driver_job`;
CREATE TABLE `work_driver_job` (
  `id`                 BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`          BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `driver_id`          BIGINT UNSIGNED NOT NULL COMMENT '司机 base_truck_driver.id',
  `task_transport_id`  BIGINT UNSIGNED NOT NULL COMMENT '运输任务ID',
  `start_org_id`       BIGINT UNSIGNED NOT NULL COMMENT '起始机构',
  `end_org_id`         BIGINT UNSIGNED NOT NULL COMMENT '目的机构',
  `status`             TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '作业状态',
  `start_handover`     VARCHAR(50) DEFAULT NULL COMMENT '提货对接人',
  `finish_handover`    VARCHAR(50) DEFAULT NULL COMMENT '交付对接人',
  `plan_departure_time`  DATETIME DEFAULT NULL COMMENT '计划发车',
  `actual_departure_time` DATETIME DEFAULT NULL COMMENT '实际发车',
  `plan_arrival_time`  DATETIME DEFAULT NULL COMMENT '计划到达',
  `actual_arrival_time` DATETIME DEFAULT NULL COMMENT '实际到达',
  `create_by`          BIGINT UNSIGNED DEFAULT NULL,
  `create_time`        DATETIME DEFAULT NULL,
  `update_by`          BIGINT UNSIGNED DEFAULT NULL,
  `update_time`        DATETIME DEFAULT NULL,
  `deleted`            TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_driver` (`tenant_id`,`driver_id`),
  KEY `idx_tenant_task` (`tenant_id`,`task_transport_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='司机作业单';

-- ---------------------------------------------------------------------
-- 末端取件/派件任务（一个订单一条）
-- task_type 1取件 2派件；sign_status 1签收 2拒收
-- status 1待执行 2进行中 3待确认 4已完成 5已取消
-- assigned_status 1未分配 2已分配 3待人工分配
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_pickup_dispatch_task`;
CREATE TABLE `work_pickup_dispatch_task` (
  `id`               BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`        BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`         BIGINT UNSIGNED NOT NULL COMMENT '订单 oms_order.id',
  `task_type`        TINYINT UNSIGNED NOT NULL COMMENT '任务类型 1取件 2派件',
  `status`           TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '任务状态',
  `sign_status`      TINYINT UNSIGNED DEFAULT NULL COMMENT '签收状态',
  `org_id`           BIGINT UNSIGNED NOT NULL COMMENT '所属网点 sys_org.id',
  `courier_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '快递员 sys_user.id',
  `assigned_status`  TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '分配状态',
  `estimated_start_time` DATETIME DEFAULT NULL COMMENT '预计开始',
  `actual_start_time`    DATETIME DEFAULT NULL COMMENT '实际开始',
  `estimated_end_time`   DATETIME DEFAULT NULL COMMENT '预计完成',
  `actual_end_time`      DATETIME DEFAULT NULL COMMENT '实际完成',
  `confirm_time`    DATETIME DEFAULT NULL COMMENT '确认时间',
  `cancel_time`     DATETIME DEFAULT NULL COMMENT '取消时间',
  `mark`            VARCHAR(50) DEFAULT NULL COMMENT '备注',
  `create_by`       BIGINT UNSIGNED DEFAULT NULL,
  `create_time`     DATETIME DEFAULT NULL,
  `update_by`       BIGINT UNSIGNED DEFAULT NULL,
  `update_time`     DATETIME DEFAULT NULL,
  `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`),
  KEY `idx_tenant_courier_status` (`tenant_id`,`courier_id`,`status`),
  KEY `idx_tenant_assigned` (`tenant_id`,`assigned_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='末端取派任务';

-- ---------------------------------------------------------------------
-- 状态流转历史（横切审计；operator_type 1后台 2司机 3快递员）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `work_status_transition_history`;
CREATE TABLE `work_status_transition_history` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `business_type`  TINYINT UNSIGNED NOT NULL COMMENT '业务类型 1运输任务（可扩展）',
  `business_id`    BIGINT UNSIGNED NOT NULL COMMENT '业务主体ID',
  `business_no`    VARCHAR(50) DEFAULT NULL COMMENT '业务单号',
  `operation_type` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '操作类型 1状态变更',
  `before_status`  TINYINT UNSIGNED DEFAULT NULL COMMENT '变更前状态',
  `after_status`   TINYINT UNSIGNED DEFAULT NULL COMMENT '变更后状态',
  `operator_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '操作人ID',
  `operator_name`  VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `operator_type`  TINYINT UNSIGNED DEFAULT NULL COMMENT '操作人类型 1后台 2司机 3快递员',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `operate_time`   DATETIME DEFAULT NULL COMMENT '操作时间',
  `create_time`    DATETIME DEFAULT NULL COMMENT '落库时间',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_business` (`tenant_id`,`business_type`,`business_id`),
  KEY `idx_tenant_operate` (`tenant_id`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='状态流转历史';
