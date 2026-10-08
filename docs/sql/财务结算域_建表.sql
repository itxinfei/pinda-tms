-- ============================================================
-- 财务结算域建表脚本
-- 依据: docs/需求文档/财务结算与调度运力IoT任务卡.md（FIN-1 ~ FIN-4）
-- 分层:
--   一、P0 最小闭环（签收即生成，单笔）
--       pd_freight_detail      运费明细（对应实体 FreightDetail）
--       pd_settlement_order    结算单（对应实体 SettlementOrder）
--   二、FIN-1 客户月结账单层（按客户+账期归集）
--       pd_freight_bill / pd_freight_bill_detail
--   三、FIN-1.1 客户价格协议 pd_price_agreement
--   四、FIN-1.2 电子发票 pd_invoice（不接真实税控）
--   五、FIN-2 司机趟次工资
--       pd_driver_settlement / pd_driver_settlement_detail
--   六、FIN-3 账期/回款/坏账
--       pd_customer_credit / pd_payment_record
--   七、FIN-4 利润：不建表，按 pd_task_cost + 订单金额查询计算
-- 适用版本: MySQL 5.7（不使用窗口函数），全部 CREATE TABLE IF NOT EXISTS，可重复执行
-- ============================================================


-- ============================================================
-- 一、P0 最小闭环
-- ============================================================

