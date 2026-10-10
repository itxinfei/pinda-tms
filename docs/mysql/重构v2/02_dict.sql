-- =====================================================================
-- 域二：公共字典 dict_ · 物理唯一、全租户共享、不带 tenant_id
-- 来源：pinda_tms.pd_area（国标行政区划，外部导入数据）
-- 说明：dict_area.id 沿用国标行政区划 int 编码（非雪花），各业务表外键用 INT。
-- =====================================================================
SET NAMES utf8mb4;
USE `pinda_tms`;

DROP TABLE IF EXISTS `dict_area`;
CREATE TABLE `dict_area` (
  `id`          INT          NOT NULL COMMENT '行政区划国标编码',
  `parent_id`   INT          DEFAULT NULL COMMENT '父级编码',
  `name`        VARCHAR(255) NOT NULL COMMENT '名称',
  `area_code`   VARCHAR(20)  DEFAULT NULL COMMENT '区域编码',
  `city_code`   VARCHAR(20)  DEFAULT NULL COMMENT '城市编码',
  `merger_name` VARCHAR(255) DEFAULT NULL COMMENT '完整名称（省市区拼接）',
  `short_name`  VARCHAR(255) DEFAULT NULL COMMENT '简称',
  `zip_code`    VARCHAR(10)  DEFAULT NULL COMMENT '邮编',
  `level`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '层级 0省 1市 2县 3镇 4乡村',
  `category`    VARCHAR(10)  DEFAULT NULL COMMENT '城乡分类 1城镇 2乡村',
  `longitude`   DECIMAL(10,6) DEFAULT NULL COMMENT '经度(WGS84)',
  `latitude`    DECIMAL(9,6)  DEFAULT NULL COMMENT '纬度(WGS84)',
  `pinyin`      VARCHAR(255) DEFAULT NULL COMMENT '拼音',
  `first`       VARCHAR(10)  DEFAULT NULL COMMENT '首字母',
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level` (`level`),
  KEY `idx_area_code` (`area_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='行政区划字典';
