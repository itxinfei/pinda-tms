-- ============================================================
-- GPS 告警记录建表脚本（pd_oms 库）
--
-- 背景：GpsAlertService 在超速(SPEED_OVER)/滞留(STAY_TOO_LONG)/偏航(DEVIATE_ROUTE)
--   时只 log.error（可选 Webhook），没有任何落库，事后无法追溯与处置。
--   2026-10-08 补建 pd_alarm_record 表，由 GpsAlertService 在三类告警触发时写入。
--
-- 建表库说明：系统中不存在独立的 pd_netty 库，pd-netty 服务的数据源实际指向
--   pd_oms（轨迹表 pd_truck_location 也在该库），因此告警表一并建在 pd_oms。
--
-- 幂等：同一 transport_task_id + alarm_type 且 status=0 的未处理记录已存在时，
--   应用层不再重复插入（拿不到 taskId 时直接插入）。
--
-- 执行（口令不写在脚本里，取服务器 .env）：
--   DB_PASS=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /data/deploy/middleware/.env)
--   docker exec -i mysql57 mysql -uroot -p"$DB_PASS" < docs/sql/告警记录_建表.sql
-- ============================================================

USE `pd_oms`;

CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_alarm_record` (
  `id` varchar(64) NOT NULL COMMENT '主键（MyBatis-Plus 雪花算法生成）',
  `alarm_type` varchar(32) NOT NULL COMMENT '告警类型: SPEED_OVER-超速 STAY_TOO_LONG-长时间停留 DEVIATE_ROUTE-偏离路线',
  `transport_task_id` varchar(64) DEFAULT NULL COMMENT '运输任务id',
  `truck_id` varchar(64) DEFAULT NULL COMMENT '车辆id（车辆上报时取 businessId）',
  `driver_id` varchar(64) DEFAULT NULL COMMENT '司机/快递员id（快递员上报时取 businessId）',
  `longitude` double DEFAULT NULL COMMENT '经度（BD09）',
  `latitude` double DEFAULT NULL COMMENT '纬度（BD09）',
  `alarm_content` varchar(512) DEFAULT NULL COMMENT '告警内容',
  `alarm_time` datetime DEFAULT NULL COMMENT '告警时间（pd-netty 服务端时间）',
  `status` int(11) NOT NULL DEFAULT '0' COMMENT '处理状态: 0-未处理 1-已处理',
  `create_time` datetime DEFAULT NULL COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_type_status` (`transport_task_id`,`alarm_type`,`status`),
  KEY `idx_alarm_type` (`alarm_type`),
  KEY `idx_status` (`status`),
  KEY `idx_alarm_time` (`alarm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='GPS告警记录表';

-- 自检：期望返回 1 行，table_name=pd_alarm_record
SELECT table_name, table_comment FROM information_schema.tables
 WHERE table_schema='pd_oms' AND table_name='pd_alarm_record';
