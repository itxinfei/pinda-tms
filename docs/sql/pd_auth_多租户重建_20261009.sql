-- ============================================================
-- pd_auth 库多租户重建：DDL + 最小种子数据
-- 日期：2026-10-09
-- 适用环境：内网 dev（192.168.20.130）
--
-- 用法：
--   1. 本脚本只做「建库 / 建表 / 插入」，不含任何 DROP DATABASE。
--   2. 旧 pd_auth 库的删除须经人工明确确认后另行处理。
--   3. 在空库（或新建库）中整体执行本脚本。
--
-- 种子账号（仅 dev，统一密码 123456，上线前必须修改）：
--   平台运营方 platform / admin / 123456
--   示例企业   pinda    / pinda / 123456
-- ============================================================

CREATE DATABASE IF NOT EXISTS `pd_auth`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE `pd_auth`;

-- ----------------------------
-- 租户（企业）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_auth_tenant` (
  `id`           bigint(20)   NOT NULL COMMENT 'ID',
  `code`         varchar(50)  NOT NULL COMMENT '企业编码（登录标识）',
  `name`         varchar(100) NOT NULL COMMENT '企业名称',
  `status`       bit(1)       NOT NULL DEFAULT b'1' COMMENT '启用状态 1启用 0禁用',
  `expire_time`  datetime              DEFAULT NULL COMMENT '到期时间，空为长期有效',
  `create_user`  bigint(20)            DEFAULT '0' COMMENT '创建人ID',
  `create_time`  datetime              DEFAULT NULL COMMENT '创建时间',
  `update_user`  bigint(20)            DEFAULT '0' COMMENT '更新人ID',
  `update_time`  datetime              DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户（企业）';

-- ----------------------------
-- 用户（账号）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_auth_user` (
  `id`                    bigint(20)   NOT NULL COMMENT 'ID',
  `tenant_id`             bigint(20)   NOT NULL COMMENT '租户ID',
  `account`               varchar(30)  NOT NULL COMMENT '账号（租户内唯一）',
  `name`                  varchar(50)  NOT NULL COMMENT '姓名',
  `org_id`                bigint(20)            DEFAULT NULL COMMENT '组织ID #c_core_org',
  `station_id`            bigint(20)            DEFAULT NULL COMMENT '岗位ID #c_core_station',
  `email`                 varchar(255)          DEFAULT NULL COMMENT '邮箱',
  `mobile`                varchar(20)           DEFAULT '' COMMENT '手机号',
  `sex`                   varchar(1)            DEFAULT 'N' COMMENT '性别 W女 M男 N未知',
  `status`                bit(1)       NOT NULL DEFAULT b'1' COMMENT '启用状态 1启用 0禁用',
  `avatar`                varchar(255)          DEFAULT '' COMMENT '头像',
  `work_describe`         varchar(255)          DEFAULT '' COMMENT '职务描述',
  `password`              varchar(60)  NOT NULL COMMENT '密码（BCrypt 哈希）',
  `password_expire_time`  datetime              DEFAULT NULL COMMENT '密码过期时间，空为不过期',
  `last_login_time`       datetime              DEFAULT NULL COMMENT '最后登录时间',
  `create_user`           bigint(20)            DEFAULT '0' COMMENT '创建人ID',
  `create_time`           datetime              DEFAULT NULL COMMENT '创建时间',
  `update_user`           bigint(20)            DEFAULT '0' COMMENT '更新人ID',
  `update_time`           datetime              DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_account` (`tenant_id`, `account`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户（账号）';

-- ----------------------------
-- 种子：两个租户
-- ----------------------------
INSERT INTO `pd_auth_tenant`
  (`id`, `code`, `name`, `status`, `expire_time`, `create_user`, `create_time`)
VALUES
  (1, 'platform', '平台运营方',     b'1', NULL, 0, NOW()),
  (2, 'pinda',    '品达物流示例企业', b'1', NULL, 0, NOW());

-- ----------------------------
-- 种子：两个管理员（密码为 BCrypt 哈希）
-- ----------------------------
INSERT INTO `pd_auth_user`
  (`id`, `tenant_id`, `account`, `name`, `org_id`, `station_id`, `sex`, `status`,
   `work_describe`, `password`, `create_user`, `create_time`)
VALUES
  (1, 1, 'admin', '平台管理员', NULL, NULL, 'N', b'1',
   '平台超级管理员',
   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES',
   0, NOW()),
  (2, 2, 'pinda', '企业管理员', NULL, NULL, 'N', b'1',
   '企业管理员',
   '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES',
   0, NOW());

-- ----------------------------
-- 组织（树形，租户私有）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_core_org` (
  `id`              bigint(20)   NOT NULL COMMENT 'ID',
  `tenant_id`       bigint(20)   NOT NULL COMMENT '租户ID',
  `name`            varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `abbreviation`    varchar(255)          DEFAULT '' COMMENT '简称',
  `parent_id`       bigint(20)            DEFAULT '0' COMMENT '父ID，根为0',
  `org_type`        tinyint(1)            DEFAULT NULL COMMENT '部门类型 1分公司 2一级转运中心 3二级转运中心 4网点',
  `province_id`     bigint(20)            DEFAULT NULL COMMENT '省',
  `city_id`         bigint(20)            DEFAULT NULL COMMENT '市',
  `county_id`       bigint(20)            DEFAULT NULL COMMENT '区',
  `address`         varchar(255)          DEFAULT NULL COMMENT '地址',
  `contract_number` varchar(20)           DEFAULT NULL COMMENT '联系电话',
  `manager_id`      bigint(20)            DEFAULT NULL COMMENT '负责人ID',
  `tree_path`       varchar(255)          DEFAULT ',' COMMENT '树路径，逗号包裹祖先',
  `sort_value`      int(11)               DEFAULT '1' COMMENT '排序',
  `status`          bit(1)                DEFAULT b'1' COMMENT '状态',
  `describe_`       varchar(255)          DEFAULT '' COMMENT '描述',
  `latitude`        varchar(255)          DEFAULT NULL COMMENT '纬度',
  `longitude`       varchar(255)          DEFAULT NULL COMMENT '经度',
  `business_hours`  varchar(255)          DEFAULT NULL COMMENT '营业时间',
  `create_time`     datetime              DEFAULT NULL,
  `create_user`     bigint(20)            DEFAULT NULL,
  `update_time`     datetime              DEFAULT NULL,
  `update_user`     bigint(20)            DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`, `parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织';

-- ----------------------------
-- 岗位（租户私有）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_core_station` (
  `id`          bigint(20)   NOT NULL COMMENT 'ID',
  `tenant_id`   bigint(20)   NOT NULL COMMENT '租户ID',
  `name`        varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `org_id`      bigint(20)            DEFAULT '0' COMMENT '组织ID #pd_core_org',
  `status`      bit(1)                DEFAULT b'1' COMMENT '状态',
  `describe_`   varchar(255)          DEFAULT '' COMMENT '描述',
  `create_time` datetime              DEFAULT NULL,
  `create_user` bigint(20)            DEFAULT NULL,
  `update_time` datetime              DEFAULT NULL,
  `update_user` bigint(20)            DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_org` (`tenant_id`, `org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位';

-- ----------------------------
-- 行政区划（国家标准，全局共享；全国数据另行导入，本脚本只建表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_area` (
  `id`          int(11)      NOT NULL COMMENT '行政ID（国标区划码）',
  `parent_id`   int(11)               DEFAULT NULL COMMENT '父级行政',
  `name`        varchar(255)          DEFAULT NULL COMMENT '行政名称',
  `area_code`   varchar(255)          DEFAULT NULL,
  `city_code`   varchar(255)          DEFAULT NULL,
  `merger_name` varchar(255)          DEFAULT NULL,
  `short_name`  varchar(255)          DEFAULT NULL,
  `zip_code`    varchar(255)          DEFAULT NULL,
  `level`       tinyint(2)            DEFAULT '0' COMMENT '等级 0省 1市 2县 3镇 4乡村',
  `lng`         varchar(255)          DEFAULT NULL,
  `lat`         varchar(255)          DEFAULT NULL,
  `pinyin`      varchar(255)          DEFAULT NULL,
  `first`       varchar(50)           DEFAULT '0' COMMENT '首字母',
  `update_time` datetime              DEFAULT NULL,
  `update_user` bigint(20)            DEFAULT NULL,
  `create_time` datetime              DEFAULT NULL,
  `create_user` bigint(20)            DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

-- ----------------------------
-- 种子：两个租户的根组织
-- ----------------------------
INSERT INTO `pd_core_org`
  (`id`, `tenant_id`, `name`, `parent_id`, `org_type`, `tree_path`, `sort_value`,
   `status`, `create_user`, `create_time`)
VALUES
  (1, 1, '平台运营总部',   0, 1, ',', 1, b'1', 0, NOW()),
  (2, 2, '品达物流总公司', 0, 1, ',', 1, b'1', 0, NOW());
