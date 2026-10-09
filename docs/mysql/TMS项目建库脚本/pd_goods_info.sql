/*
 * pd_goods_info 商品信息表（DDL-only，1 张表）
 *
 * 来源：pd-authority/pd_goods_info.sql（无导出头，随权限系统讲义一起进仓）
 * 生成：2026-10-09，需求文档《52-旧代码独占知识固化_计费事件与异常上报契约》§4.1 顶层化产物。
 * 与来源差异：仅剔除 2 条演示 INSERT；建表语句逐字保留。
 *
 * 目标库：pd_base（依据 docs/05-部署文档.md §4.3 (3)：货物信息演示数据，无代码强引用，归入 pd_base）
 * 定性：遗留表。全仓 grep GoodsInfo / goods_info 在 *.java *.js *.vue 中 0 命中（仅演示 SQL 与文档提及），
 *       旧代码没有任何消费方；《45-数据库表结构设计_大模型专用》已标 ⚠️遗留表。
 *       重构若不做商品主数据可整表不建；本文件只为保住删码后无从考证的原始列定义。
 * 注意：本表未采用框架公共基类（publish_status/audit_status 为 tinyint，无 status_ 列），
 *       与 pd_auth 系列风格不同，不要当成统一规范。
 * ⚠️ 含 DROP TABLE IF EXISTS，只可用于全新建库，禁止直接对已有环境执行。
 */

create database if not exists `pd_base` default character set utf8mb4 collate utf8mb4_general_ci;
use `pd_base`;

SET FOREIGN_KEY_CHECKS=0;
-- ----------------------------
-- Table structure for `pd_goods_info`
-- ----------------------------
DROP TABLE IF EXISTS `pd_goods_info`;
CREATE TABLE `pd_goods_info` (
  `id` bigint(20) NOT NULL COMMENT '商品ID',
  `code` char(16) COLLATE utf8mb4_bin NOT NULL COMMENT '商品编码',
  `name` varchar(20) COLLATE utf8mb4_bin NOT NULL COMMENT '商品名称',
  `bar_code` varchar(50) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '国条码',
  `brand_id` bigint(20) DEFAULT NULL COMMENT '品牌表ID',
  `one_category_id` bigint(20) DEFAULT NULL COMMENT '一级分类ID',
  `two_category_id` bigint(20) DEFAULT NULL COMMENT '二级分类ID',
  `three_category_id` bigint(20) DEFAULT NULL COMMENT '三级分类ID',
  `supplier_id` bigint(20) DEFAULT NULL COMMENT '商品的供应商ID',
  `price` decimal(8,2) NOT NULL COMMENT '商品售价价格',
  `average_cost` decimal(18,2) NOT NULL COMMENT '商品加权平均成本',
  `publish_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '上下架状态:0下架，1上架',
  `audit_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '审核状态: 0未审核，1已审核',
  `weight` float DEFAULT NULL COMMENT '商品重量',
  `length` float DEFAULT NULL COMMENT '商品长度',
  `height` float DEFAULT NULL COMMENT '商品重量',
  `width` float DEFAULT NULL COMMENT '商品宽度',
  `color` varchar(20) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '颜色',
  `production_date` datetime NOT NULL COMMENT '生产日期',
  `shelf_life` int(11) NOT NULL COMMENT '商品有效期',
  `descript` text COLLATE utf8mb4_bin COMMENT '商品描述',
  `update_time` datetime DEFAULT NULL,
  `update_user` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='商品信息表';

