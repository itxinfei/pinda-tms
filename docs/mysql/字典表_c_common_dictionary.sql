/*
 * 数据字典两表 DDL：c_common_dictionary / c_common_dictionary_item
 *
 * 生成：2026-10-09，需求文档《52-旧代码独占知识固化_计费事件与异常上报契约》§4.2 顶层化产物。
 *
 * ⚠️ 关键事实（务必先读，否则会把"反推 DDL"误当成"旧系统有一套字典实现"）：
 *   1. 全仓 grep `c_common_dictionary` 只命中两个实体类与文档，**没有任何建表脚本、没有 mapper、没有 XML**；
 *      即旧库很可能从未真正建过这两张表（现网是否存在需实测，见文末）。
 *   2. 全仓 grep `Dictionary`（*.java）只命中这两个实体类本身：
 *      pd-authority/pd-apps/pd-auth/pd-auth-entity/.../entity/common/Dictionary.java
 *      pd-authority/pd-apps/pd-auth/pd-auth-entity/.../entity/common/DictionaryItem.java
 *      —— 无 Controller / 无 Service / 无 Mapper，后端字典功能在旧代码里**从未实现**，属框架脚手架残留。
 *   3. 但前端契约是真实存在的（这才是需要固化的部分）：
 *      pd-admin-ui/src/api/Dictionary.js      → GET /authority/dictionary/page、POST|PUT|DELETE /authority/dictionary
 *      pd-admin-ui/src/api/DictionaryItem.js  → GET /authority/dictionaryItem/page、POST|PUT|DELETE /authority/dictionaryItem
 *      pd-admin-ui/src/views/pinda/base/dict/{Dictionary,DictionaryItem,DictionaryItemEdit,Edit,Index}.vue
 *      docs/sql/前端缺失接口补齐.sql:9-14 已为这 6 个端点注册过 pd_auth_resource —— 而端点服务端不存在，调用必然 404。
 *   4. 前端另有一个"枚举字典"入口与上面的库表**无关**，别混为一谈：
 *      GET /enums（AuthorityGeneralController.enums()，pd-auth-server，无类级 @RequestMapping）
 *      返回 Map<枚举简名, Map<code, desc>>，固定 5 组：HttpMethod / DataScopeType / LogType / AuthorizeType / Sex；
 *      code 取 BaseEnum.getCode()（各枚举均重写为 name()，故 key 是大写常量名，DataScopeType 也是 ALL/THIS_LEVEL/… 而非数字），
 *      desc 取中文。网关侧 GeneratorController 另有 /dictionary/enums 聚合入口（Feign 调 authorityGeneralApi.enums()）。
 *      前端 Common.js:5-11 已把调用路径从旧契约 /gate/dictionary/enums 订正为 /enums。
 *
 * 因此本脚本的定位是：《02-优先级总览》P1-21（数据字典+应用管理 CRUD，落点 pd-auth，BA-18 已闭合）
 * 的**建表基线**，不是旧实现快照。列名/长度/可空性逐列反推自上述两个实体（@TableField + @Length + @NotEmpty/@NotNull），
 * 公共审计列来自基类 com.itheima.pinda.base.entity.{SuperEntity,Entity}
 * （id/create_time/create_user/update_time/update_user；@TableId(type = IdType.INPUT) → 主键由应用侧发号，**不设自增**）。
 *
 * 待 P1-21 评审确认的设计约束（本脚本按最合理口径先落，标注为推断）：
 *   - UNIQUE(c_common_dictionary.code)：源自实体注释"一颗树仅仅有一个统一的编码"。
 *   - UNIQUE(c_common_dictionary_item(dictionary_id, code))：同树内编码唯一，源自 @NotEmpty 校验与页面按字典分组维护的用法。
 *   - 删除字典时若其下存在字典项需禁止删除（《22》§五 R-01 口径，不做级联删除）——该约束在代码层实现，本脚本不建外键。
 *   - 落点库默认 pd_auth（实体位于 pd-auth 模块）；若 P1-21 评审改为独立公共库需整体改名。
 *   - 表名保留实体里的 `c_common_` 前缀：BA-18 明令"禁止新建第二套字典服务/字典表"，
 *     故《22》若提出别的命名，必须以本文件为准修订文档，而不是另起一表。
 *
 * 幂等：CREATE TABLE IF NOT EXISTS，可重复执行，不动已有数据，不含 DROP。
 */

create database if not exists `pd_auth` default character set utf8mb4 collate utf8mb4_general_ci;

-- ----------------------------
-- 字典目录
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_auth`.`c_common_dictionary` (
  `id`          bigint(20)   NOT NULL                        COMMENT '主键（应用侧发号，非自增）',
  `code`        varchar(64)  NOT NULL                        COMMENT '编码：一颗树仅仅有一个统一的编码',
  `name`        varchar(64)  NOT NULL                        COMMENT '名称',
  `describe_`   varchar(200) DEFAULT ''                      COMMENT '描述',
  `status_`     bit(1)       DEFAULT b'1'                    COMMENT '状态：1启用 0停用',
  `create_user` bigint(20)   DEFAULT NULL                    COMMENT '创建人ID',
  `create_time` datetime     DEFAULT NULL                    COMMENT '创建时间',
  `update_user` bigint(20)   DEFAULT NULL                    COMMENT '最后修改人ID',
  `update_time` datetime     DEFAULT NULL                    COMMENT '最后修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dictionary_code` (`code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='字典目录';

-- ----------------------------
-- 字典项
-- ----------------------------
CREATE TABLE IF NOT EXISTS `pd_auth`.`c_common_dictionary_item` (
  `id`              bigint(20)   NOT NULL                    COMMENT '主键（应用侧发号，非自增）',
  `dictionary_id`   bigint(20)   NOT NULL                    COMMENT '字典id',
  `dictionary_code` varchar(64)  NOT NULL                    COMMENT '字典编码（冗余，便于按 code 取项）',
  `code`            varchar(64)  NOT NULL                    COMMENT '字典项编码',
  `name`            varchar(64)  NOT NULL                    COMMENT '字典项名称',
  `status_`         bit(1)       DEFAULT b'1'                COMMENT '状态：1启用 0停用',
  `describe_`       varchar(255) DEFAULT ''                  COMMENT '描述',
  `sort_value`      int(11)      DEFAULT '1'                 COMMENT '排序',
  `create_user`     bigint(20)   DEFAULT NULL                COMMENT '创建人ID',
  `create_time`     datetime     DEFAULT NULL                COMMENT '创建时间',
  `update_user`     bigint(20)   DEFAULT NULL                COMMENT '最后修改人ID',
  `update_time`     datetime     DEFAULT NULL                COMMENT '最后修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_dictionary_item_code` (`dictionary_id`, `code`) USING BTREE,
  KEY `idx_dictionary_item_dictionary_code` (`dictionary_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=COMPACT COMMENT='字典项';

-- ------------------------------------------------------------
-- 自检：确认两表结构与实体列一一对应
-- ------------------------------------------------------------
-- SELECT table_name, column_name, column_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_schema = 'pd_auth' AND table_name IN ('c_common_dictionary','c_common_dictionary_item')
--  ORDER BY table_name, ordinal_position;
--
-- 现网是否已存在这两张表：本脚本在本地代码树内推导，未对开发测试库执行任何探测
-- （Auto Mode 下不访问远端）。开工前请先跑一次
--   SHOW TABLES FROM pd_auth LIKE 'c_common_dictionary%';
-- 若已存在，先 SHOW CREATE TABLE 比对后再决定是否 ALTER。
