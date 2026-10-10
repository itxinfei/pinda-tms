-- =====================================================================
-- 域七：C 端会员 member_ · 全部带 tenant_id
-- 来源：pd_users（pd_member、pd_address_book）
-- 决策：
--   1) 会员按租户隔离（客户小程序属于某租户），手机号租户内唯一；
--      旧会员 ID（M9301 等）废弃，统一雪花，登录走手机号验证码（无密码列）。
--   2) 旧 pd_member.auth_id（9301）不再需要（无独立认证库）；ETL 仅迁 phone/身份证。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 会员
-- id_card_verify 0未验证 1通过 2未通过；status 1正常 0禁用
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `member_info`;
CREATE TABLE `member_info` (
  `id`             BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`      BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `phone`          VARCHAR(20) NOT NULL COMMENT '手机号（登录名）',
  `nickname`       VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar`         VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `id_card_no`     VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  `id_card_verify` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '身份证验证状态',
  `status`         TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1正常 0禁用',
  `create_by`      BIGINT UNSIGNED DEFAULT NULL,
  `create_time`    DATETIME DEFAULT NULL,
  `update_by`      BIGINT UNSIGNED DEFAULT NULL,
  `update_time`    DATETIME DEFAULT NULL,
  `deleted`        TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_phone` (`tenant_id`,`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员';

-- ---------------------------------------------------------------------
-- 会员地址簿；is_default 1默认 0否
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `member_address_book`;
CREATE TABLE `member_address_book` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`     BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `member_id`     BIGINT UNSIGNED NOT NULL COMMENT '会员ID',
  `contact_name`  VARCHAR(50) NOT NULL COMMENT '联系人姓名',
  `phone_number`  VARCHAR(20) NOT NULL COMMENT '联系人电话',
  `extension_number` VARCHAR(10) DEFAULT NULL COMMENT '分机号',
  `province_id`   INT DEFAULT NULL COMMENT '省',
  `city_id`       INT DEFAULT NULL COMMENT '市',
  `county_id`     INT DEFAULT NULL COMMENT '区县',
  `address`       VARCHAR(255) NOT NULL COMMENT '详细地址',
  `longitude`     DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude`      DECIMAL(9,6)  DEFAULT NULL COMMENT '纬度',
  `company_name`  VARCHAR(100) DEFAULT NULL COMMENT '公司名称',
  `is_default`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '1默认 0否',
  `create_by`     BIGINT UNSIGNED DEFAULT NULL,
  `create_time`   DATETIME DEFAULT NULL,
  `update_by`     BIGINT UNSIGNED DEFAULT NULL,
  `update_time`   DATETIME DEFAULT NULL,
  `deleted`       TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_member` (`tenant_id`,`member_id`),
  KEY `idx_tenant_default` (`tenant_id`,`member_id`,`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='会员地址簿';