-- 运费明细：按费用项记录计费结果，签收后自动落库，可对账可追溯
CREATE TABLE IF NOT EXISTS `pd_freight_detail` (
  `id`                 VARCHAR(64)    NOT NULL                COMMENT '主键（雪花id）',
  `order_id`           VARCHAR(64)    NOT NULL                COMMENT '订单id（关联 pd_order.id）',
  `transport_order_id` VARCHAR(64)    NULL                    COMMENT '运单id',
  `fee_item`           VARCHAR(32)    NOT NULL                COMMENT '费用项编码: FREIGHT-运费；后续扩展首重/续重/保价/上楼等',
  `fee_item_name`      VARCHAR(64)    NOT NULL                COMMENT '费用项名称',
  `quantity`           DECIMAL(12,2)  NULL                    COMMENT '数量（重量/件数/票数）',
  `unit`               VARCHAR(16)    NULL                    COMMENT '计量单位（票/kg/方等）',
  `unit_price`         DECIMAL(12,2)  NULL                    COMMENT '单价',
  `amount`             DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '金额',
  `create_time`        DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_fee` (`order_id`, `fee_item`),
  KEY `idx_transport_order` (`transport_order_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费明细（按费用项，签收后自动生成）';

-- 结算单：记录结算对象、应收/应付方向、金额与对账状态
CREATE TABLE IF NOT EXISTS `pd_settlement_order` (
  `id`                 VARCHAR(64)    NOT NULL                COMMENT '主键（雪花id）',
  `settlement_no`      VARCHAR(64)    NOT NULL                COMMENT '结算单号',
  `order_id`           VARCHAR(64)    NOT NULL                COMMENT '订单id',
  `transport_order_id` VARCHAR(64)    NULL                    COMMENT '运单id',
  `settle_object_type` INT(4)         NOT NULL                COMMENT '结算对象类型: 1-客户 2-司机 3-承运商',
  `settle_object_id`   VARCHAR(64)    NOT NULL                COMMENT '结算对象id（散客兜底 GUEST）',
  `direction`          INT(4)         NOT NULL                COMMENT '方向: 1-应收 2-应付',
  `period_start`       DATE           NULL                    COMMENT '周期开始日期',
  `period_end`         DATE           NULL                    COMMENT '周期结束日期',
  `order_count`        INT(11)        NOT NULL DEFAULT 1      COMMENT '关联订单数',
  `receivable_amount`  DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '应收金额',
  `payable_amount`     DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '应付金额',
  `settled_amount`     DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '已结算金额',
  `status`             INT(4)         NOT NULL DEFAULT 0      COMMENT '状态: 0-待对账 1-已对账 2-已结算',
  `remark`             VARCHAR(255)   NULL                    COMMENT '备注',
  `create_time`        DATETIME       NOT NULL                COMMENT '创建时间',
  `update_time`        DATETIME       NOT NULL                COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_object` (`settle_object_type`, `settle_object_id`),
  KEY `idx_status_period` (`status`, `period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单（签收后自动生成，最小闭环）';


-- ============================================================
-- 二、FIN-1 客户运费账单（对客户/加盟方的应收账单，按账期归集）
-- ============================================================

CREATE TABLE IF NOT EXISTS `pd_freight_bill` (
  `id`           VARCHAR(64)    NOT NULL                COMMENT '主键',
  `bill_no`      VARCHAR(64)    NOT NULL                COMMENT '账单号',
  `member_id`    VARCHAR(64)    NOT NULL                COMMENT '客户id',
  `period_start` DATE           NOT NULL                COMMENT '账期开始',
  `period_end`   DATE           NOT NULL                COMMENT '账期结束',
  `order_count`  INT(11)        NOT NULL DEFAULT 0      COMMENT '运单数',
  `total_amount` DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '应收总额',
  `paid_amount`  DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '已收金额',
  `status`       INT(4)         NOT NULL DEFAULT 0      COMMENT '0待结算 1部分结算 2已结算 3已坏账',
  `due_date`     DATE           NULL                    COMMENT '到期日',
  `remark`       VARCHAR(255)   NULL                    COMMENT '备注',
  `create_time`  DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  KEY `idx_member` (`member_id`),
  KEY `idx_status_due` (`status`, `due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户运费账单';

CREATE TABLE IF NOT EXISTS `pd_freight_bill_detail` (
  `id`                 VARCHAR(64)    NOT NULL                COMMENT '主键',
  `bill_id`            VARCHAR(64)    NOT NULL                COMMENT '账单id',
  `order_id`           VARCHAR(64)    NULL                    COMMENT '订单id',
  `transport_order_id` VARCHAR(64)    NULL                    COMMENT '运单id',
  `base_freight`       DECIMAL(12,2)  NULL                    COMMENT '基础运费',
  `extra_freight`      DECIMAL(12,2)  NULL                    COMMENT '附加费（保价/上楼/超区等）',
  `adjust_amount`      DECIMAL(12,2)  NULL                    COMMENT '调整金额（争议后调整，调账新增行留痕）',
  `amount`             DECIMAL(12,2)  NOT NULL                COMMENT '小计',
  `create_time`        DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费账单明细';


-- ============================================================
-- 三、FIN-1.1 客户价格协议（月结/企业客户专属计价，命中优先于默认 Drools 规则）
-- ============================================================

CREATE TABLE IF NOT EXISTS `pd_price_agreement` (
  `id`            VARCHAR(64)    NOT NULL                COMMENT '主键',
  `member_id`     VARCHAR(64)    NOT NULL                COMMENT '客户id（月结/企业客户）',
  `line_id`       VARCHAR(64)    NULL                    COMMENT '线路id（空=全线路）',
  `goods_type_id` VARCHAR(64)    NULL                    COMMENT '货物类型id（空=全部）',
  `weight_min`    DECIMAL(10,2)  NULL                    COMMENT '重量段下限(kg)',
  `weight_max`    DECIMAL(10,2)  NULL                    COMMENT '重量段上限(kg)',
  `price_type`    INT(4)         NOT NULL                COMMENT '计价方式: 1按票 2按吨 3按方',
  `price`         DECIMAL(12,2)  NOT NULL                COMMENT '单价',
  `discount`      DECIMAL(5,2)   NOT NULL DEFAULT 1.00   COMMENT '月结折扣（0.90=9折）',
  `effective_from` DATE          NOT NULL                COMMENT '生效日期',
  `effective_to`   DATE          NULL                    COMMENT '失效日期（空=长期有效）',
  `status`        INT(4)         NOT NULL DEFAULT 1      COMMENT '状态: 1生效 0停用',
  `create_time`   DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_member` (`member_id`),
  KEY `idx_effective` (`status`, `effective_from`, `effective_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户价格协议';


-- ============================================================
-- 四、FIN-1.2 电子发票（只做状态管理，不接真实税控/数电票）
-- ============================================================

CREATE TABLE IF NOT EXISTS `pd_invoice` (
  `id`           VARCHAR(64)    NOT NULL                COMMENT '主键',
  `invoice_no`   VARCHAR(64)    NULL                    COMMENT '发票号（税控开具后回填；测试环境自编 TEST-前缀）',
  `bill_id`      VARCHAR(64)    NOT NULL                COMMENT '关联账单（pd_freight_bill.id）',
  `member_id`    VARCHAR(64)    NOT NULL                COMMENT '客户id',
  `amount`       DECIMAL(14,2)  NOT NULL                COMMENT '开票金额（须等于账单金额）',
  `type`         INT(4)         NOT NULL DEFAULT 1      COMMENT '发票类型: 1增值税普票 2增值税专票',
  `status`       INT(4)         NOT NULL DEFAULT 1      COMMENT '状态: 1待申请 2已申请 3已开具 4已邮寄 5已作废',
  `tax_no`       VARCHAR(64)    NULL                    COMMENT '客户税号',
  `mail_address` VARCHAR(255)   NULL                    COMMENT '邮寄地址',
  `create_time`  DATETIME       NOT NULL                COMMENT '创建时间',
  `invoice_time` DATETIME       NULL                    COMMENT '开具时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子发票';


-- ============================================================
-- 五、FIN-2 司机趟次工资结算
-- ============================================================

CREATE TABLE IF NOT EXISTS `pd_driver_settlement` (
  `id`           VARCHAR(64)    NOT NULL                COMMENT '主键',
  `settle_no`    VARCHAR(64)    NOT NULL                COMMENT '结算单号',
  `driver_id`    VARCHAR(64)    NOT NULL                COMMENT '司机id',
  `period_start` DATE           NOT NULL                COMMENT '周期开始',
  `period_end`   DATE           NOT NULL                COMMENT '周期结束',
  `trip_count`   INT(11)        NOT NULL DEFAULT 0      COMMENT '完成趟次',
  `base_wage`    DECIMAL(12,2)  NULL                    COMMENT '底薪',
  `trip_fee`     DECIMAL(12,2)  NULL                    COMMENT '趟次费合计',
  `allowance`    DECIMAL(12,2)  NULL                    COMMENT '补贴（油补/过路/餐补）',
  `deduction`    DECIMAL(12,2)  NULL                    COMMENT '扣款（违章/货损/迟到）',
  `total_amount` DECIMAL(12,2)  NOT NULL                COMMENT '应发合计',
  `status`       INT(4)         NOT NULL DEFAULT 0      COMMENT '状态: 0待审 1已审 2已发放',
  `create_time`  DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settle_no` (`settle_no`),
  KEY `idx_driver` (`driver_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机结算单';

CREATE TABLE IF NOT EXISTS `pd_driver_settlement_detail` (
  `id`                VARCHAR(64)    NOT NULL                COMMENT '主键',
  `settlement_id`     VARCHAR(64)    NOT NULL                COMMENT '结算单id',
  `task_transport_id` VARCHAR(64)    NULL                    COMMENT '趟次（运输任务）id',
  `driver_job_id`     VARCHAR(64)    NULL                    COMMENT '司机作业id',
  `amount`            DECIMAL(12,2)  NOT NULL                COMMENT '该趟金额',
  `remark`            VARCHAR(255)   NULL                    COMMENT '备注',
  `create_time`       DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_settle` (`settlement_id`),
  KEY `idx_task` (`task_transport_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='司机结算明细（按趟次）';


-- ============================================================
-- 六、FIN-3 账期/回款/坏账
-- ============================================================

-- 客户账期与信用
CREATE TABLE IF NOT EXISTS `pd_customer_credit` (
  `id`                 VARCHAR(64)    NOT NULL                COMMENT '主键',
  `member_id`          VARCHAR(64)    NOT NULL                COMMENT '客户id',
  `credit_period_days` INT(11)        NOT NULL DEFAULT 0      COMMENT '账期天数',
  `credit_limit`       DECIMAL(14,2)  NULL                    COMMENT '信用额度',
  `used_amount`        DECIMAL(14,2)  NOT NULL DEFAULT 0.00   COMMENT '已用额度',
  `risk_level`         INT(4)         NOT NULL DEFAULT 0      COMMENT '风险等级: 0正常 1关注 2预警 3黑名单',
  `create_time`        DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户账期与信用';

-- 收付款流水（应收登记/回款/坏账核销/退款）
CREATE TABLE IF NOT EXISTS `pd_payment_record` (
  `id`          VARCHAR(64)    NOT NULL                COMMENT '主键',
  `bill_id`     VARCHAR(64)    NULL                    COMMENT '关联账单id',
  `type`        INT(4)         NOT NULL                COMMENT '类型: 1应收登记 2回款 3坏账核销 4退款',
  `amount`      DECIMAL(14,2)  NOT NULL                COMMENT '金额',
  `pay_method`  INT(4)         NULL                    COMMENT '支付方式: 1现金 2转账 3在线支付 4到付核销',
  `occur_time`  DATETIME       NOT NULL                COMMENT '发生时间',
  `operator`    VARCHAR(64)    NULL                    COMMENT '操作人id',
  `remark`      VARCHAR(255)   NULL                    COMMENT '备注',
  `create_time` DATETIME       NOT NULL                COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_bill` (`bill_id`),
  KEY `idx_occur_time` (`occur_time`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收付款流水';


-- ============================================================
-- 七、FIN-4 每趟/每单利润：不建表
--   趟次利润 = Σ(该趟关联订单 Order.amount) − Σ(pd_task_cost where task_transport_id)
--   订单利润 = Order.amount − 分摊成本
--   pd_task_cost 由运营可视化任务卡 OPS-4 另行建表；MySQL 5.7 用 SUM(CASE WHEN) 聚合，禁用窗口函数。
-- ============================================================
