-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.6
-- 日期: 2026-08-11
-- 描述: 新增订单支付单表，支撑微信/支付宝/模拟渠道的统一支付流程
-- =====================================================

DROP TABLE IF EXISTS `pd_payment_order`;
CREATE TABLE `pd_payment_order` (
    `id`              varchar(64)  NOT NULL COMMENT '主键',
    `order_id`        varchar(64)  DEFAULT NULL COMMENT '订单ID',
    `pay_no`          varchar(64)  DEFAULT NULL COMMENT '支付流水号(系统生成)',
    `pay_channel`     varchar(16)  DEFAULT NULL COMMENT '支付渠道: wechat-微信 alipay-支付宝 mock-模拟',
    `amount`          decimal(10,2) DEFAULT NULL COMMENT '支付金额',
    `status`          int(11)      DEFAULT '0' COMMENT '状态: 0-待支付 1-已支付 2-已关闭 3-已退款',
    `prepay_params`   text         COMMENT '渠道预支付参数(JSON, 供前端拉起支付)',
    `channel_trade_no` varchar(64) DEFAULT NULL COMMENT '渠道交易号(支付成功后回填)',
    `pay_time`        datetime     DEFAULT NULL COMMENT '支付时间',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_id` (`order_id`),
    KEY `idx_pay_no` (`pay_no`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单支付单表';
