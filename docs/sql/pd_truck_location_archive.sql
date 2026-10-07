-- ============================================================
-- GPS 轨迹归档表（pd-netty 的 LocationRecordArchive 实体对应）
-- 建在 pd_oms 库，与主表 pd_truck_location 同构，另加归档时间与坐标口径字段。
-- 背景：实体与归档逻辑（LocationRecordServiceImpl.archiveBatch）早已存在，
--       但库里一直缺这张表，归档轮次每次都 catch 异常、历史轨迹无法清理。
-- 执行：DB_PASS=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /data/deploy/middleware/.env)
--       docker exec -i mysql57 mysql -uroot -p"$DB_PASS" < docs/sql/pd_truck_location_archive.sql
-- ============================================================
CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_truck_location_archive` (
  `id` varchar(64) NOT NULL COMMENT '主键(businessId#type#currentTime)',
  `business_id` varchar(64) DEFAULT NULL COMMENT '业务id: 车辆id 或 快递员id',
  `name` varchar(64) DEFAULT NULL COMMENT '司机/快递员名称',
  `phone` varchar(32) DEFAULT NULL COMMENT '司机/快递员电话',
  `license_plate` varchar(32) DEFAULT NULL COMMENT '车牌号',
  `type` varchar(16) DEFAULT NULL COMMENT '类型: truck-车辆 courier-快递员',
  `lng` varchar(32) DEFAULT NULL COMMENT '经度',
  `lat` varchar(32) DEFAULT NULL COMMENT '纬度',
  `current_time` varchar(32) DEFAULT NULL COMMENT '设备上报时间 yyyyMMddHHmmss',
  `team` varchar(64) DEFAULT NULL COMMENT '所属车队',
  `transport_task_id` varchar(64) DEFAULT NULL COMMENT '运输任务id',
  `create_time` datetime DEFAULT NULL COMMENT '入库时间',
  `archive_time` datetime DEFAULT NULL COMMENT '归档时间',
  `coord_system` varchar(16) DEFAULT NULL COMMENT '坐标系 WGS84/GCJ02/BD09/CGCS2000',
  `source` varchar(16) DEFAULT NULL COMMENT '定位来源 GPS/北斗',
  PRIMARY KEY (`id`),
  KEY `idx_archive_time` (`archive_time`),
  KEY `idx_business_id` (`business_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS轨迹归档表';

-- ------------------------------------------------------------
-- 主表补列（2026-10-07）：实体 LocationRecord 早已带 coordSystem/source
-- （P0-4 北斗字段最小改造 · D-13），但线上 pd_truck_location 一直没有这两列。
-- 因为 MyBatis-Plus 默认 NOT_NULL 策略，字段为空时不会出现在 INSERT 里，所以
-- 一直没暴露；一旦真机上报带上坐标系，整批 saveBatch 就会 Unknown column 失败，
-- 表现是"轨迹一条都不落库"。
-- ------------------------------------------------------------
ALTER TABLE `pd_oms`.`pd_truck_location`
  ADD COLUMN `coord_system` varchar(16) DEFAULT NULL COMMENT '坐标系 WGS84/GCJ02/BD09/CGCS2000',
  ADD COLUMN `source` varchar(16) DEFAULT NULL COMMENT '定位来源 GPS/北斗';
