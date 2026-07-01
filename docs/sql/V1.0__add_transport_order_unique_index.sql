-- =====================================================
-- 品达TMS - 数据库迁移脚本
-- 版本: V1.0
-- 日期: 2026-07-01
-- 描述: 添加运单表order_id唯一索引，防止重复创建
-- =====================================================

-- 1. 检查 pd_transport_order 表是否存在
-- 2. 为 pd_transport_order 表的 order_id 字段添加唯一索引
--    目的：防止并发场景下重复创建运单

-- 注意：如果 order_id 已有重复数据，需要先清理重复数据再执行此脚本

-- 查看重复数据（如果存在会阻止唯一索引创建）
-- SELECT order_id, COUNT(*)
-- FROM pd_transport_order
-- WHERE order_id IS NOT NULL
-- GROUP BY order_id
-- HAVING COUNT(*) > 1;

-- 添加唯一索引
CREATE UNIQUE INDEX uk_transport_order_order_id
ON pd_transport_order (order_id)
WHERE order_id IS NOT NULL;

-- 如果已有重复数据，可以保留第一条，删除其他的
-- 备份表（可选）
-- CREATE TABLE pd_transport_order_backup AS SELECT * FROM pd_transport_order;

-- 删除重复数据（保留最早创建的记录）
-- DELETE FROM pd_transport_order
-- WHERE id NOT IN (
--     SELECT MIN(id)
--     FROM pd_transport_order
--     WHERE order_id IS NOT NULL
--     GROUP BY order_id
-- );

-- =====================================================
-- 业务逻辑说明
-- =====================================================
-- 业务变更：订单创建时自动创建运单
-- - 原逻辑：由快递员交件时创建运单
-- - 新逻辑：调度时自动创建运单（如果不存在）
--
-- 状态流转：
-- - 运输任务完成(4)
--   → 运单状态更新为"到达终端网点"(4)
--   → 订单状态更新为"已签收"(23009)
-- =====================================================
