-- ============================================================================
-- 阻断 C 解除包：pd_auth.pd_auth_resource 接口注册 SQL 模板
-- ============================================================================
-- 用途：把三端（driver/courier/customer）业务接口 + 告警等新增接口注册进权限资源表，
--       并绑定到角色，使网关 AccessFilter 放行（否则判"未知请求" UNAUTHORIZED）。
-- 生成日期：2026-10-06
-- 依据：pd_auth.pd_auth_resource 真实表结构（MySQL 5.7.44 服务端实测，客户端为 8.0）+ AccessFilter 源码
--       + auth-server GET /resource/list 实测返回 + Nacos pd-gateway-test.yml 13 条路由
--
-- ⚠️ 本文件仅提供 SQL，**未执行任何写库操作**。执行前务必先跑 §0 备份。
--
-- ✅ 已实测（2026-10-06，事务内执行后 ROLLBACK，库未变更）：
--    · 资源 INSERT 5 条 → 成功，回滚后 0 条
--    · 角色授权 INSERT（用户变量写法）→ 成功，回滚后 0 条
--    · code 唯一索引生效（重复 code 报 1062 Duplicate entry）
-- ⚠️ 服务端实测版本 = **MySQL 5.7.44**（不是 8.0），
--    故**不支持 ROW_NUMBER() 窗口函数**，§5 授权语句已改用 5.7 兼容的「用户变量」写法。
--
-- 🚨 执行纪律（血的教训，实测踩过）：
--    1. **必须整段包在 START TRANSACTION ... ROLLBACK 里先试跑**，
--       确认 SELECT 计数正确后再 COMMIT。
--    2. ⚠️ 千万不要用"批量抽取未注释行再拼接"的方式做语法校验——
--       极易把 COMMIT/ROLLBACK 截断在中间，导致**测试数据真的留在生产库**。
--       本次验证曾因此留下 5 条脏数据（48→53），已清理恢复（现为 48/102/0）。
--       语法校验请**手工复制小段**执行，或整段连同 ROLLBACK 一起复制。
--    3. 校验完务必回查：SELECT COUNT(*) FROM pd_auth_resource;  -- 应为 48
-- ============================================================================

-- ============================================================================
-- §0 执行前必做：备份（MyISAM 表必须锁表或停写）
-- ============================================================================
-- 方式一：mysqldump 备份三张表（推荐）
--   mysqldump -h192.168.20.130 -P3306 -uroot -p pd_auth \
--     pd_auth_resource pd_auth_role_authority pd_auth_menu > backup_pdauth_20261006.sql
--
-- 方式二：库内建备份表（引擎是 InnoDB，可在线操作）
-- CREATE TABLE pd_auth_resource_bak_20261006 AS SELECT * FROM pd_auth_resource;
-- CREATE TABLE pd_auth_role_authority_bak_20261006 AS SELECT * FROM pd_auth_role_authority;
-- SELECT COUNT(*) FROM pd_auth_resource;              -- 期望 48
-- SELECT COUNT(*) FROM pd_auth_role_authority;        -- 期望 102

-- 回滚语句（备份后误操作时执行）
-- DELETE r FROM pd_auth_resource r
--   LEFT JOIN pd_auth_resource_bak_20261006 b ON r.id = b.id WHERE b.id IS NULL;
-- INSERT INTO pd_auth_resource SELECT * FROM pd_auth_resource_bak_20261006;
-- INSERT INTO pd_auth_role_authority
--   SELECT * FROM pd_auth_role_authority_bak_20261006 ra
--   WHERE NOT EXISTS (SELECT 1 FROM pd_auth_role_authority x WHERE x.id = ra.id);


-- ============================================================================
-- §1 表结构事实（mysql 8.0 客户端连 5.7.44 服务端实测，务必遵守）
-- ============================================================================
-- pd_auth_resource:
--   id          bigint(20)      NOT NULL  PRIMARY KEY   ← ⚠️ 无 AUTO_INCREMENT，必须显式赋值
--   code        varchar(150)    DEFAULT ''  UNIQUE KEY UN_CODE  ← ⚠️ 全局唯一，重复会报错
--   name        varchar(150)    NOT NULL DEFAULT ''      ← 接口名称（中文）
--   menu_id     bigint(20)      DEFAULT NULL             ← 关联 pd_auth_menu.id
--   method      varchar(10)     DEFAULT NULL             ← GET/POST/PUT/DELETE
--   url         varchar(255)    DEFAULT NULL             ← ★服务内路径，见 §2 规则
--   describe_   varchar(255)    DEFAULT ''
--   create_user / create_time / update_user / update_time
--
-- 现有数据基准（2026-10-06 实测）：
--   48 条资源 / 102 条角色绑定（42 MENU + 60 RESOURCE）/ 最大 id = 684539815017848257
--   角色：100=PT_ADMIN 平台管理员、643779012732130273=BASE_USER 普通员工、
--         645198153556958497=DEPT_MANAGER 部门经理
--   ⚠️ 现有角色中**没有** driver/courier/customer 端角色，
--      故下方 §5 提供两种授权方案：① 暂挂平台管理员(联调用) ② 新建三端角色(正式用)
--
-- ID 生成规则：雪花 ID 单调递增。以下新 ID 从 684539815017848258 起顺延，
-- 与现有数据不冲突；执行前可再跑一次 SELECT MAX(id) 确认。


