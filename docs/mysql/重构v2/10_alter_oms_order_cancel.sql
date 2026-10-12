-- =====================================================================
-- 增量：订单取消原因/取消时间（管理端订单取消功能，2026-10-12）
-- 状态仍写 oms_order.status=12（已取消），状态合法性由 OrderServiceImpl 矩阵校验
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

ALTER TABLE `oms_order`
  ADD COLUMN `cancel_reason` VARCHAR(200) DEFAULT NULL COMMENT '取消原因' AFTER `status`,
  ADD COLUMN `cancel_time`   DATETIME     DEFAULT NULL COMMENT '取消时间' AFTER `cancel_reason`;
