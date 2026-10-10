-- ============================================================
-- 支付单表 pd_payment_order（P0）
-- 对应实体: com.itheima.pinda.entity.PaymentOrder
-- 读写来源: com.itheima.pinda.service.impl.PayServiceImpl
--   - createPayment(): 按订单创建支付单（save），并发时依赖 uk_order_id 唯一索引兜底复用
--   - handleCallback(): 按 pay_no 查单，回调后置已支付、回填渠道交易号与支付时间
--   - refund(): 置已退款
-- 适用版本: MySQL 5.7，CREATE TABLE IF NOT EXISTS 可重复执行
-- ============================================================

CREATE TABLE IF NOT EXISTS `pd_payment_order` (
  `id`               VARCHAR(64)    NOT NULL                COMMENT '主键（雪花id，应用生成）',
  `order_id`         BIGINT         NOT NULL                COMMENT '订单id（关联 oms_order.id，雪花 BIGINT）',
  `pay_no`           VARCHAR(64)    NOT NULL                COMMENT '支付流水号（系统生成，按此查询并处理回调）',
  `pay_channel`      VARCHAR(32)    NOT NULL DEFAULT 'mock' COMMENT '支付渠道: wechat-微信 alipay-支付宝 mock-模拟',
  `amount`           DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '支付金额（取订单金额，回调时校验一致性）',
  `status`           INT(4)         NOT NULL DEFAULT 0      COMMENT '状态: 0-待支付 1-已支付 2-已关闭 3-已退款',
  `prepay_params`    TEXT           NULL                    COMMENT '渠道预支付参数（JSON，供前端拉起支付）',
  `channel_trade_no` VARCHAR(64)    NULL                    COMMENT '渠道交易号（支付成功回调回填）',
  `pay_time`         DATETIME       NULL                    COMMENT '支付时间（支付成功回调写入）',
  `create_time`      DATETIME       NOT NULL                COMMENT '创建时间',
  `update_time`      DATETIME       NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_no` (`pay_no`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单支付单（统一承载微信/支付宝/模拟渠道支付流水）';

-- 说明（现状口径，非本脚本问题）：
-- 1. uk_order_id 与 PayServiceImpl 的并发兜底逻辑（捕获 DuplicateKeyException 后复用已有支付单）配套，
--    表示一个订单全生命周期只允许一条支付单记录；若后续要支持"关闭/退款后重新发起支付"，
--    需先调整 PayServiceImpl 并发口径，再将本唯一索引改为普通索引或联合唯一索引。
-- 2. 全新环境先执行本脚本再走支付流程，支付单落库不再因缺表报 500。