-- ============================================================================
-- §2 ★最关键：url 字段填什么（填错则永远鉴权失败）
-- ============================================================================
-- 网关 AccessFilter 源码（pd-gateway/.../filter/AccessFilter.java:63-67）：
--     requestURI = StrUtil.subSuf(requestURI, zuulPrefix.length()); // zuulPrefix="/api"
--     requestURI = StrUtil.subSuf(requestURI, requestURI.indexOf("/", 1));
--     String permission = method + requestURI;
-- hutool 5.1.0 StrUtil.subSuf(str, fromIndex) = str.substring(fromIndex)（从指定位置到结尾）
--
-- 即 permission = HTTP方法 + 剥掉("/api" + 网关服务名前缀) 后的路径，匹配方式是
-- **permission.startsWith(表中 method+url)**（前缀匹配，非精确匹配）。
--
-- 三个对照示例（已用真实 48 条数据验证一致）：
--   请求 GET /api/web-driver/user/profile  →  permission = "GET/user/profile"
--   请求 GET /api/authority/resource/page  →  permission = "GET/resource/page"  ✅表中已有
--   请求 POST/api/user                     →  permission = "POST/user"           ✅表中已有
--
-- ✅ 正确填法：url = **服务内路径**，不含 "/api"、不含 "web-driver" 等网关段
--      driver   /api/web-driver/user/profile      →  url = '/user/profile'
--      courier  /api/web-courier/courier/xxx/page  →  url = '/courier/xxx/page'
--      customer /api/web-customer/mailing/page     →  url = '/mailing/page'
--
-- ❌ 常见错误（会导致 404 或永远 401）：
--      url = '/api/web-driver/user/profile'   （含 /api 和网关段）
--      url = '/web-driver/user/profile'      （含网关段）
--
-- ⚠️ 路径重复陷阱（已实测确认，非推测）：
--      '/user/profile' 同时出现在 pd-web-driver 与 pd-web-customer。
--      因匹配是 startsWith 前缀匹配且**不带服务名**，两条业务会共用同一资源记录。
--      本模板按 driver 优先注册（code 用 driver:profile 区分），
--      customer 端复用同一条记录即可，不必重复插入。
--      若后续需要分别管控，再改为带服务名前缀的登记方案（需同步改 AccessFilter，勿轻改）。


-- ============================================================================
-- §3 注册前自检：确认这些接口确实还没注册（防重复插入）
-- ============================================================================
-- SELECT method, url, code FROM pd_auth.pd_auth_resource
--  WHERE url IN ('/user/profile','/business/cargo/wait','/courier/pickupDispatchTask/page',
--                 '/mailing/page','/alarm/page');
-- 期望：**返回 0 行**。若已有行说明别人已注册，跳过对应 INSERT。


-- ============================================================================
-- §4 插入资源（示例：先插 5 条核心接口，验证通过再全量插）
-- ============================================================================
-- 用 START TRANSACTION 包裹，出错 ROLLBACK
USE pd_auth;   -- ⚠️ 必须先选库（实测：不加会报 ERROR 1046 No database selected）
START TRANSACTION;

-- 司机端：用户资料
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (684539815017848258, 'driver:profile', '司机-个人资料', NULL, 'GET', '/user/profile',
   '司机端获取个人资料（mobile-x 端共用）', '3', NOW(), '3', NOW());

-- 司机端：运输任务-待接
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (684539815017848259, 'driver:cargo:wait', '司机-待接运输任务', NULL, 'GET', '/business/cargo/wait',
   '司机端待接运输任务列表', '3', NOW(), '3', NOW());

-- 快递员端：取派件任务分页
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (684539815017848260, 'courier:pickupDispatch:page', '快递员-取派件任务分页', NULL, 'POST',
   '/courier/pickupDispatchTask/page', '快递员端取派件任务分页查询', '3', NOW(), '3', NOW());

-- 客户端：寄件下单分页
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (684539815017848261, 'customer:mailing:page', '客户-寄件订单分页', NULL, 'POST', '/mailing/page',
   '客户端寄件订单分页查询', '3', NOW(), '3', NOW());

