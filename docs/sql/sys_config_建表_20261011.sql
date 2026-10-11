-- ======================================================================
-- CFG-01 参数配置表（平台全局参数 / 第三方凭据，后台可改、业务热读取）
-- 库：pinda_tms（单库多租户）
-- 日期：2026-10-11
-- 说明：
--   1. 本表为参数配置唯一权威真源；业务服务本地短 TTL 缓存（默认 30s）直读本表，
--      库中无配置或配置停用时回落 Spring Environment（yml / 环境变量 / -D 启动参数）。
--   2. sensitive=1 的配置项，config_value 存 AES 密文（主密钥 pinda.config.crypto-key），
--      管理端列表/详情脱敏回显，编辑留空表示不修改。
--   3. 一期只做平台全局（tenant_id=0）；tenant_id 列预留多租户扩展。
--   4. 物理删除（不做逻辑删除）：参数表非业务单据，避免唯一键与逻辑删冲突；
--      需要审计留痕时由后续操作日志能力承担。
--   5. 种子值刻意留空：真实 AK 由管理员在后台填写，源码 / 镜像 / SQL 不包含真实凭据。
-- 幂等：可重复执行（CREATE TABLE IF NOT EXISTS + 不存在才插入的种子）。
-- ======================================================================

CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`           bigint unsigned NOT NULL COMMENT 'ID（雪花算法）',
  `tenant_id`    bigint unsigned NOT NULL DEFAULT 0 COMMENT '租户ID，0=平台全局（一期只使用0）',
  `config_name`  varchar(100)    NOT NULL DEFAULT '' COMMENT '参数名称',
  `config_key`   varchar(150)    NOT NULL DEFAULT '' COMMENT '参数键名（租户内唯一，如 baidu.map.ak）',
  `config_value` text            COMMENT '参数值（sensitive=1 时为AES密文）',
  `value_type`   varchar(20)     NOT NULL DEFAULT 'string' COMMENT '值类型：string/number/boolean/json',
  `is_sensitive` tinyint unsigned NOT NULL DEFAULT 0 COMMENT '是否敏感：1是（加密入库+脱敏回显） 0否',
  `status`       tinyint unsigned NOT NULL DEFAULT 1 COMMENT '状态：1启用 0停用（停用后业务侧回落Environment）',
  `remark`       varchar(255)    DEFAULT '' COMMENT '备注',
  `create_by`    bigint unsigned DEFAULT NULL COMMENT '创建人ID',
  `create_time`  datetime        DEFAULT NULL COMMENT '创建时间',
  `update_by`    bigint unsigned DEFAULT NULL COMMENT '更新人ID',
  `update_time`  datetime        DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_key` (`tenant_id`, `config_key`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='参数配置表';

-- ----------------------------------------------------------------------
-- 种子：百度地图服务端 AK（CFG-01 一期唯一打通的第三方凭据）
-- 值留空：上线后由管理员在「系统管理-参数配置」填写；未填写期间沿用
--        启动参数 -Dbaidu.map.ak / 环境变量 / yml 兜底。
-- ----------------------------------------------------------------------
INSERT INTO `sys_config`
  (`id`, `tenant_id`, `config_name`, `config_key`, `config_value`, `value_type`, `is_sensitive`, `status`, `remark`, `create_by`, `create_time`)
SELECT 1, 0, '百度地图服务端AK', 'baidu.map.ak', '', 'string', 1, 1,
       '百度地图开放平台 服务端AK（geocoding/directionlite）；后台修改后最长30秒全网生效，AK调用失败即时重读',
       NULL, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM `sys_config` WHERE `tenant_id` = 0 AND `config_key` = 'baidu.map.ak'
);
