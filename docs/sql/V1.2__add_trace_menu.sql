-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.2
-- 日期: 2026-08-11
-- 描述: 新增"车辆轨迹监控"菜单与权限资源
-- =====================================================

-- 1. 菜单（一级菜单，路由 /pinda/trace，组件 pinda/trace/index）
INSERT INTO `pd_auth_menu`
(`id`, `name`, `describe_`, `is_public`, `path`, `component`, `is_enable`, `sort_value`, `icon`, `group_`, `parent_id`, `create_user`, `create_time`, `update_user`, `update_time`)
VALUES
('660000000000000001', '车辆轨迹监控', 'GPS轨迹查询与回放', b'0', '/pinda/trace', 'pinda/trace/index', b'1', '4', 'el-icon-location-outline', '', '0', '3', '2026-08-11 10:00:00', '3', '2026-08-11 10:00:00');

-- 2. 权限资源（轨迹查询接口）
INSERT INTO `pd_auth_resource`
(`id`, `code`, `name`, `menu_id`, `method`, `url`, `describe_`, `create_user`, `create_time`, `update_user`, `update_time`)
VALUES
('660000000000000101', 'trace:replay', '轨迹回放', '660000000000000001', 'GET', '/trace/replay', '按业务ID+类型查询完整轨迹', '3', '2026-08-11 10:00:00', '3', '2026-08-11 10:00:00'),
('660000000000000102', 'trace:latest', '最近位置', '660000000000000001', 'GET', '/trace/latest', '按业务ID+类型查询最新位置', '3', '2026-08-11 10:00:00', '3', '2026-08-11 10:00:00'),
('660000000000000103', 'trace:page', '轨迹分页', '660000000000000001', 'POST', '/trace/page', '轨迹明细分页查询', '3', '2026-08-11 10:00:00', '3', '2026-08-11 10:00:00');
