-- 角色授权表误清空后的恢复脚本（可重复执行）
-- 背景：2026-10-07 23:3x 端到端测试误调 DELETE/POST 授权接口，导致 pd_auth_role_authority 被清空(0 行)
-- 恢复策略：20:19 备份(144 行，含各角色 MENU/RESOURCE) + 平台管理员 role100 重新授予全部资源
USE pd_auth;

-- 0) 留档当前状态
DROP TABLE IF EXISTS pd_auth_role_authority_bak_20261007_2340;
CREATE TABLE pd_auth_role_authority_bak_20261007_2340 AS SELECT * FROM pd_auth_role_authority;

-- 1) 从 20:19 备份恢复（全角色 MENU + 原始 RESOURCE）
INSERT INTO pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT b.id, b.authority_id, b.authority_type, b.role_id, b.create_time, b.create_user
FROM pd_auth_role_authority_bak_20261007_201923 b
LEFT JOIN pd_auth_role_authority a ON a.id = b.id
WHERE a.id IS NULL;

-- 2) 平台管理员 role100：清掉旧 RESOURCE 授权后授予全部资源（383 条）
DELETE FROM pd_auth_role_authority WHERE role_id = '100' AND authority_type = 'RESOURCE';

SET @i := 0;
INSERT INTO pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
SELECT 780000000000000000 + (@i := @i + 1), r.id, 'RESOURCE', '100', NOW(), 3
FROM pd_auth_resource r, (SELECT @i := 0) t;

-- 3) 校验
SELECT '资源总数' AS item, COUNT(*) AS val FROM pd_auth_resource
UNION ALL SELECT '授权表总行数', COUNT(*) FROM pd_auth_role_authority
UNION ALL SELECT 'role100 RESOURCE', COUNT(*) FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
UNION ALL SELECT 'role100 MENU', COUNT(*) FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='MENU'
UNION ALL SELECT '其它角色授权', COUNT(*) FROM pd_auth_role_authority WHERE role_id <> '100';
SELECT authority_type, COUNT(*) c FROM pd_auth_role_authority GROUP BY authority_type;
