-- ============================================================================
-- 七段链路端到端打通：主数据修复 + 脏数据清理 + 结算建表
--
-- 目标：用一条真实订单走通「订单→运单→调度→运输任务→司机/快递员作业→轨迹→结算」，
--   每段都有可断言落库。本脚本负责清掉历史脏数据、补齐收发货区域归属主数据、
--   绑定真实可用的车与司机、登记一条可手动触发的全流程调度任务，并建结算两表。
--
-- 全部 DELETE 均带 WHERE 条件，仅点名指定数据；INSERT/UPDATE 幂等可重复执行。
--
-- 关键口径（2026-10-08 实测）：
--   网点 pd_base.pd_agency      深圳总站=100            广州总站=200
--   区县 pd_auth.pd_area        深圳罗湖=2001852        广州天河=2001834
--   城市                        深圳市=1000198          广州市=1000196
--   早班车次                    深圳-广州 08:00 = 2900000000000000001
--   可用车辆                    粤B12345 = 2200000000000000001（status=1）
--   真实司机/快递员/客户         driver03=9103 courier01=9201 courier02=9202 customer01=9203
--
-- 执行（口令取服务器 .env，不写进脚本）：
--   DB_PASS=$(sed -n 's/^MYSQL_ROOT_PASSWORD=//p' /data/deploy/middleware/.env)
--   docker exec -i mysql57 mysql -uroot -p"$DB_PASS" < docs/sql/端到端打通_主数据与清理.sql
-- 告警表 pd_alarm_record 另由 docs/sql/告警记录_建表.sql 在 pd_oms 创建。
-- ============================================================================

