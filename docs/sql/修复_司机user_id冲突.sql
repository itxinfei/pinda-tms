-- ============================================================
-- 修复：pd_truck_driver.user_id 重复导致 /sys/driver/{userId} 查 2 条报 TooManyResultsException
-- 根因：阻断C脚本误将 5 条 seed 司机(原本 user_id=10001~10005) 改写为 9101~9105，
--       与库内既有真实司机(id=9101/9102/9103, user_id=9101/9102/9103) 冲突。
-- 修复：① 还原 seed 司机 user_id 为 10001~10005（去除冲突，保留既有真实司机）；
--       ② 为 driver04/05(9104/9105) 各补 1 条司机档案，使 5 个司机账号均有 profile。
-- 库：pd_base  字符集：utf8mb4
-- 说明：id 为 varchar(20)，真实司机采用 id==user_id 形态；seed 司机为长整型 id。
-- ============================================================
USE pd_base;

-- 先回显修复前冲突计数（应 = 3：9101/9102/9103 各 2 条）
SELECT '修复前 user_id 重复项:' AS chk, user_id, COUNT(*) c
FROM pd_truck_driver GROUP BY user_id HAVING c > 1;

-- ① 还原 5 条 seed 司机 user_id（按 id 精确匹配，避免误伤真实司机）
UPDATE pd_truck_driver SET user_id = '10001' WHERE id = '2400000000000000001';
UPDATE pd_truck_driver SET user_id = '10002' WHERE id = '2400000000000000002';
UPDATE pd_truck_driver SET user_id = '10003' WHERE id = '2400000000000000003';
UPDATE pd_truck_driver SET user_id = '10004' WHERE id = '2400000000000000004';
UPDATE pd_truck_driver SET user_id = '10005' WHERE id = '2400000000000000005';

-- ② 为 driver04 / driver05 补司机档案（id 与 user_id 同形，沿用真实司机形态）
INSERT INTO pd_truck_driver (id, user_id, fleet_id, age, picture, driving_age)
VALUES ('9104', '9104', '2100000000000000002', 38, NULL, 10);
INSERT INTO pd_truck_driver (id, user_id, fleet_id, age, picture, driving_age)
VALUES ('9105', '9105', '2100000000000000003', 45, NULL, 12);

-- 修复后校验
SELECT '修复后 user_id 重复项(应为空):' AS chk, COUNT(*) AS dup
FROM (SELECT user_id FROM pd_truck_driver GROUP BY user_id HAVING COUNT(*) > 1) t;

SELECT '最终司机清单:' AS chk;
SELECT id, user_id, fleet_id, age FROM pd_truck_driver ORDER BY user_id;

-- 模拟业务查询：driver01/02/03/04/05 各自的 findOne 应各返回 1 条
SELECT 'findOne(9101) 命中:' AS chk, COUNT(*) FROM pd_truck_driver WHERE user_id='9101';
SELECT 'findOne(9102) 命中:' AS chk, COUNT(*) FROM pd_truck_driver WHERE user_id='9102';
SELECT 'findOne(9103) 命中:' AS chk, COUNT(*) FROM pd_truck_driver WHERE user_id='9103';
SELECT 'findOne(9104) 命中:' AS chk, COUNT(*) FROM pd_truck_driver WHERE user_id='9104';
SELECT 'findOne(9105) 命中:' AS chk, COUNT(*) FROM pd_truck_driver WHERE user_id='9105';
