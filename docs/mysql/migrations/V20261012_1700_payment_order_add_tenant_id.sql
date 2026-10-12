-- +migrate Up
-- 任务: 全流程 E2E 实测——订单 9401 支付链路修复
-- 说明: pd_payment_order 补 tenant_id 租户列。多租户插件 TenantLineInnerInterceptor 对 oms 域
--       所有表追加 tenant_id 条件，该表缺列导致支付查询报 Unknown column 'tenant_id'，
--       被 Feign fallback 吞成"支付服务不可用"。逻辑同 docs/sql/pd_payment_order_加租户列_20261011.sql，
--       按迁移规范补 Up/Down 段。
-- 作者: atomcode  日期: 2026-10-12

-- 1) 加列+租户索引（幂等：列不存在才执行；先 DEFAULT 0 落地保证 NOT NULL 即时生效）
SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `pd_payment_order` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''租户ID（多租户插件写入时自动填充）'' AFTER `id`, ADD KEY `idx_tenant_status` (`tenant_id`,`status`)',
    'SELECT ''tenant_id already exists, skip'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pd_payment_order'
    AND COLUMN_NAME = 'tenant_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 按订单回填真实租户（支付单与订单一对一；当前无业务数据，预期 affected rows = 0）
UPDATE `pd_payment_order` p
  JOIN `oms_order` o ON o.`id` = p.`order_id`
SET p.`tenant_id` = o.`tenant_id`
WHERE p.`tenant_id` = 0;

-- 3) 去掉 DEFAULT 0（仍有默认值时才执行），与其他业务表口径一致
SET @ddl := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `pd_payment_order` ALTER COLUMN `tenant_id` DROP DEFAULT',
    'SELECT ''tenant_id default already dropped, skip'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pd_payment_order'
    AND COLUMN_NAME = 'tenant_id'
    AND COLUMN_DEFAULT IS NOT NULL);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- +migrate Down
-- 前提：表无业务数据（dev 环境已核实为空表），回滚即删索引+删列，不损失数据
SET @ddl := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `pd_payment_order` DROP KEY `idx_tenant_status`, DROP COLUMN `tenant_id`',
    'SELECT ''tenant_id not exists, skip'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pd_payment_order'
    AND COLUMN_NAME = 'tenant_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
