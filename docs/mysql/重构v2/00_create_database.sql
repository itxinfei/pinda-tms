-- =====================================================================
-- 品达 TMS 单库多租户重构 · 建库与全局规范
-- 制定：2026-10-10
-- 目标：合并旧 8 库为单一物理库，业务表统一 tenant_id 隔离；公共字典物理唯一。
-- =====================================================================
--
-- 【执行前置条件 / 库名策略】
--   最终库名 = pinda_tms。当前服务器上已存在旧 pinda_tms（大杂烩 65 表）。
--   正式执行本脚本前，需先把旧库整体改名保留（在「切换批次」单独操作，先报告）：
--     1) 新建 pinda_tms_legacy；
--     2) 逐表 RENAME TABLE pinda_tms.<t> TO pinda_tms_legacy.<t>;
--   旧库改名后再执行下方 CREATE DATABASE pinda_tms。
--
--   过渡期也可先把本脚本库名改为 pinda_tms_new 做演练，验证通过后再切换。
--
-- 【防注释乱码】所有建表会话必须显式：SET NAMES utf8mb4;
-- =====================================================================

CREATE DATABASE IF NOT EXISTS `pinda_tms`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_0900_ai_ci
  DEFAULT ENCRYPTION='N';

USE `pinda_tms`;

-- ---------------------------------------------------------------------
-- 全局字段规范（各表统一遵守）
-- ---------------------------------------------------------------------
-- 主键      id              BIGINT UNSIGNED   应用层雪花 @TableId(ASSIGN_ID)
-- 多租户    tenant_id       BIGINT UNSIGNED   平台表/字典表不带
-- 创建人    create_by       BIGINT UNSIGNED
-- 创建时间  create_time     DATETIME
-- 更新人    update_by       BIGINT UNSIGNED
-- 更新时间  update_time     DATETIME
-- 逻辑删除  deleted         TINYINT UNSIGNED DEFAULT 0   @TableLogic
-- 状态/枚举                 TINYINT UNSIGNED             Java 枚举映射，禁硬编码数字
-- 布尔                     TINYINT UNSIGNED DEFAULT 0
-- 金额                     DECIMAL(12,2)               禁用 float/double
-- 经度/纬度                DECIMAL(10,6)/DECIMAL(9,6)
-- ---------------------------------------------------------------------
