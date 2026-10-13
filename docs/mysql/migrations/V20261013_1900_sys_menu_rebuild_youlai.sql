-- +migrate Up
-- 任务: 《62》P0 SYS-03/D-42 —— sys_menu 重建为 youlai 结构（目录C/菜单M/外链E/按钮B）
-- 说明: 旧 sys_menu 是 pd_auth_menu 结构（仅页面菜单、无 type/perms，无种子无代码引用）。
--       重建为前端 vue3-element-admin 动态路由契约：type C/M/E/B，perm 同时承载按钮权限
--       （v-hasPerm）与后端 @SaCheckPermission。全局平台表（不带 tenant_id，沿用租户插件忽略清单）。
--       sys_role_menu 已存在（tenant_id,role_id,menu_id），角色-菜单/按钮授权直接复用。
--       按钮(type=B)与菜单(type=M)共用本表、按 parent_id 挂在所属菜单下。
-- 幂等: 表不存在才建；菜单种子按固定 id 幂等 upsert（INSERT ... ON DUPLICATE KEY UPDATE）。
-- 作者: Claude Code  日期: 2026-10-13

-- 1) 建表（幂等）。
--    注意：若库中存在【旧版 sys_menu】(pd_auth_menu 结构，无 type 列)，需先手工改名留底再执行本迁移：
--      RENAME TABLE `sys_menu` TO `sys_menu_legacy_20261013`;
--    本 dev 库已于 2026-10-13 完成该 rename（旧表为空、无代码引用）；全新部署无旧表可直接执行。
--    不在迁移脚本里做条件 RENAME——存储过程/DELIMITER 在部分 CI 的 mysql < file 链路不稳定。
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id`           BIGINT UNSIGNED NOT NULL COMMENT 'ID（种子用固定短ID；新增走雪花）',
  `parent_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID，根为0',
  `type`         CHAR(1)        NOT NULL DEFAULT 'M' COMMENT 'C目录 M菜单 E外链 B按钮',
  `name`         VARCHAR(50)    NOT NULL DEFAULT '' COMMENT '菜单/按钮名称',
  `route_name`   VARCHAR(80)    NOT NULL DEFAULT '' COMMENT '路由name（需唯一，前端keep-alive用）',
  `path`         VARCHAR(200)   NOT NULL DEFAULT '' COMMENT '路由path（目录相对；菜单可绝对，/开头不拼父级）',
  `component`    VARCHAR(255)   DEFAULT NULL COMMENT '前端组件路径（views下相对路径，目录=Layout，按钮=NULL）',
  `perm`         VARCHAR(100)   NOT NULL DEFAULT '' COMMENT '权限标识，如 sys:user:add（按钮必填）',
  `icon`         VARCHAR(100)   NOT NULL DEFAULT '' COMMENT '图标',
  `sort_value`   INT            NOT NULL DEFAULT 1 COMMENT '排序（升序）',
  `visible`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否在菜单显示 1显示 0隐藏',
  `status`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `redirect`     VARCHAR(200)   DEFAULT NULL COMMENT '目录默认跳转',
  `keep_alive`   TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '是否缓存 1是 0否',
  `external_url` VARCHAR(255)   DEFAULT NULL COMMENT '外链地址（type=E）',
  `create_by`    BIGINT UNSIGNED DEFAULT NULL,
  `create_time`  DATETIME       DEFAULT NULL,
  `update_by`    BIGINT UNSIGNED DEFAULT NULL,
  `update_time`  DATETIME       DEFAULT NULL,
  `deleted`      TINYINT UNSIGNED NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_status_visible` (`status`,`visible`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单/按钮（全局，平台统一定义）';

-- 2) 业务菜单种子：与前端 src/router/index.ts 现有静态业务菜单 1:1（保证动态切换不丢页面）
--    目录 id 1xx；叶子菜单 id 2xx/3xx；按钮在后续 P0 权限步骤补（type=B，perm=域:资源:动作）。
--    顶层目录 component='Layout'；叶子 path 以 / 开头为绝对路径（保留现有 URL）。
INSERT INTO `sys_menu`
(`id`,`parent_id`,`type`,`name`,`route_name`,`path`,`component`,`perm`,`icon`,`sort_value`,`visible`,`status`,`redirect`,`keep_alive`,`deleted`)
VALUES
-- ===== 顶级目录（component=Layout 由后端 /routes 输出，此处仅标记；目录 path 用相对段）=====
(101,0,'C','工作台','','/dashboard',NULL,'','el-icon-HomeFilled',1,1,1,'/dashboard',0,0),
(102,0,'C','订单中心','OrderCenter','order-center',NULL,'','el-icon-Document',2,1,1,NULL,0,0),
(103,0,'C','调度中心','DispatchCenter','dispatch-center',NULL,'','el-icon-Operation',3,1,1,NULL,0,0),
(104,0,'C','运输在途','TransitCenter','transit',NULL,'','el-icon-Monitor',4,1,1,NULL,0,0),
(105,0,'C','基础档案','ArchiveCenter','archive',NULL,'','el-icon-Files',5,1,1,NULL,0,0),
(106,0,'C','系统管理','System','system',NULL,'','el-icon-Setting',9,1,1,NULL,0,0),

-- ===== 工作台 =====
(201,101,'M','仪表盘','Dashboard','/dashboard','dashboard/index','','el-icon-HomeFilled',1,1,1,NULL,1,0),

-- ===== 订单中心（绝对路径，保留现有 URL）=====
(210,102,'M','订单管理','Order','/order','order/index','','el-icon-Document',1,1,1,NULL,1,0),
(211,102,'M','货物编辑','OrderCargoEdit','/order/cargoEdit','order/cargoEdit','','el-icon-Goods',2,0,1,NULL,1,0),
(212,102,'M','运单管理','Transport','/transport','transport/index','','el-icon-Tickets',3,1,1,NULL,1,0),

-- ===== 调度中心 =====
(220,103,'M','运输任务','Task','/task','task/index','','el-icon-Odometer',1,1,1,NULL,1,0),
(221,103,'M','取派件任务','PickupDispatch','/pickup-dispatch','pickup-dispatch/index','','el-icon-Aim',2,1,1,NULL,1,0),
(222,103,'M','司机作业单','DriverJob','/driver-job','driver-job/index','','el-icon-Avatar',3,1,1,NULL,1,0),

-- ===== 运输在途 =====
(230,104,'M','告警中心','Alarm','/alarm','alarm/index','','el-icon-Warning',1,1,1,NULL,1,0),

-- ===== 基础档案 =====
(240,105,'M','车辆管理','Truck','/truck','truck/index','','el-icon-Van',1,1,1,NULL,1,0),
(241,105,'M','车型管理','TruckType','/truck-type','truck-type/index','','el-icon-Files',2,1,1,NULL,1,0),
(242,105,'M','车队管理','Fleet','/fleet','fleet/index','','el-icon-OfficeBuilding',3,1,1,NULL,1,0),
(243,105,'M','司机管理','Driver','/driver','driver/index','','el-icon-User',4,1,1,NULL,1,0),
(244,105,'M','线路管理','TransportLine','/transport-line','transport-line/index','','el-icon-Guide',5,1,1,NULL,1,0),
(245,105,'M','线路类型','TransportLineType','/transport-line-type','transport-line-type/index','','el-icon-Connection',6,1,1,NULL,1,0),
(246,105,'M','货物类型','GoodsType','/goods-type','goods-type/index','','el-icon-Goods',7,1,1,NULL,1,0),
(247,105,'M','会员管理','Member','/member','member/index','','el-icon-UserFilled',8,1,1,NULL,1,0),

-- ===== 系统管理（已就绪可见；用户/角色/菜单等待本P0后端就绪后由后续迁移置visible=1）=====
(260,106,'M','用户管理','User','/user','system/user/index','sys:user','el-icon-User',1,0,1,NULL,1,0),
(261,106,'M','角色管理','Role','/role','system/role/index','sys:role','el-icon-Stamp',2,0,1,NULL,1,0),
(262,106,'M','菜单管理','Menu','/menu','system/menu/index','sys:menu','el-icon-Menu',3,1,1,NULL,1,0),
(263,106,'M','组织架构','Org','/org','system/org/index','sys:org','el-icon-Share',4,1,1,NULL,1,0),
(264,106,'M','岗位管理','Station','/station','system/station/index','sys:station','el-icon-UserFilled',5,1,1,NULL,1,0),
(265,106,'M','字典管理','Dict','/dict','system/dict/index','sys:dict','el-icon-Notebook',6,0,1,NULL,1,0),
(266,106,'M','参数配置','Config','/config','system/config/index','sys:config','el-icon-Tools',7,1,1,NULL,1,0),
(267,106,'M','消息通知','Notice','/notice','system/notice/index','sys:notice','el-icon-Bell',8,0,1,NULL,1,0),
(268,106,'M','操作日志','Log','/log','system/log/index','sys:log','el-icon-DocumentCopy',9,0,1,NULL,1,0),
(269,106,'M','行政区划','Area','/area','system/area/index','sys:area','el-icon-MapLocation',10,1,1,NULL,1,0),
(270,106,'M','租户管理','Tenant','/tenant','system/tenant/index','sys:tenant','el-icon-Briefcase',11,0,1,NULL,1,0)
ON DUPLICATE KEY UPDATE
  `parent_id`=VALUES(`parent_id`),`type`=VALUES(`type`),`name`=VALUES(`name`),
  `route_name`=VALUES(`route_name`),`path`=VALUES(`path`),`component`=VALUES(`component`),
  `perm`=VALUES(`perm`),`icon`=VALUES(`icon`),`sort_value`=VALUES(`sort_value`),
  `visible`=VALUES(`visible`),`status`=VALUES(`status`),`redirect`=VALUES(`redirect`),
  `keep_alive`=VALUES(`keep_alive`),`deleted`=0;

-- +migrate Down
-- 回滚：删除新表并恢复旧表（若曾 RENAME）。生产慎用，仅开发期。
DROP TABLE IF EXISTS `sys_menu`;
SET @legacy_exists2 := (SELECT COUNT(*) FROM information_schema.TABLES
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_menu_legacy_20261013');
-- 如需恢复旧表，手工执行: RENAME TABLE `sys_menu_legacy_20261013` TO `sys_menu`;