-- ----------------------------------------------------------------------------
-- Part A：结算两表建表（pd_oms，幂等）
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_freight_detail` (
  `id` varchar(64) NOT NULL COMMENT '主键（雪花id）',
  `order_id` varchar(64) NOT NULL COMMENT '订单id',
  `transport_order_id` varchar(64) DEFAULT NULL COMMENT '运单id',
  `fee_item` varchar(32) NOT NULL COMMENT '费用项编码：FREIGHT-运费',
  `fee_item_name` varchar(32) DEFAULT NULL COMMENT '费用项名称',
  `quantity` decimal(10,2) DEFAULT NULL COMMENT '数量',
  `unit` varchar(16) DEFAULT NULL COMMENT '计量单位：票',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `amount` decimal(12,2) NOT NULL COMMENT '金额（订单总价）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_transport_order_id` (`transport_order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费明细';

CREATE TABLE IF NOT EXISTS `pd_oms`.`pd_settlement_order` (
  `id` varchar(64) NOT NULL COMMENT '主键（雪花id）',
  `settlement_no` varchar(64) NOT NULL COMMENT '结算单号，STL+雪花id',
  `order_id` varchar(64) NOT NULL COMMENT '订单id',
  `transport_order_id` varchar(64) DEFAULT NULL COMMENT '运单id',
  `settle_object_type` tinyint(4) NOT NULL COMMENT '结算对象类型：1-客户 2-司机 3-承运商',
  `settle_object_id` varchar(64) NOT NULL COMMENT '结算对象id，散客兜底为 GUEST',
  `direction` tinyint(4) NOT NULL COMMENT '方向：1-应收 2-应付',
  `period_start` date DEFAULT NULL COMMENT '账期开始日期',
  `period_end` date DEFAULT NULL COMMENT '账期结束日期',
  `order_count` int(11) DEFAULT '1' COMMENT '关联订单数',
  `receivable_amount` decimal(12,2) DEFAULT '0.00' COMMENT '应收金额',
  `payable_amount` decimal(12,2) DEFAULT '0.00' COMMENT '应付金额',
  `settled_amount` decimal(12,2) DEFAULT '0.00' COMMENT '已结算金额',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-待对账 1-已对账 2-已结算',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_settlement_no` (`settlement_no`),
  KEY `idx_settle_object` (`settle_object_type`,`settle_object_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='结算单';

-- ----------------------------------------------------------------------------
-- Part B：清理历史脏数据（全部带 WHERE，仅删指定 id）
-- ----------------------------------------------------------------------------

-- B1. 脏订单连带的货物、位置信息
DELETE FROM `pd_oms`.`pd_order_cargo`
 WHERE `order_id` IN ('PJ0001','PJ0002','PJ0003');

DELETE FROM `pd_oms`.`pd_order_location`
 WHERE `order_id` IN ('PJ0001','PJ0002','PJ0003');

-- B2. 脏订单（status=1 为错误口径，member 为孤儿会员）
DELETE FROM `pd_oms`.`pd_order`
 WHERE `id` IN ('PJ0001','PJ0002','PJ0003');

-- B3. 孤儿运输任务（无运单关联）
DELETE FROM `pd_work`.`pd_task_transport`
 WHERE `id` IN ('RW0001','RW0002');

-- B4. 脏运单
DELETE FROM `pd_work`.`pd_transport_order`
 WHERE `id` IN ('TY0001','TY0002','TY0003');

-- ----------------------------------------------------------------------------
-- Part C：主数据修复
-- ----------------------------------------------------------------------------

-- C1. 网点业务范围：深圳罗湖 → 深圳总站(100)；广州天河 → 广州总站(200)
--     muti_points 为区县包围多边形（点键 lng/lat）；确定性兜底链路按 area_id 精确匹配。
DELETE FROM `pd_base`.`pd_agency_scope`
 WHERE `area_id` IN ('2001852','2001834');

INSERT INTO `pd_base`.`pd_agency_scope` (`id`,`agency_id`,`area_id`,`muti_points`) VALUES
 ('as_sz_2001852','100','2001852',
  '[[{"lng":114.10,"lat":22.53},{"lng":114.17,"lat":22.53},{"lng":114.17,"lat":22.58},{"lng":114.10,"lat":22.58}]]'),
 ('as_gz_2001834','200','2001834',
  '[[{"lng":113.29,"lat":23.10},{"lng":113.37,"lat":23.10},{"lng":113.37,"lat":23.17},{"lng":113.29,"lat":23.17}]]');

-- C2. 快递员业务范围：9201 → 深圳罗湖(2001852)；9202 → 广州天河(2001834)
--     旧记录误关联 area_id=100，先按 user_id 删除再插。
DELETE FROM `pd_base`.`pd_courier_scop`
 WHERE `user_id` IN ('9201','9202');

INSERT INTO `pd_base`.`pd_courier_scop` (`id`,`user_id`,`area_id`,`muti_points`) VALUES
 ('courier_scope_9201','9201','2001852',
  '[[{"lng":114.10,"lat":22.53},{"lng":114.17,"lat":22.53},{"lng":114.17,"lat":22.58},{"lng":114.10,"lat":22.58}]]'),
 ('courier_scope_9202','9202','2001834',
  '[[{"lng":113.29,"lat":23.10},{"lng":113.37,"lat":23.10},{"lng":113.37,"lat":23.17},{"lng":113.29,"lat":23.17}]]');

-- C3. 调度任务：清掉 5 条 business_id 为伪值的演示任务，登记一条真实全流程任务
--     business_id=100（深圳总站，pd_core_org 中存在该 id），bean_name=dispatchTask。
--     cron 设为每天凌晨低频执行，跑单以手动 PUT /schedule/run 触发，避免中途自动调度干扰。
DELETE FROM `pd_dispatch`.`schedule_job`
 WHERE `id` IN ('3A00000000000000001','3A00000000000000002','3A00000000000000003',
                '3A00000000000000004','3A00000000000000005');

INSERT INTO `pd_dispatch`.`schedule_job`
 (`id`,`bean_name`,`params`,`cron_expression`,`status`,`remark`,`business_id`,`create_date`)
 VALUES
 ('job_dispatch_sz_100','dispatchTask','DISTANCE','0 13 2 * * ?',1,
  '深圳总站全流程调度（分类→线路→运单→车次车司机）','100', NOW());

-- C4. 早班车次绑定改为真实司机：卡车 2200000000000000001(status=1) + 司机 9103(启用)
--     同一车次另一条绑定仍为不存在于 pd_auth_user 的假司机，会被可用性过滤排除，
--     故调度必然选中「卡车001 + 司机9103」这一组合。
UPDATE `pd_base`.`pd_transport_trips_truck_driver`
   SET `user_id` = '9103'
 WHERE `id` = '2A00000000000000001';

-- ----------------------------------------------------------------------------
-- Part D：执行后自检
-- ----------------------------------------------------------------------------

-- D1. 结算两表期望均存在（exists_flag=1）
SELECT 'pd_freight_detail' AS table_name, COUNT(*) AS exists_flag
  FROM information_schema.tables
 WHERE table_schema='pd_oms' AND table_name='pd_freight_detail'
UNION ALL
SELECT 'pd_settlement_order', COUNT(*)
  FROM information_schema.tables
 WHERE table_schema='pd_oms' AND table_name='pd_settlement_order';

-- D2. 脏数据期望全部为 0
SELECT 'dirty_order' AS item, COUNT(*) AS cnt FROM `pd_oms`.`pd_order`
 WHERE id IN ('PJ0001','PJ0002','PJ0003')
UNION ALL
SELECT 'dirty_transport_order', COUNT(*) FROM `pd_work`.`pd_transport_order`
 WHERE id IN ('TY0001','TY0002','TY0003')
UNION ALL
SELECT 'orphan_task', COUNT(*) FROM `pd_work`.`pd_task_transport`
 WHERE id IN ('RW0001','RW0002');

-- D3. 主数据计数期望：agency_scope=2, courier_scop=2, schedule_job(business 100)=1,
--     早班绑定司机=9103
SELECT 'agency_scope' AS item, COUNT(*) AS cnt FROM `pd_base`.`pd_agency_scope`
 WHERE area_id IN ('2001852','2001834')
UNION ALL
SELECT 'courier_scop', COUNT(*) FROM `pd_base`.`pd_courier_scop`
 WHERE user_id IN ('9201','9202')
UNION ALL
SELECT 'schedule_job_100', COUNT(*) FROM `pd_dispatch`.`schedule_job`
 WHERE business_id='100';

SELECT `transport_trips_id`,`truck_id`,`user_id`
  FROM `pd_base`.`pd_transport_trips_truck_driver`
 WHERE `id`='2A00000000000000001';
