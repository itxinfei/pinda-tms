/*
 * pd_auth 权限域建表脚本（DDL-only，11 张表）
 *
 * 来源：pd-authority/pd_auth.sql（Navicat dump，导出日期 2020-03-23，源库 MySQL 5.7.28）
 * 生成：2026-10-09，需求文档《52-旧代码独占知识固化_计费事件与异常上报契约》§4.1 顶层化产物。
 * 与来源差异：仅剔除 248 条 INSERT（2020 年框架演示数据，含 pd_auth_user 口令散列，不可作为种子）；
 *              DROP/CREATE/索引/注释逐字保留，未做任何改写。
 * 表清单：pd_auth_menu, pd_auth_resource, pd_auth_role, pd_auth_role_authority, pd_auth_role_org,
 *          pd_auth_user, pd_auth_user_role, pd_common_login_log, pd_common_opt_log, pd_core_org, pd_core_station
 * 同域其余表落点：pd_area → TMS项目建库脚本/pd_base.sql（并见 docs/sql/区划数据/导入区划四级_可重复执行.sql）；
 *          c_common_dictionary / c_common_dictionary_item → 字典表_c_common_dictionary.sql
 *          undo_log → seata_undo_log.sql（每个业务库都要建）
 * 权威种子（造数据请用这些，不要用本脚本）：
 *   资源注册：docs/sql/管理端接口资源注册_20261007.sql、资源注册路径修正_20261007.sql、三端异常上报接口_资源注册_20261008.sql
 *   三端账号：docs/sql/阻断C_三端接口注册与账号闭环.sql
 * ⚠️ 本脚本含 DROP TABLE IF EXISTS，执行即清空目标库既有数据；只可用于全新建库，禁止直接对已有环境执行。
 */

create database if not exists `pd_auth` default character set utf8mb4 collate utf8mb4_general_ci;
use `pd_auth`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

