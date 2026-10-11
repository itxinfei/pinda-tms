-- ============================================================
-- 迁移：pd_payment_order 补 tenant_id 租户列
-- 日期：2026-10-11
-- 背景：多租户插件 TenantLineInnerInterceptor 对 oms 域所有表追加 tenant_id 条件，
--       pd_payment_order 此前无此列；全新环境建表脚本已同步加列，本脚本仅用于已建表的存量库。
-- 前提：在 pinda_tms 库执行；若 pd_payment_order 表尚不存在，直接执行
--       docs/sql/pd_payment_order_建表.sql（已含租户列），本脚本无需执行。
-- 特性：步骤 1/3 基于 information_schema 判断，可重复执行；步骤 2 为带 WHERE 的回填。
-- ============================================================

-- ------------------------------------------------------------
-- 步骤 1：加租户列与租户索引（列不存在时才执行）
--   先以 DEFAULT 0 落地，保证 NOT NULL 即时生效；步骤 3 回填后再去掉 DEFAULT，
--   最终态与 oms_order 等业务表一致（tenant_id BIGINT UNSIGNED NOT NULL，无默认值）。
-- ------------------------------------------------------------
SET @ddl := (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `pd_payment_order` ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT ''租户ID（多租户插件写入时自动填充）'' AFTER `id`, ADD KEY `idx_tenant_status` (`tenant_id`,`status`)',
    'SELECT ''tenant_id already exists, step 1 skipped'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pd_payment_order'
    AND COLUMN_NAME = 'tenant_id');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 步骤 2：按订单回填真实租户（支付单与订单一对一；当前无业务数据，预期 affected rows = 0）
--   UPDATE 带 WHERE，符合数据安全红线。
-- ------------------------------------------------------------
UPDATE `pd_payment_order` p
  JOIN `oms_order` o ON o.`id` = p.`order_id`
SET p.`tenant_id` = o.`tenant_id`
WHERE p.`tenant_id` = 0;

-- ------------------------------------------------------------
-- 步骤 3：去掉 DEFAULT 0（仍有默认值时才执行）
--   此后插件写入必须显式补 tenant_id，与其他业务表口径统一。
-- ------------------------------------------------------------
SET @ddl := (
  SELECT IF(COUNT(*) > 0,
    'ALTER TABLE `pd_payment_order` ALTER COLUMN `tenant_id` DROP DEFAULT',
    'SELECT ''tenant_id default already dropped, step 3 skipped'' AS msg')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'pd_payment_order'
    AND COLUMN_NAME = 'tenant_id'
    AND COLUMN_DEFAULT IS NOT NULL);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 验证（人工执行，预期）：
--   SHOW COLUMNS FROM pd_payment_order LIKE 'tenant_id';
--     → Type bigint unsigned, Null NO, Default NULL
--   SHOW INDEX FROM pd_payment_order WHERE Key_name = 'idx_tenant_status';
--     → 1 行
-- ============================================================
