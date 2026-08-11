-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.5
-- 日期: 2026-08-11
-- 描述: 新增 GPS 轨迹归档表，用于承载从 pd_truck_location 清理出的历史轨迹，
--       实现"归档→清理"闭环，避免历史数据直接丢失且不影响主表查询性能
-- =====================================================

DROP TABLE IF EXISTS `pd_truck_location_archive`;
CREATE TABLE `pd_truck_location_archive` (
    `id`             varchar(64)  NOT NULL COMMENT '主键(businessId#type#currentTime)',
    `business_id`    varchar(64)  DEFAULT NULL COMMENT '业务id: 快递员id 或 车辆id',
    `name`           varchar(64)  DEFAULT NULL COMMENT '司机/快递员名称',
    `phone`          varchar(32)  DEFAULT NULL COMMENT '司机/快递员电话',
    `license_plate`  varchar(32)  DEFAULT NULL COMMENT '车牌号',
    `type`           varchar(16)  DEFAULT NULL COMMENT '类型: truck-车辆 courier-快递员',
    `lng`            varchar(32)  DEFAULT NULL COMMENT '经度',
    `lat`            varchar(32)  DEFAULT NULL COMMENT '纬度',
    `current_time`   varchar(32)  DEFAULT NULL COMMENT '设备上报时间 yyyyMMddHHmmss',
    `team`           varchar(64)  DEFAULT NULL COMMENT '所属车队',
    `transport_task_id` varchar(64) DEFAULT NULL COMMENT '运输任务id',
    `create_time`    datetime     DEFAULT NULL COMMENT '原始入库时间',
    `archive_time`   datetime     DEFAULT NULL COMMENT '归档时间',
    PRIMARY KEY (`id`),
    KEY `idx_archive_business_id` (`business_id`),
    KEY `idx_archive_create_time` (`create_time`),
    KEY `idx_archive_current_time` (`current_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS轨迹归档表';
