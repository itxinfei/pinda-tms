-- ============================================================
-- 行政区划四级数据导入脚本（可重复执行）
-- 数据源：modood/Administrative-divisions-of-China（国家统计局口径）
--生成：2026-10-07    数据版本：2024-07
-- 内容：省31 / 市342 / 县2978 / 镇街道 41352 = 44703 条
-- ============================================================
-- ⚠️ 执行前必读
-- 1. 本脚本会清空 pd_area 表后重新导入，勿在生产有业务数据时执行
-- 2. 三个库都要执行：pd_auth / pd_aggregation / pinda_tms
-- 3. CSV 文件：docs/sql/区划数据/行政区划四级_20240701.csv
-- ============================================================

-- ---------- 步骤 0：建表（若不存在） ----------
CREATE TABLE IF NOT EXISTS `pd_area` (
  `id` int(11) NOT NULL COMMENT '本系统ID：level*1000000+序号',
  `parent_id` int(11) DEFAULT NULL COMMENT '父级ID，根节点为NULL',
  `name` varchar(255) DEFAULT NULL COMMENT '名称',
  `area_code` varchar(255) DEFAULT NULL COMMENT '行政区划代码(2/4/6/9位)',
  `city_code` varchar(255) DEFAULT NULL COMMENT '所属市代码',
  `merger_name` varchar(255) DEFAULT NULL COMMENT '合并名称',
  `short_name` varchar(255) DEFAULT NULL COMMENT '简称',
  `zip_code` varchar(255) DEFAULT NULL COMMENT '邮编',
  `level` tinyint(2) DEFAULT NULL COMMENT '0省 1市 2县 3镇/街道 4村',
  `category` varchar(10) DEFAULT NULL COMMENT '城乡分类:1城镇 2乡村(暂空)',
  `lng` varchar(255) DEFAULT NULL COMMENT '经度(待补)',
  `lat` varchar(255) DEFAULT NULL COMMENT '纬度(待补)',
  `pinyin` varchar(255) DEFAULT NULL COMMENT '拼音(待补)',
  `first` varchar(50) DEFAULT NULL COMMENT '首字母(待补)',
  `update_time` datetime DEFAULT NULL,
  `update_user` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `create_user` bigint(20) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行政区划';

-- ---------- 步骤 1：索引（幂等，先删后建避免重复键报错） ----------
DROP INDEX IF EXISTS idx_pd_area_parent ON pd_area;
DROP INDEX IF EXISTS idx_pd_area_code ON pd_area;
DROP INDEX IF EXISTS idx_pd_area_level ON pd_area;
CREATE INDEX idx_pd_area_parent ON pd_area(parent_id);
CREATE INDEX idx_pd_area_code ON pd_area(area_code);
CREATE INDEX idx_pd_area_level ON pd_area(level);

-- ---------- 步骤 2：清空并导入（由mysql 客户端执行 LOAD DATA） ----------
-- 备份当前数据（可选）：
-- CREATE TABLE pd_area_bak_YYYYMMDD AS SELECT * FROM pd_area;
TRUNCATE TABLE pd_area;

-- 用 mysql 命令行执行（注意 --local-infile=1）：
-- mysql -h<host> -u<user> -p<pwd> --default-character-set=utf8mb4 --local-infile=1 <db> < load_area.sql
--
-- LOAD DATA LOCAL INFILE 'D:/MyCode/pinda-tms/docs/sql/区划数据/行政区划四级_20240701.csv'
-- INTO TABLE pd_area
-- CHARACTER SET utf8mb4
-- FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"'
-- LINES TERMINATED BY '\n'
-- IGNORE 1 LINES
-- (id, parent_id, name, area_code, city_code, level, short_name,
--  @merger, @zip, @lvl_dummy, @lng, @lat, @pinyin, @first,
--  @update_time, @update_user, @create_time, @create_user);
-- 注意：CSV 列顺序为 id,parent_id,name,area_code,city_code,level,short_name
--       而表结构中 level 之后是 merger_name,short_name,zip_code,category,lng...
--       故用 @占位符跳过，实际以列名映射为准。

-- ---------- 步骤 3：修正根节点 ----------
-- CSV 中 parent_id=0 表示根，需转为 NULL（MyBatis 查询根节点用NULL 判断）
UPDATE pd_area SET parent_id = NULL WHERE parent_id = 0 AND level = 0;

-- ---------- 步骤 4：验证 ----------
-- 期望：level 0=31, 1=342, 2=2978, 3=41352，合计 44703
SELECT level, COUNT(*) AS 条数 FROM pd_area GROUP BY level ORDER BY level;

-- 期望：0（无孤儿节点）
SELECT COUNT(*) AS 孤儿节点数
FROM pd_area c LEFT JOIN pd_area p ON c.parent_id = p.id
WHERE c.level > 0 AND p.id IS NULL;

-- 期望：31
SELECT COUNT(*) AS 根节点数 FROM pd_area WHERE level = 0 AND parent_id IS NULL;

-- ---------- 步骤 5：按年增量更新（2025年及以后） ----------
-- 1. 从 https://github.com/modood/Administrative-divisions-of-China 下载最新 provinces/cities/areas/streets.json
-- 2. 重新运行 docs/sql/区划数据/导入脚本_生成CSV.py
-- 3. TRUNCATE + LOAD DATA 重新导入（行政区划调整多为新增/撤销，整体替换最稳妥）
-- 4. 若业务表已引用旧 area_id（province_id/city_id/county_id），
--    需写迁移脚本把「已删除的区划」映射到新的上级，否则历史数据会悬空。
--    建议届时用 LEFT JOIN 比对 area_code 生成映射表，人工确认后再迁移。