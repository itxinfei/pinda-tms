-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.1
-- 日期: 2026-08-11
-- 描述: 新增 GPS 轨迹明细表，用于持久化车辆/快递员上报的位置数据
-- =====================================================

-- 轨迹明细表
DROP TABLE IF EXISTS `pd_truck_location`;
CREATE TABLE `pd_truck_location` (
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
    `create_time`    datetime     DEFAULT NULL COMMENT '入库时间',
    PRIMARY KEY (`id`),
    KEY `idx_business_id` (`business_id`),
    KEY `idx_current_time` (`current_time`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS轨迹明细表';
