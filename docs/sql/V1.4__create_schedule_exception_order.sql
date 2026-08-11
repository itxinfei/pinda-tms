-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.4
-- 日期: 2026-08-11
-- 描述: 新增"异常调度订单"登记表，用于记录无法调度(ERROR分组)的订单，
--       支持人工处理流转（查询 → 处理 → 关闭）
-- =====================================================

DROP TABLE IF EXISTS `pd_schedule_exception_order`;
CREATE TABLE `pd_schedule_exception_order` (
    `id`          varchar(64)  NOT NULL COMMENT '主键',
    `order_id`    varchar(64)  DEFAULT NULL COMMENT '订单ID',
    `agency_id`   varchar(64)  DEFAULT NULL COMMENT '当前机构ID（调度发生时所在网点）',
    `reason`      varchar(255) DEFAULT NULL COMMENT '异常原因（如：起始/目的机构信息缺失）',
    `status`      int(11)      DEFAULT '0' COMMENT '状态：0-待处理 1-已处理',
    `remark`      varchar(255) DEFAULT NULL COMMENT '处理备注',
    `create_time` datetime     DEFAULT NULL COMMENT '登记时间',
    `handle_time` datetime     DEFAULT NULL COMMENT '处理时间',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_status` (`status`),
    KEY `idx_agency_id` (`agency_id`),
    -- 仅对待处理(status=0)记录约束 order_id 唯一（MySQL 8.0.13+ 函数索引）：
    -- 已处理(status=1)的历史记录不阻塞后续调度周期重新登记与再次处理，
    -- 同时并发登记待处理记录时由该索引兜底幂等
    UNIQUE KEY `uk_order_pending` ((CASE WHEN `status` = 0 THEN `order_id` END))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常调度订单登记表';
