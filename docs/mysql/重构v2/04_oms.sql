-- =====================================================================
-- 域四：订单与财务 oms_ · 全部带 tenant_id
-- 来源：pd_oms（order/cargo/location/freight_detail/settlement_order/rule）
-- =====================================================================
-- ✅ 订单状态编码（2026-10-10 用户已拍板：紧凑 TINYINT 1~12）：
--   旧 OrderStatus 为 23000~23011 五位 INT；新表 status 收敛为紧凑 TINYINT：
--     1 待取件(旧23000)      2 已取件(旧23001)    3 网点自寄(旧23002)
--     4 网点入库(旧23003)    5 待装车(旧23004)    6 运输中(旧23005)
--     7 网点出库(旧23006)    8 待派送(旧23007)    9 派送中(旧23008)
--    10 已签收(旧23009)     11 拒收(旧23010)     12 已取消(旧23011)
--   枚举 OrderStatus.code 已同步改为 1~12；调用方一律用枚举常量，无硬编码旧码。
--   注：支付流水表 pd_payment_order 独立建表，DDL 位于 docs/sql/pd_payment_order_建表.sql，不在本文件。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 订单主表
-- order_type 1同城 2城际；pickup_type 1网点自寄 2上门取件
-- payment_method 1预付 2到付；payment_status 1未付 2已付
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order`;
CREATE TABLE `oms_order` (
  `id`                    BIGINT UNSIGNED NOT NULL COMMENT '订单ID（雪花，即业务单号）',
  `tenant_id`             BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_no`              VARCHAR(32) DEFAULT NULL COMMENT '订单号（展示用，可与id同值）',
  `order_type`            TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单类型 1同城 2城际',
  `pickup_type`           TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '取件类型 1网点自寄 2上门取件',
  `status`                TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '订单状态（见文件头映射）',
  `member_id`             BIGINT UNSIGNED DEFAULT NULL COMMENT '下单会员 member_info.id',
  `sender_name`           VARCHAR(50)  DEFAULT NULL COMMENT '寄件人',
  `sender_phone`          VARCHAR(20)  DEFAULT NULL COMMENT '寄件人电话',
  `sender_province_id`    INT DEFAULT NULL COMMENT '寄件省',
  `sender_city_id`        INT DEFAULT NULL COMMENT '寄件市',
  `sender_county_id`      INT DEFAULT NULL COMMENT '寄件区县',
  `sender_address`       VARCHAR(255) DEFAULT NULL COMMENT '寄件详细地址',
  `sender_address_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '寄件地址簿ID',
  `receiver_name`         VARCHAR(50)  DEFAULT NULL COMMENT '收件人',
  `receiver_phone`        VARCHAR(20)  DEFAULT NULL COMMENT '收件人电话',
  `receiver_province_id`  INT DEFAULT NULL COMMENT '收件省',
  `receiver_city_id`      INT DEFAULT NULL COMMENT '收件市',
  `receiver_county_id`    INT DEFAULT NULL COMMENT '收件区县',
  `receiver_address`     VARCHAR(255) DEFAULT NULL COMMENT '收件详细地址',
  `receiver_address_id`  BIGINT UNSIGNED DEFAULT NULL COMMENT '收件地址簿ID',
  `current_org_id`       BIGINT UNSIGNED DEFAULT NULL COMMENT '当前所属网点 sys_org.id',
  `payment_method`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '付款方式 1预付 2到付',
  `payment_status`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '付款状态 1未付 2已付',
  `amount`               DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '订单金额',
  `distance`             DECIMAL(10,2) DEFAULT NULL COMMENT '距离(km)',
  `estimated_arrival_time` DATETIME DEFAULT NULL COMMENT '预计到达时间',
  `create_by`            BIGINT UNSIGNED DEFAULT NULL,
  `create_time`          DATETIME DEFAULT NULL,
  `update_by`            BIGINT UNSIGNED DEFAULT NULL,
  `update_time`          DATETIME DEFAULT NULL,
  `deleted`              TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_member` (`tenant_id`,`member_id`),
  KEY `idx_tenant_org` (`tenant_id`,`current_org_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_create` (`tenant_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单';

-- ---------------------------------------------------------------------
-- 订单货物明细
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order_cargo`;
CREATE TABLE `oms_order_cargo` (
  `id`                 BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`          BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`           BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `transport_order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运单 work_transport_order.id（统一旧 tran_order_id 命名）',
  `goods_type_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '货物类型 base_goods_type.id',
  `name`               VARCHAR(100) DEFAULT NULL COMMENT '货物名称',
  `unit`               VARCHAR(20)  DEFAULT NULL COMMENT '单位',
  `cargo_value`        DECIMAL(12,2) DEFAULT NULL COMMENT '货值（保价）',
  `cargo_barcode`      VARCHAR(50)  DEFAULT NULL COMMENT '条码',
  `quantity`           INT NOT NULL DEFAULT 1 COMMENT '数量',
  `volume`             DECIMAL(10,4) DEFAULT NULL COMMENT '单件体积(m³)',
  `weight`             DECIMAL(10,3) DEFAULT NULL COMMENT '单件重量(kg)',
  `total_volume`       DECIMAL(10,4) DEFAULT NULL COMMENT '总体积(m³)',
  `total_weight`       DECIMAL(10,3) DEFAULT NULL COMMENT '总重量(kg)',
  `remark`             VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by`          BIGINT UNSIGNED DEFAULT NULL,
  `create_time`        DATETIME DEFAULT NULL,
  `update_by`          BIGINT UNSIGNED DEFAULT NULL,
  `update_time`        DATETIME DEFAULT NULL,
  `deleted`            TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单货物明细';

-- ---------------------------------------------------------------------
-- 订单位置（坐标 + 起止网点计算结果），与订单 1:1
-- status 0无效 1有效
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_order_location`;
CREATE TABLE `oms_order_location` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`       BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `send_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '寄件经度',
  `send_latitude`  DECIMAL(9,6)  DEFAULT NULL COMMENT '寄件纬度',
  `receive_longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '收件经度',
  `receive_latitude`  DECIMAL(9,6)  DEFAULT NULL COMMENT '收件纬度',
  `send_org_id`    BIGINT UNSIGNED DEFAULT NULL COMMENT '计算出的起始网点（旧 send_agent_id）',
  `receive_org_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '计算出的目的网点（旧 receive_agent_id）',
  `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0无效 1有效',
  `create_by`      BIGINT UNSIGNED DEFAULT NULL,
  `create_time`    DATETIME DEFAULT NULL,
  `update_by`      BIGINT UNSIGNED DEFAULT NULL,
  `update_time`    DATETIME DEFAULT NULL,
  `deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='订单位置';

-- ---------------------------------------------------------------------
-- 运费明细（数量×单价=金额）
-- fee_item：FREIGHT 运费（可扩展）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_freight_detail`;
CREATE TABLE `oms_freight_detail` (
  `id`                 BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`          BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `order_id`           BIGINT UNSIGNED NOT NULL COMMENT '订单ID',
  `transport_order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运单ID',
  `fee_item`           VARCHAR(32) NOT NULL COMMENT '费用项编码',
  `fee_item_name`      VARCHAR(32) DEFAULT NULL COMMENT '费用项名称',
  `quantity`           DECIMAL(10,2) NOT NULL DEFAULT 1.00 COMMENT '数量',
  `unit`               VARCHAR(16) DEFAULT NULL COMMENT '计量单位',
  `unit_price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '单价',
  `amount`             DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `create_by`          BIGINT UNSIGNED DEFAULT NULL,
  `create_time`        DATETIME DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`),
  KEY `idx_tenant_transport` (`tenant_id`,`transport_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='运费明细';

-- ---------------------------------------------------------------------
-- 结算单
-- settle_object_type 1客户 2司机 3承运商；direction 1应收 2应付
-- status 0待对账 1已对账 2已结算
-- 粒度：去掉旧 uk_order_id，允许一单聚合多订单（order_count），订单→结算仅逻辑关联。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_settlement_order`;
CREATE TABLE `oms_settlement_order` (
  `id`                 BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`          BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `settlement_no`      VARCHAR(40) NOT NULL COMMENT '结算单号（STL+雪花）',
  `order_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '代表订单ID',
  `transport_order_id` BIGINT UNSIGNED DEFAULT NULL COMMENT '运单ID',
  `settle_object_type` TINYINT UNSIGNED NOT NULL COMMENT '结算对象 1客户 2司机 3承运商',
  `settle_object_id`   VARCHAR(64) NOT NULL COMMENT '对象ID（散客=GUEST）',
  `direction`          TINYINT UNSIGNED NOT NULL COMMENT '方向 1应收 2应付',
  `period_start`       DATE DEFAULT NULL COMMENT '账期开始',
  `period_end`         DATE DEFAULT NULL COMMENT '账期结束',
  `order_count`        INT NOT NULL DEFAULT 1 COMMENT '关联订单数',
  `receivable_amount`  DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '应收金额',
  `payable_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '应付金额',
  `settled_amount`     DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '已结算金额',
  `status`             TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0待对账 1已对账 2已结算',
  `remark`             VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_by`          BIGINT UNSIGNED DEFAULT NULL,
  `create_time`        DATETIME DEFAULT NULL,
  `update_by`          BIGINT UNSIGNED DEFAULT NULL,
  `update_time`        DATETIME DEFAULT NULL,
  `deleted`            TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_settlement_no` (`tenant_id`,`settlement_no`),
  KEY `idx_tenant_object` (`tenant_id`,`settle_object_type`,`settle_object_id`),
  KEY `idx_tenant_status` (`tenant_id`,`status`),
  KEY `idx_tenant_order` (`tenant_id`,`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='结算单';

-- ---------------------------------------------------------------------
-- 计费规则（Drools 脚本）
-- 支持平台默认 + 租户自定义：tenant_id=0 表示系统默认规则。
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `oms_charge_rule`;
CREATE TABLE `oms_charge_rule` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID，0为系统默认',
  `rule_key`    VARCHAR(100) NOT NULL COMMENT '规则标识',
  `version`     VARCHAR(20)  NOT NULL COMMENT '版本',
  `content`     MEDIUMTEXT NOT NULL COMMENT '规则脚本',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key_version` (`tenant_id`,`rule_key`,`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='计费规则';