-- 告警中心（OPS-0 已定案：pd-web-manager 新建 AlarmController 暴露）
INSERT INTO pd_auth.pd_auth_resource
  (id, code, name, menu_id, method, url, describe_, create_user, create_time, update_user, update_time)
VALUES
  (684539815017848262, 'manager:alarm:page', '告警中心-分页', NULL, 'POST', '/alarm/page',
   '管理端告警分页（pd-web-manager 聚合暴露）', '3', NOW(), '3', NOW());

-- 校验：确认 5 条都进来了
-- SELECT COUNT(*) FROM pd_auth.pd_auth_resource WHERE id BETWEEN 684539815017848258
--   AND 684539815017848262;   -- 期望 5
COMMIT;   -- 确认无误后提交；若报错则 ROLLBACK


-- ============================================================================
-- §5 绑定到角色（两种方案，按需选一种）
-- ============================================================================
-- ⚠️ 现状：pd_auth_role 中只有 平台管理员/普通员工/部门经理，**无三端角色**。
--        且网关注入的 userid 来自 JWT，三端账号需先在 pd_auth_user 中存在。
-- ⚠️ 执行前务必先 USE pd_auth;（否则报 ERROR 1046 No database selected）

-- 方案 A（联调临时用，1 分钟搞定）：全部挂平台管理员（role_id=100）
-- ✅ 已实测可跑（MySQL 5.7 用户变量写法）
-- SET @i = 690609420538744417;
-- INSERT INTO pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
-- SELECT (@i := @i + 1), r.id, 'RESOURCE', 100, NOW(), '3'
--   FROM pd_auth_resource r
--  WHERE r.id >= 684539815017848258;   -- ⚠️ 用 >= 而非区间，避免漏插后续批次
--
-- ⚠️ 若用 MySQL 8.0 也可写：690609420538744417 + ROW_NUMBER() OVER (ORDER BY r.id) - 1
--    但本环境是 5.7.44，**用 ROW_NUMBER 会报语法错**，请勿混用。

-- 方案 B（正式用，建议）：新建三端角色并分别授权
-- INSERT INTO pd_auth_role (id, code, name, describe_, create_user, create_time, update_user, update_time)
-- VALUES (690609420538744420, 'DRIVER',   '司机端',   '司机 App 用户',   '3', NOW(), '3', NOW()),
--        (690609420538744421, 'COURIER',  '快递员端', '快递员 App 用户', '3', NOW(), '3', NOW()),
--        (690609420538744422, 'CUSTOMER', '客户端',   '客户小程序用户',  '3', NOW(), '3', NOW());
--
-- -- 授权：司机端角色给司机接口，依次类推
-- SET @j = 690609420538744499;
-- INSERT INTO pd_auth_role_authority (id, authority_id, authority_type, role_id, create_time, create_user)
-- SELECT (@j := @j + 1), r.id, 'RESOURCE', 690609420538744420, NOW(), '3'
--   FROM pd_auth_resource r
--  WHERE r.code LIKE 'driver:%';


