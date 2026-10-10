-- =====================================================================
-- 域一：认证授权 / 平台系统 sys_
-- 来源旧库：pd_auth（pd_auth_tenant/user、pd_core_org/station）
--           pinda_tms（pd_auth_menu/resource/role）
-- 约定：除 sys_tenant / sys_menu / sys_resource 为平台级外，其余带 tenant_id。
--       主键雪花 BIGINT；状态/布尔统一 TINYINT UNSIGNED（1是0否，除特别注明）。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 租户（企业）· 平台级，本身不带 tenant_id
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_tenant`;
CREATE TABLE `sys_tenant` (
  `id`            BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `code`          VARCHAR(50)  NOT NULL COMMENT '企业编码（登录标识）',
  `name`          VARCHAR(100) NOT NULL COMMENT '企业名称',
  `status`        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `expire_time`   DATETIME     DEFAULT NULL COMMENT '到期时间，空为长期有效',
  `contact_name`  VARCHAR(50)  DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `logo`          VARCHAR(255) DEFAULT '' COMMENT 'logo URL',
  `description`   VARCHAR(255) DEFAULT '' COMMENT '企业简介',
  `create_by`     BIGINT UNSIGNED DEFAULT NULL,
  `create_time`   DATETIME     DEFAULT NULL,
  `update_by`     BIGINT UNSIGNED DEFAULT NULL,
  `update_time`   DATETIME     DEFAULT NULL,
  `deleted`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户（企业）';

-- ---------------------------------------------------------------------
-- 用户（后台账号）· 租户内
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id`                   BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`            BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `account`              VARCHAR(30)  NOT NULL COMMENT '账号（租户内唯一）',
  `name`                 VARCHAR(50)  NOT NULL COMMENT '姓名',
  `org_id`               BIGINT UNSIGNED DEFAULT NULL COMMENT '主组织ID',
  `station_id`           BIGINT UNSIGNED DEFAULT NULL COMMENT '主岗位ID',
  `email`                VARCHAR(255) DEFAULT NULL COMMENT '邮箱',
  `mobile`               VARCHAR(20)  DEFAULT '' COMMENT '手机号',
  `sex`                  TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '性别 0未知 1男 2女',
  `status`               TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '状态 1启用 0禁用',
  `avatar`               VARCHAR(255) DEFAULT '' COMMENT '头像',
  `work_describe`        VARCHAR(255) DEFAULT '' COMMENT '职务描述',
  `password`             VARCHAR(60)  NOT NULL COMMENT '密码（BCrypt）',
  `password_expire_time` DATETIME     DEFAULT NULL COMMENT '密码过期时间，空为不过期',
  `last_login_time`      DATETIME     DEFAULT NULL COMMENT '最后登录时间',
  `create_by`            BIGINT UNSIGNED DEFAULT NULL,
  `create_time`          DATETIME     DEFAULT NULL,
  `update_by`            BIGINT UNSIGNED DEFAULT NULL,
  `update_time`          DATETIME     DEFAULT NULL,
  `deleted`              TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_account` (`tenant_id`,`account`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户（后台账号）';

-- ---------------------------------------------------------------------
-- 组织（机构树）· 租户内
-- org_type 1分公司 2一级转运中心 3二级转运中心 4网点
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_org`;
CREATE TABLE `sys_org` (
  `id`              BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`       BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`            VARCHAR(255) NOT NULL DEFAULT '' COMMENT '名称',
  `abbreviation`    VARCHAR(255) DEFAULT '' COMMENT '简称',
  `parent_id`       BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父ID，根为0',
  `org_type`        TINYINT UNSIGNED DEFAULT NULL COMMENT '1分公司 2一级转运 3二级转运 4网点',
  `province_id`     INT          DEFAULT NULL COMMENT '省（dict_area.id）',
  `city_id`         INT          DEFAULT NULL COMMENT '市（dict_area.id）',
  `county_id`       INT          DEFAULT NULL COMMENT '区县（dict_area.id）',
  `address`         VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `contract_number` VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  `manager_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '负责人ID（sys_user.id）',
  `tree_path`       VARCHAR(500) NOT NULL DEFAULT ',' COMMENT '树路径，逗号包裹祖先',
  `sort_value`      INT          NOT NULL DEFAULT 1 COMMENT '排序',
  `status`          TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `longitude`       DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  `latitude`        DECIMAL(9,6)  DEFAULT NULL COMMENT '纬度',
  `business_hours`  VARCHAR(255) DEFAULT NULL COMMENT '营业时间',
  `description`     VARCHAR(255) DEFAULT '' COMMENT '描述',
  `create_by`       BIGINT UNSIGNED DEFAULT NULL,
  `create_time`     DATETIME     DEFAULT NULL,
  `update_by`       BIGINT UNSIGNED DEFAULT NULL,
  `update_time`     DATETIME     DEFAULT NULL,
  `deleted`         TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_parent` (`tenant_id`,`parent_id`),
  KEY `idx_tenant_type` (`tenant_id`,`org_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织（机构）';

-- ---------------------------------------------------------------------
-- 岗位 · 租户内
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_station`;
CREATE TABLE `sys_station` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`        VARCHAR(255) NOT NULL DEFAULT '' COMMENT '岗位名称',
  `org_id`      BIGINT UNSIGNED NOT NULL COMMENT '所属组织ID',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `description` VARCHAR(255) DEFAULT '' COMMENT '描述',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_org` (`tenant_id`,`org_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位';

-- ---------------------------------------------------------------------
-- 菜单 · 平台级（系统统一定义，全租户共享），不带 tenant_id
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单，根为0',
  `name`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '菜单名称',
  `description` VARCHAR(200) DEFAULT '' COMMENT '功能描述',
  `is_public`   TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否公开 1是 0否（公开即免授权可见）',
  `path`        VARCHAR(255) DEFAULT '' COMMENT '路由 path',
  `component`   VARCHAR(255) DEFAULT NULL COMMENT '路由组件',
  `is_enable`   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `sort_value`  INT          NOT NULL DEFAULT 1 COMMENT '排序',
  `icon`        VARCHAR(255) DEFAULT '' COMMENT '图标',
  `group_`      VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '菜单分组',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_enable_public` (`is_enable`,`is_public`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单';

-- ---------------------------------------------------------------------
-- 资源（接口/按钮/数据权限）· 平台级
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_resource`;
CREATE TABLE `sys_resource` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `code`        VARCHAR(150) NOT NULL DEFAULT '' COMMENT '资源编码（全局唯一）',
  `name`        VARCHAR(150) NOT NULL DEFAULT '' COMMENT '资源名称',
  `menu_id`     BIGINT UNSIGNED DEFAULT NULL COMMENT '所属菜单ID',
  `method`      VARCHAR(10)  DEFAULT NULL COMMENT 'HTTP 方法',
  `url`         VARCHAR(255) DEFAULT NULL COMMENT '接口 URL',
  `description` VARCHAR(255) DEFAULT '' COMMENT '描述',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='资源（接口权限）';

-- ---------------------------------------------------------------------
-- 角色 · 租户内（内置角色 readonly=1）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `name`        VARCHAR(30)  NOT NULL DEFAULT '' COMMENT '角色名称',
  `code`        VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '角色编码',
  `description` VARCHAR(100) DEFAULT '' COMMENT '功能描述',
  `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `readonly`    TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否内置 1是 0否',
  `create_by`   BIGINT UNSIGNED DEFAULT NULL,
  `create_time` DATETIME     DEFAULT NULL,
  `update_by`   BIGINT UNSIGNED DEFAULT NULL,
  `update_time` DATETIME     DEFAULT NULL,
  `deleted`     TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_id`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色';

-- ---------------------------------------------------------------------
-- 关联：用户-角色 / 角色-菜单 / 角色-资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id`        BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `user_id`   BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
  `role_id`   BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`tenant_id`,`user_id`,`role_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-角色';

DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id`        BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id` BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `role_id`   BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id`   BIGINT UNSIGNED NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`tenant_id`,`role_id`,`menu_id`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-菜单';

DROP TABLE IF EXISTS `sys_role_resource`;
CREATE TABLE `sys_role_resource` (
  `id`          BIGINT UNSIGNED NOT NULL COMMENT 'ID',
  `tenant_id`   BIGINT UNSIGNED NOT NULL COMMENT '租户ID',
  `role_id`     BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
  `resource_id` BIGINT UNSIGNED NOT NULL COMMENT '资源ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_resource` (`tenant_id`,`role_id`,`resource_id`),
  KEY `idx_resource` (`resource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色-资源';