/*
Navicat MySQL Data Transfer

Source Server         : 本地MySQL数据库
Source Server Version : 50728
Source Host           : localhost:3306
Source Database       : pd_auth

Target Server Type    : MYSQL
Target Server Version : 50728
File Encoding         : 65001

Date: 2020-03-23 12:02:03
*/

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for `pd_auth_menu`
-- ----------------------------
DROP TABLE IF EXISTS `pd_auth_menu`;
CREATE TABLE `pd_auth_menu` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '菜单名称',
  `describe_` varchar(200) DEFAULT '' COMMENT '功能描述',
  `is_public` bit(1) DEFAULT b'0' COMMENT '是否公开菜单\r\n就是无需分配就可以访问的。所有人可见',
  `path` varchar(255) DEFAULT '' COMMENT '对应路由path',
  `component` varchar(255) DEFAULT NULL COMMENT '对应路由组件component',
  `is_enable` bit(1) DEFAULT b'1' COMMENT '状态',
  `sort_value` int(11) DEFAULT '1' COMMENT '排序',
  `icon` varchar(255) DEFAULT '' COMMENT '菜单图标',
  `group_` varchar(20) DEFAULT '' COMMENT '菜单分组',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父级菜单id',
  `create_user` bigint(20) DEFAULT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint(20) DEFAULT NULL COMMENT '更新人id',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `INX_STATUS` (`is_enable`,`is_public`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='菜单';


-- Table structure for `pd_auth_resource`
DROP TABLE IF EXISTS `pd_auth_resource`;
CREATE TABLE `pd_auth_resource` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `code` varchar(150) DEFAULT '' COMMENT '资源编码\n规则：\n链接：\n数据列：\n按钮：',
  `name` varchar(150) NOT NULL DEFAULT '' COMMENT '接口名称',
  `menu_id` bigint(20) DEFAULT NULL COMMENT '菜单ID\n#c_auth_menu',
  `method` varchar(10) DEFAULT NULL,
  `url` varchar(255) DEFAULT NULL,
  `describe_` varchar(255) DEFAULT '' COMMENT '接口描述',
  `create_user` bigint(20) DEFAULT NULL COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint(20) DEFAULT NULL COMMENT '更新人id',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `UN_CODE` (`code`) USING BTREE COMMENT '编码唯一'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='资源';


-- Table structure for `pd_auth_role`
DROP TABLE IF EXISTS `pd_auth_role`;
CREATE TABLE `pd_auth_role` (
  `id` bigint(20) NOT NULL,
  `name` varchar(30) NOT NULL DEFAULT '' COMMENT '角色名称',
  `code` varchar(20) DEFAULT '' COMMENT '角色编码',
  `describe_` varchar(100) DEFAULT '' COMMENT '功能描述',
  `status` bit(1) DEFAULT b'1' COMMENT '状态',
  `readonly` bit(1) DEFAULT b'0' COMMENT '是否内置角色',
  `create_user` bigint(20) DEFAULT '0' COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint(20) DEFAULT '0' COMMENT '更新人id',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `UN_CODE` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色';


-- Table structure for `pd_auth_role_authority`
DROP TABLE IF EXISTS `pd_auth_role_authority`;
CREATE TABLE `pd_auth_role_authority` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `authority_id` bigint(20) NOT NULL COMMENT '权限id\n#c_auth_resource\n#c_auth_menu',
  `authority_type` varchar(10) NOT NULL DEFAULT 'MENU' COMMENT '权限类型\n#AuthorizeType{MENU:菜单;RESOURCE:资源;}',
  `role_id` bigint(20) NOT NULL COMMENT '角色id\n#c_auth_role',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_user` bigint(20) DEFAULT '0' COMMENT '创建人',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `IDX_KEY` (`role_id`,`authority_type`,`authority_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色的资源';


-- Table structure for `pd_auth_role_org`
DROP TABLE IF EXISTS `pd_auth_role_org`;
CREATE TABLE `pd_auth_role_org` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `role_id` bigint(20) DEFAULT NULL COMMENT '角色ID\n#c_auth_role',
  `org_id` bigint(20) DEFAULT NULL COMMENT '部门ID\n#c_core_org',
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色组织关系';


-- Table structure for `pd_auth_user`
DROP TABLE IF EXISTS `pd_auth_user`;
CREATE TABLE `pd_auth_user` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `account` varchar(30) NOT NULL COMMENT '账号',
  `name` varchar(50) NOT NULL COMMENT '姓名',
  `org_id` bigint(20) DEFAULT NULL COMMENT '组织ID\n#c_core_org',
  `station_id` bigint(20) DEFAULT NULL COMMENT '岗位ID\n#c_core_station',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `mobile` varchar(20) DEFAULT '' COMMENT '手机',
  `sex` varchar(1) DEFAULT 'N' COMMENT '性别\n#Sex{W:女;M:男;N:未知}',
  `status` bit(1) DEFAULT b'0' COMMENT '启用状态 1启用 0禁用',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像',
  `work_describe` varchar(255) DEFAULT '' COMMENT '工作描述\r\n比如：  市长、管理员、局长等等   用于登陆展示',
  `password_error_last_time` datetime DEFAULT NULL COMMENT '最后一次输错密码时间',
  `password_error_num` int(11) DEFAULT '0' COMMENT '密码错误次数',
  `password_expire_time` datetime DEFAULT NULL COMMENT '密码过期时间',
  `password` varchar(64) NOT NULL DEFAULT '' COMMENT '密码',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_user` bigint(20) DEFAULT '0' COMMENT '创建人id',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_user` bigint(20) DEFAULT '0' COMMENT '更新人id',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `UN_ACCOUNT` (`account`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='用户';


-- Table structure for `pd_auth_user_role`
DROP TABLE IF EXISTS `pd_auth_user_role`;
CREATE TABLE `pd_auth_user_role` (
  `id` bigint(20) NOT NULL,
  `role_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '角色ID\n#c_auth_role',
  `user_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '用户ID\n#c_core_accou',
  `create_user` bigint(20) DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `IDX_KEY` (`role_id`,`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='角色分配\r\n账号角色绑定';


-- Table structure for `pd_common_login_log`
DROP TABLE IF EXISTS `pd_common_login_log`;
CREATE TABLE `pd_common_login_log` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `request_ip` varchar(50) DEFAULT '' COMMENT '操作IP',
  `user_id` bigint(20) DEFAULT NULL COMMENT '登录人ID',
  `user_name` varchar(50) DEFAULT NULL COMMENT '登录人姓名',
  `account` varchar(30) DEFAULT '' COMMENT '登录人账号',
  `description` varchar(255) DEFAULT '' COMMENT '登录描述',
  `login_date` date DEFAULT NULL COMMENT '登录时间',
  `ua` varchar(500) DEFAULT '0' COMMENT '浏览器请求头',
  `browser` varchar(100) DEFAULT NULL COMMENT '浏览器名称',
  `browser_version` varchar(255) DEFAULT NULL COMMENT '浏览器版本',
  `operating_system` varchar(100) DEFAULT NULL COMMENT '操作系统',
  `location` varchar(50) DEFAULT '' COMMENT '登录地点',
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `IDX_BROWSER` (`browser`) USING BTREE,
  KEY `IDX_OPERATING` (`operating_system`) USING BTREE,
  KEY `IDX_LOGIN_DATE` (`login_date`,`account`) USING BTREE,
  KEY `IDX_ACCOUNT_IP` (`account`,`request_ip`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='登录日志';


-- Table structure for `pd_common_opt_log`
DROP TABLE IF EXISTS `pd_common_opt_log`;
CREATE TABLE `pd_common_opt_log` (
  `id` bigint(20) NOT NULL COMMENT '主键',
  `request_ip` varchar(50) DEFAULT '' COMMENT '操作IP',
  `type` varchar(5) DEFAULT 'OPT' COMMENT '日志类型\n#LogType{OPT:操作类型;EX:异常类型}',
  `user_name` varchar(50) DEFAULT '' COMMENT '操作人',
  `description` varchar(255) DEFAULT '' COMMENT '操作描述',
  `class_path` varchar(255) DEFAULT '' COMMENT '类路径',
  `action_method` varchar(50) DEFAULT '' COMMENT '请求方法',
  `request_uri` varchar(50) DEFAULT '' COMMENT '请求地址',
  `http_method` varchar(10) DEFAULT 'GET' COMMENT '请求类型\n#HttpMethod{GET:GET请求;POST:POST请求;PUT:PUT请求;DELETE:DELETE请求;PATCH:PATCH请求;TRACE:TRACE请求;HEAD:HEAD请求;OPTIONS:OPTIONS请求;}',
  `params` longtext COMMENT '请求参数',
  `result` longtext COMMENT '返回值',
  `ex_desc` longtext COMMENT '异常详情信息',
  `ex_detail` longtext COMMENT '异常描述',
  `start_time` timestamp NULL DEFAULT NULL COMMENT '开始时间',
  `finish_time` timestamp NULL DEFAULT NULL COMMENT '完成时间',
  `consuming_time` bigint(20) DEFAULT '0' COMMENT '消耗时间',
  `ua` varchar(500) DEFAULT '' COMMENT '浏览器',
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `index_type` (`type`) USING BTREE COMMENT '日志类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='系统日志';


-- Table structure for `pd_core_org`
DROP TABLE IF EXISTS `pd_core_org`;
CREATE TABLE `pd_core_org` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `abbreviation` varchar(255) DEFAULT '' COMMENT '简称',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父ID',
  `org_type` tinyint(1) DEFAULT NULL COMMENT '部门类型 1为分公司，2为一级转运中心 3为二级转运中心 4为网点',
  `province_id` bigint(20) DEFAULT NULL COMMENT '省',
  `city_id` bigint(20) DEFAULT NULL COMMENT '市',
  `county_id` bigint(20) DEFAULT NULL COMMENT '区',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `contract_number` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `manager_id` bigint(20) DEFAULT NULL COMMENT '负责人id',
  `tree_path` varchar(255) DEFAULT ',' COMMENT '树结构',
  `sort_value` int(11) DEFAULT '1' COMMENT '排序',
  `status` bit(1) DEFAULT b'1' COMMENT '状态',
  `describe_` varchar(255) DEFAULT '' COMMENT '描述',
  `latitude` varchar(255) DEFAULT NULL COMMENT '纬度',
  `longitude` varchar(255) DEFAULT NULL COMMENT '经度',
  `business_hours` varchar(255) DEFAULT NULL COMMENT '营业时间',
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  FULLTEXT KEY `FU_PATH` (`tree_path`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='组织';


-- Table structure for `pd_core_station`
DROP TABLE IF EXISTS `pd_core_station`;
CREATE TABLE `pd_core_station` (
  `id` bigint(20) NOT NULL COMMENT 'ID',
  `name` varchar(255) NOT NULL DEFAULT '' COMMENT '名称',
  `org_id` bigint(20) DEFAULT '0' COMMENT '组织ID\n#c_core_org',
  `status` bit(1) DEFAULT b'1' COMMENT '状态',
  `describe_` varchar(255) DEFAULT '' COMMENT '描述',
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `update_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='岗位';

SET FOREIGN_KEY_CHECKS = 1;
