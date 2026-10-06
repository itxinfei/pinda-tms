-- ============================================================
-- pd_agency 机构主表 —— 建表 + 初始化（FR-00 配套，闭合机构主表缺口）
-- 生成日期：2026-10-06
-- 关联：docs/sql/种子数据初始化_FR00.sql
-- 背景：
--   经核查 pd_base 全库无任何 agency 主表，但 pd_transport_line /
--   pd_fleet / pd_agency_scope 的 agency_id 均为 varchar(20) NOT NULL，
--   且整库无外键约束。种子脚本已写入 agency_id=100/200（深圳/广州），
--   当前这些引用指向一张不存在的父表（逻辑悬空）。本脚本补齐 pd_agency，
--   使 agency_id 引用闭合。
-- 约定（与种子脚本一致）：
--   100=深圳总站  200=广州总站  300=东莞中转站  400=佛山中转站
-- 执行：
--   MYSQL_PWD=<pwd> mysql -h 192.168.20.130 -P 3306 -uroot pd_base \
--     < docs/sql/pd_agency_建表与初始化.sql
-- 说明：脚本幂等（IF NOT EXISTS + ON DUPLICATE KEY UPDATE），可重复执行。
-- ============================================================

USE pd_base;

-- 强制连接字符集为 utf8mb4，避免 Windows 客户端默认 latin1/gbk 导致中文乱码(ERROR 1366)
SET NAMES utf8mb4;

-- 幂等建表：已存在则跳过，避免重复执行报错
CREATE TABLE IF NOT EXISTS `pd_agency` (
  `id`            varchar(20)  NOT NULL                COMMENT '机构id',
  `name`          varchar(255) DEFAULT NULL           COMMENT '机构名称',
  `number`        varchar(255) DEFAULT NULL           COMMENT '机构编号',
  `address`       varchar(255) DEFAULT NULL           COMMENT '机构地址',
  `contact_name`  varchar(50)  DEFAULT NULL           COMMENT '联系人',
  `contact_phone` varchar(50)  DEFAULT NULL           COMMENT '联系人电话',
  `lon`           varchar(50)  DEFAULT NULL           COMMENT '经度',
  `lat`           varchar(50)  DEFAULT NULL           COMMENT '纬度',
  `manager`       varchar(50)  DEFAULT NULL           COMMENT '负责人',
  `status`        tinyint(2)   DEFAULT '1'            COMMENT '状态 0=禁用 1=启用',
  `create_date`   datetime     DEFAULT NULL           COMMENT '创建时间',
  `update_date`   datetime     DEFAULT NULL           COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='机构表';

-- 初始化数据（联调/测试用，与种子脚本 agency_id 段严格对齐）
INSERT INTO `pd_agency`
  (id, name, number, address, contact_name, contact_phone, lon, lat, manager, status, create_date, update_date)
VALUES
  ('100', '深圳总站',   'AG-SZ-001', '广东省深圳市', '张伟', '13800000001', '114.057', '22.543', '张伟', 1, NOW(), NOW()),
  ('200', '广州总站',   'AG-GZ-001', '广东省广州市', '王芳', '13800000002', '113.264', '23.129', '王芳', 1, NOW(), NOW()),
  ('300', '东莞中转站', 'AG-DG-001', '广东省东莞市', '陈明', '13800000003', '113.751', '23.020', '陈明', 1, NOW(), NOW()),
  ('400', '佛山中转站', 'AG-FS-001', '广东省佛山市', '刘强', '13800000004', '113.122', '23.021', '刘强', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name=VALUES(name), number=VALUES(number), address=VALUES(address),
  contact_name=VALUES(contact_name), contact_phone=VALUES(contact_phone),
  lon=VALUES(lon), lat=VALUES(lat), manager=VALUES(manager),
  status=VALUES(status), update_date=NOW();

-- 验证
SELECT id, name, number, manager, status FROM pd_agency ORDER BY id;
