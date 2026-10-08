-- ======================================================================
-- 资源注册路径修正：把"路径模板字面串"归一化为"父路径"
-- ----------------------------------------------------------------------
-- 根因:
--   pd_auth_resource 中大量资源把 URI 模板当字面串注册，例如:
--     PUT /courier/detail/{id}
--     GET /order-manager/order/{id}
--     PUT /mailing/{id}
--   网关 AccessFilter 对真实请求 (如 PUT /courier/detail/123) 做匹配时,
--   无论旧的 startsWith 还是修复后的边界匹配, 都无法命中含 "{id}" 的字面串,
--   导致所有路径变量接口 count==0, 直接判 401。
--
-- 修复思路:
--   为每个路径模板资源补登一条"父路径"资源 (截到第一个 /{ 段之前), 例如:
--     PUT /courier/detail/{id}      -> PUT /courier/detail
--     GET /order-manager/order/{id} -> GET /order-manager/order
--     PUT /mailing/{id}             -> PUT /mailing
--   配合网关 AccessFilter 的边界匹配 equals || startsWith(parent + "/"),
--   真实 URI 即可命中; 同时不会产生 /ord 误放行 /order 的前缀碰撞。
--
-- 归一化安全规则 (防止父路径过度放宽):
--   仅当第一个 {变量} 段之后"没有字面量后缀"时才自动归一化, 即:
--     /a/detail/{id}            -> 安全, 父路径 /a/detail
--     /a/delivered/{x}/{y}      -> 安全, 父路径 /a/delivered
--     /agency/{id}/scope        -> 跳过: 父路径 /agency 会把 /agency/user/... 一并放行
--     /base/truck/{id}/license  -> 跳过: 父路径 /base/truck 会过度放宽
--   被跳过的记录会在文末"人工复核清单"中列出。这些端点多数已被同 method 的
--   集合根父路径覆盖 (如 POST /transfor-center/bussiness/trips 已覆盖
--   .../trips/{id}/truckDriver), 未覆盖的需逐个评估后单独注册, 切勿一刀切。
--
-- 幂等性:
--   只 INSERT, 不 DROP / DELETE / TRUNCATE; 同一 (url+method) 已存在则跳过,
--   脚本可重复执行, 第二次执行预期 0 行变更。兼容 MySQL 5.7 (无窗口函数)。
--
-- ----------------------------------------------------------------------
-- 【执行前置条件, 必须逐条确认】
--   1. 必须先执行 (顺序不可颠倒):
--        a) docs/sql/管理端接口资源注册_20261007.sql
--        b) docs/sql/阻断C_三端接口注册与账号闭环.sql
--      本脚本从上述脚本注册的模板资源派生父路径并复制其授权。
--   2. 执行前, pd-web 各聚合接口的"资源归属(数据权限)校验"必须已上线。
--      父路径注册会把同一前缀下的写接口统一纳入鉴权放行集合, 若归属校验
--      未上线, 将直接暴露"水平越权写"(操作他人/他网点订单)的风险。
--   3. 本脚本不触碰任何应用配置, 不删除旧的模板字面串资源 (它们不再被
--      命中, 作为审计记录保留)。
--
-- 【执行后动作, 必须执行】
--   必须重启【所有】pd-gateway 网关节点。原因: AccessFilter 的资源清单
--   (RESOURCE_NEED_TO_CHECK) 走 j2cache L1 = Caffeine 进程内缓存,
--   不重启不会重新加载, 新注册的父路径不会生效。
-- ======================================================================

USE pd_auth;

-- ----------------------------------------------------------------------
-- 步骤 1: 补登父路径资源
--   id 段使用固定号段 7001007000000xxxxxx (Snowflake 号段之外, 人工可识别),
--   先在派生表内按 (父路径 + method) 去重, 再用 NOT EXISTS 跳过已存在记录。
-- ----------------------------------------------------------------------
INSERT INTO pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
SELECT
  7001007000000000000 + x.rn,
  CONCAT('pathfix:', x.method, ':', x.parent_url),
  '路径归一化父路径',
  NULL,
  x.method,
  x.parent_url,
  CONCAT('由路径变量模板资源归一化, 源资源id: ', x.src_ids),
  3, NOW(), 3, NOW()
