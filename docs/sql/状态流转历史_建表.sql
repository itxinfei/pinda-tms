-- ============================================================
-- 状态流转历史建表脚本（pd_work 库）
--
-- 背景：pd-work 里 StatusTransitionHistory 这条链是完整的——实体
--   （entity/state/StatusTransitionHistory.java:24 @TableName）、Mapper、
--   ServiceImpl、以及 TaskTransportServiceImpl 的发车/到达/派送三处调用都在，
--   但仓库里从来没有这张表的 DDL，线上 pd_work 也查不到它（2026-10-08 实测）。
--   StatusTransitionHistoryServiceImpl:53-55 的 catch 只记 error 并 return false，
--   调用方也不看返回值，所以症状不是"业务失败"，而是**审计留证一条都没落下来**。
--   对物流单来说，"谁在什么时候把状态从几改到几"正是合规要的东西，
--   表缺失会一直静默着，等到需要举证时才发现没有数据。
--
-- 幂等：CREATE TABLE IF NOT EXISTS，可重复执行；不删不改现有数据。
--
-- 执行（口令不写在脚本里，从服务器 .env 取）：
--   P=$(docker exec mysql57 printenv MYSQL_ROOT_PASSWORD)
--   docker exec -i mysql57 mysql -uroot -p"$P" < docs/sql/状态流转历史_建表.sql
-- ============================================================

USE `pd_work`;

CREATE TABLE IF NOT EXISTS `pd_status_transition_history` (
  `id`             varchar(64)  NOT NULL COMMENT '主键（CustomIdGenerator 雪花算法，实体里是 String）',
  `business_type`  int(11)      DEFAULT NULL COMMENT '业务类型: 1-运输任务（与 TaskTransportServiceImpl.BUSINESS_TYPE_TRANSPORT_TASK 对齐）',
  `business_id`    varchar(64)  DEFAULT NULL COMMENT '业务主键',
  `business_no`    varchar(64)  DEFAULT NULL COMMENT '业务单号',
  `operation_type` int(11)      DEFAULT NULL COMMENT '操作类型: 1-状态变更',
  `before_status`  int(11)      DEFAULT NULL COMMENT '变更前状态',
  `after_status`   int(11)      DEFAULT NULL COMMENT '变更后状态',
  `operator_id`    varchar(64)  DEFAULT NULL COMMENT '操作人 id（取自网关透传的身份头）',
  `operator_name`  varchar(64)  DEFAULT NULL COMMENT '操作人姓名',
  `operator_type`  int(11)      DEFAULT NULL COMMENT '操作人类型: 1-后台 2-司机 3-快递员',
  `remark`         varchar(255) DEFAULT NULL COMMENT '备注，如"发车确认"',
  `operate_time`   datetime     DEFAULT NULL COMMENT '操作时间',
  `create_time`    datetime     DEFAULT NULL COMMENT '记录创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_business` (`business_type`, `business_id`),
  KEY `idx_operate_time` (`operate_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '业务状态流转审计历史';

-- 自检：表存在，且列与实体的 11 个业务字段一一对上
SELECT COUNT(*) AS cols_now
  FROM information_schema.columns
 WHERE table_schema = 'pd_work' AND table_name = 'pd_status_transition_history';