-- ============================================================================
-- §6 全量注册清单（待建接口，接入时逐批执行；⚠️ 以《后端接口文档.md》§4/§5/§6 为准）
-- ============================================================================
-- 司机端 pd-web-driver（网关 /api/web-driver/**，注册 url 用服务内路径）
--   GET  /user/profile                  → code driver:profile
--   GET  /business/cargo/wait           → code driver:cargo:wait
--   GET  /business/cargo/history        → code driver:cargo:history
--   GET  /business/cargo/onTheWay       → code driver:cargo:onTheWay
--   GET  /business/cargo/detail         → code driver:cargo:detail
--   GET  /business/cargo/orders         → code driver:cargo:orders
--   PUT  /business/cargo/pickUp         → code driver:cargo:pickUp
--   PUT  /business/cargo/finish         → code driver:cargo:finish
--   GET  /business/car/info             → code driver:car:info
--   POST /attachment/upload             → code driver:attachment:upload
--
-- 快递员端 pd-web-courier（网关 /api/web-courier/**）
--   GET/POST/PUT ... /courier/**、/pickupDispatchTask/**、/transportOrder/** 等
--   ⚠️ 详见《后端接口文档.md》§5（13 个接口）；pd-web-courier **无附件 Controller**，
--      POD 上传须走 customer/driver 的 /attachment/upload。
--
-- 客户端 pd-web-customer（网关 /api/web-customer/**）
--   /mailing/**、/address/**、/user/profile、/order/** 等 19 个接口
--   ⚠️ /user/profile 与 driver 同名（见 §2 陷阱说明），复用同一条资源记录。
--
-- 管理端新增（本次开发后注册）
--   /alarm/page、/alarm/{id}、/alarm/handle、/alarm/count   → code manager:alarm:*
--
-- 💡 批量技巧：所有业务接口的 `method + '/' + 类级RequestMapping + '/' + 方法路径` 即为
--    url。最省事的做法是让后端同学用 @RequestMapping 注解值导出清单，再套进本模板。


-- ============================================================================
-- §7 验证步骤（按顺序做，缺一步会出现"以为成功其实没生效"）
-- ============================================================================
-- 步骤 1：查 auth-server 实时清单（服务直连 9000，绕过网关）
--   curl -s "http://192.168.20.130:9000/resource/list" | head -c 600
--   期望：返回的数组里出现 "GET/user/profile"、"POST/mailing/page" 等新增标识。
--   ⚠️ 若没有，可能是 auth-server 有缓存，需重启 pd-auth-server。
--
-- 步骤 2：★必须清网关缓存，否则改了库也不生效
--   AccessFilter 用 j2cache 缓存 RESOURCE_NEED_TO_CHECK（缓存 key=该常量名）。
--   两种方式（二选一）：
--     a) 登录 Nacos 控制台 → 缓存管理 → 清理对应 key
--     b) 重启 pd-gateway（最简单可靠）：
--        docker restart <网关容器名>
--   ⚠️ 不清缓存的话，网关仍按旧清单判"未知请求"，会误以为 SQL 没生效。
--
-- 步骤 3：带 token 调一个刚注册的三端接口
--   curl -s -H "token: <上一步登录拿到的 JWT>" \
--        "http://192.168.20.130:8760/api/web-driver/user/profile"
--   期望：返回 code=0 的业务数据；**不是** pre:AccessFilter 的 500。
--
-- 步骤 4：验证未注册的接口仍被拒（反向验证过滤器真的在工作）
--   curl -s -o /dev/null -w "%{http_code}" \
--        "http://192.168.20.130:8760/api/web-driver/thisPathNeverRegistered"
--   期望：500 且 message 为 "pre:AccessFilter"（未注册=被拦）。
--
-- ⚠️ 排查口径：
--   返回 404            → 路由/服务名拼错，或服务未注册到 Nacos
--   500 pre:AccessFilter 且步骤 1 能查到新记录 → 网关缓存未清（回到步骤 2）
--   500 pre:AccessFilter 且步骤 1 查不到       → SQL 未执行/未提交/连错库
--   "用户没有访问xxx资源的权限"（log.warn）     → 资源已注册但**未绑定角色**（见 §5）


-- ============================================================================
-- §8 一键执行版（熟悉后可折叠 §0-§6 单独使用）
-- ============================================================================
-- ⚠️ 本脚本会真实写库，执行前请确认已备份且已获得授权。
-- 建议先跑 §4 的 5 条样本验证通路，再跑全量。

-- BEGIN;
-- USE pd_auth;   -- ⚠️ 必须先选库，否则 ERROR 1046
-- -- 0) 备份
-- CREATE TABLE pd_auth_resource_bak_20261006 AS SELECT * FROM pd_auth_resource;
-- CREATE TABLE pd_auth_role_authority_bak_20261006 AS SELECT * FROM pd_auth_role_authority;
--
-- -- 1) 资源注册（此处省略全量 INSERT，见 §6 清单）
-- -- INSERT INTO pd_auth_resource (id,code,name,menu_id,method,url,describe_,
-- --                               create_user,create_time,update_user,update_time)
-- -- VALUES (...), (...);
--
-- -- 2) 角色授权（⚠️ MySQL 5.7 兼容写法，勿用 ROW_NUMBER）
-- SET @i = 690609420538744417;
-- INSERT INTO pd_auth_role_authority (id,authority_id,authority_type,role_id,create_time,create_user)
-- SELECT (@i := @i + 1), r.id, 'RESOURCE', 100, NOW(), '3'
--   FROM pd_auth_resource r WHERE r.id >= 684539815017848258;
--
-- -- 3) 校验
-- SELECT COUNT(*) AS 新增资源数 FROM pd_auth_resource WHERE id >= 684539815017848258;
-- SELECT COUNT(*) AS 新增授权数 FROM pd_auth_role_authority WHERE role_id = 100
--   AND authority_type = 'RESOURCE' AND authority_id >= 684539815017848258;
-- -- COMMIT;  -- 校验数对再提交，否则 ROLLBACK;

-- ============================================================================
-- 结束。相关文档：《后端接口文档.md》§8.2 / 《联调就绪检查清单.md》§2 / 《全量需求自检报告.md》L-3
-- ============================================================================
