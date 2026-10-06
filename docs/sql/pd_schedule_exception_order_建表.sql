-- 异常调度订单登记表（pd-dispatch）
-- 对应实体 com.itheima.pinda.entity.ScheduleExceptionOrder
-- 记录智能调度中无法完成线路规划（ERROR 分组，如起始/目的机构信息缺失）的订单，供运营人工处理
CREATE TABLE IF NOT EXISTS `pd_schedule_exception_order` (
  `id` varchar(64) NOT NULL COMMENT 'id',
  `order_id` varchar(64) DEFAULT NULL COMMENT '订单ID',
  `agency_id` varchar(64) DEFAULT NULL COMMENT '当前机构ID（调度发生时所在网点）',
  `reason` varchar(500) DEFAULT NULL COMMENT '异常原因',
  `status` int(4) DEFAULT '0' COMMENT '状态：0-待处理 1-已处理',
  `remark` varchar(500) DEFAULT NULL COMMENT '处理备注',
  `create_time` datetime DEFAULT NULL COMMENT '登记时间',
  `handle_time` datetime DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异常调度订单登记表';