FROM (
  SELECT
    @rn := @rn + 1 AS rn,
    g.parent_url,
    g.method,
    g.src_ids
  FROM (
    SELECT
      c.parent_url,
      c.method,
      GROUP_CONCAT(DISTINCT c.src_id ORDER BY c.src_id) AS src_ids
    FROM (
      SELECT
        r.id AS src_id,
        r.method,
        -- 截到第一个 "/{" 之前, 得到父路径
        SUBSTRING(r.url, 1, LOCATE('/{', r.url) - 1) AS parent_url,
        -- 第一个变量段右花括号之后的剩余部分, 用于判断是否存在字面量后缀
        SUBSTRING(r.url, LOCATE('}', r.url, LOCATE('/{', r.url)) + 1) AS tail
      FROM pd_auth_resource r
      WHERE r.url LIKE '%{%'
    ) c
    WHERE c.tail = ''
       -- 剩余部分只能是一个或多个 "/{变量}" 段, 不允许出现 "/scope" 这类字面量段
       OR c.tail REGEXP '^/\\{[A-Za-z0-9_]+\\}(/\\{[A-Za-z0-9_]+\\})*$'
    GROUP BY c.parent_url, c.method
  ) g
  CROSS JOIN (SELECT @rn := 0) init_var
) x
WHERE NOT EXISTS (
  SELECT 1
  FROM pd_auth_resource e
  WHERE e.url = x.parent_url
    AND e.method = x.method
);

-- ----------------------------------------------------------------------
-- 步骤 2: 复制授权
--   把每个模板资源原有角色授权, 复制给对应的父路径资源。
--   这样平台管理员 (role_id=100 PT_ADMIN) 以及司机端/客户端各角色都能
--   自动获得归一化父路径的授权, 无需逐角色手工补。
--   幂等: NOT EXISTS 跳过已存在的 (role_id + 父路径资源) 授权。
-- ----------------------------------------------------------------------
INSERT INTO pd_auth_role_authority
  (id, role_id, authority_id, authority_type)
SELECT
  7001007000100000000 + @gid,
  s.role_id,
  s.new_res_id,
  'RESOURCE'
FROM (
  SELECT DISTINCT
    nr.id   AS new_res_id,
    ra.role_id
  FROM (
    SELECT
      r.id AS src_id,
      r.method,
      SUBSTRING(r.url, 1, LOCATE('/{', r.url) - 1) AS parent_url,
      SUBSTRING(r.url, LOCATE('}', r.url, LOCATE('/{', r.url)) + 1) AS tail
    FROM pd_auth_resource r
    WHERE r.url LIKE '%{%'
  ) c
  JOIN pd_auth_resource nr
    ON nr.url = c.parent_url
   AND nr.method = c.method
  JOIN pd_auth_role_authority ra
    ON ra.authority_type = 'RESOURCE'
   AND ra.authority_id = c.src_id
  WHERE c.tail = ''
     OR c.tail REGEXP '^/\\{[A-Za-z0-9_]+\\}(/\\{[A-Za-z0-9_]+\\})*$'
) s
CROSS JOIN (SELECT @gid := 0) init_gid
WHERE NOT EXISTS (
  SELECT 1
  FROM pd_auth_role_authority e
  WHERE e.role_id = s.role_id
    AND e.authority_id = s.new_res_id
    AND e.authority_type = 'RESOURCE'
);

-- ----------------------------------------------------------------------
-- 验证 / 人工复核清单 (只读 SELECT)
-- ----------------------------------------------------------------------

-- 1) 本次新增的父路径资源数量 (重复执行为 0 行新增; 此处统计在册的归一化资源总数)
SELECT COUNT(*) AS 归一化父路径资源数
FROM pd_auth_resource
WHERE id BETWEEN 7001007000000000001 AND 7001007000000999999;

-- 2) 平台管理员 role_id=100 对归一化资源的授权数
SELECT COUNT(*) AS role100_归一化资源授权数
FROM pd_auth_role_authority ra
JOIN pd_auth_resource r ON r.id = ra.authority_id
WHERE ra.role_id = '100'
  AND ra.authority_type = 'RESOURCE'
  AND r.id BETWEEN 7001007000000000001 AND 7001007000000999999;

-- 3) 人工复核清单: 第一个变量段后仍含字面量后缀、被自动跳过的模板资源。
--    先确认是否已被同 method 的集合根父路径覆盖; 未覆盖的逐个单独注册,
--    禁止为图省事直接注册更上层父路径。
SELECT
  r.method,
  r.url,
  SUBSTRING(r.url, 1, LOCATE('/{', r.url) - 1) AS 待评估父路径,
  SUBSTRING(r.url, LOCATE('}', r.url, LOCATE('/{', r.url)) + 1) AS 字面量后缀
FROM pd_auth_resource r
WHERE r.url LIKE '%{%'
  AND SUBSTRING(r.url, LOCATE('}', r.url, LOCATE('/{', r.url)) + 1) <> ''
  AND SUBSTRING(r.url, LOCATE('}', r.url, LOCATE('/{', r.url)) + 1)
      NOT REGEXP '^/\\{[A-Za-z0-9_]+\\}(/\\{[A-Za-z0-9_]+\\})*$';

-- ----------------------------------------------------------------------
-- 执行完成后: 重启所有 pd-gateway 节点, 然后用真实 (非 {id} 字面) URI
-- 验证路径变量接口不再 401。
-- ----------------------------------------------------------------------
