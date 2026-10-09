/*
 * Seata AT 回滚日志表 undo_log —— 7 个业务库一次性建表脚本
 *
 * 生成：2026-10-09，需求文档《52-旧代码独占知识固化_计费事件与异常上报契约》§4.1 顶层化产物。
 * 结构来源：docs/05-部署文档.md §4.4（声明与现网 SHOW CREATE TABLE 逐字一致），非凭空手写。
 * 依赖版本：io.seata:seata-spring-boot-starter:1.2.0（pom.xml:77-81）
 *          该版本客户端 io.seata.core.constants.ClientTableColumnsName 常量池只有 7 列：
 *          xid / branch_id / context / rollback_info / log_status / log_created / log_modified，
 *          没有 ext 列，也没有自增 id 主键 —— 与本脚本一致。升级到 Seata 1.4+ 时需另加 ext。
 * 为什么 7 个库都要建：各服务 enable-auto-data-source-proxy: true，
 *          任一库的数据源被纳入全局事务分支就必须有这张表，否则回滚日志写入直接失败。
 * 幂等：全部 CREATE DATABASE / CREATE TABLE IF NOT EXISTS，可重复执行，不动已有数据。
 */

create database if not exists `pd_aggregation` default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_auth`        default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_base`        default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_dispatch`    default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_oms`         default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_users`       default character set utf8mb4 collate utf8mb4_general_ci;
create database if not exists `pd_work`        default character set utf8mb4 collate utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `pd_aggregation`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_auth`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_base`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_dispatch`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_oms`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_users`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

CREATE TABLE IF NOT EXISTS `pd_work`.`undo_log` (
  `branch_id` BIGINT NOT NULL, `xid` VARCHAR(128) NOT NULL, `context` VARCHAR(128) NOT NULL,
  `rollback_info` LONGBLOB NOT NULL, `log_status` INT NOT NULL,
  `log_created` DATETIME(6) NOT NULL, `log_modified` DATETIME(6) NOT NULL,
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AT undo table';

-- ------------------------------------------------------------
-- 自检：应返回 7 行（另有历史库 pinda_tms 不在本脚本范围内）
-- ------------------------------------------------------------
-- SELECT table_schema, COUNT(*) AS cols
--   FROM information_schema.columns
--  WHERE table_name = 'undo_log' AND table_schema LIKE 'pd\_%'
--  GROUP BY table_schema ORDER BY table_schema;
