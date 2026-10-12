-- =====================================================================
-- 脏数据清理建议脚本（dev / pinda_tms）  日期：2026-10-12
-- ---------------------------------------------------------------------
-- ⚠️ 本脚本仅为「建议」，由人工核对后本人执行，AI 不自动运行。
-- 原则：
--   1) 一律先软删（UPDATE deleted=1），可逆；仅 pd_payment_order / oms_freight_detail
--      无 deleted 列，才使用「带主键 + 业务键双重 WHERE」的物理 DELETE。
--   2) 每条语句都带精确 WHERE，无全表删除、无 TRUNCATE。
--   3) 清理顺序：先下游子表，再主订单；货型务必等引用它的货物软删后再删。
--   4) 执行前可先跑文末「核对 SELECT」，确认行数与预期一致。
-- 背景：这些均为早期联调/取消验证残留（order_no 为 NULL、PJ 自寄单号、
--      测试客户 9301-9303、无车次无车且终到机构 101/102 不存在的运输任务）。
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- A. 孤立残缺运输任务 9601/9602（trips_id/truck_id 均 NULL，end_org 101/102 不存在；
--    已核实无 work_transport_order_task / driver_job / history 引用，可安全软删）
-- ---------------------------------------------------------------------
UPDATE `work_task_transport` SET deleted=1, update_time=NOW()
WHERE id IN (9601,9602) AND deleted=0;

-- ---------------------------------------------------------------------
-- B. PJ 自寄测试单 9401/9402/9403（PJ0001-PJ0003，会员为测试客户01-03）整链
--    顺序：运费/支付(物理) -> 结算 -> 取派 -> 运单 -> 订单
-- ---------------------------------------------------------------------
-- 运费明细（无 deleted 列，物理删；主键+订单双条件）
DELETE FROM `oms_freight_detail` WHERE id=8846500302848 AND order_id=9401;
-- 支付单（无 deleted 列，物理删；id 为 varchar）
DELETE FROM `pd_payment_order` WHERE id='14351771766785' AND order_id=9401;
-- 结算单
UPDATE `oms_settlement_order` SET deleted=1, update_time=NOW()
WHERE id=8846559023104 AND order_id=9401 AND deleted=0;
-- 取派任务（2 条，均挂 9401）
UPDATE `work_pickup_dispatch_task` SET deleted=1, update_time=NOW()
WHERE id IN (589182406656,589773803520) AND order_id=9401 AND deleted=0;
-- 运单 9501/9502/9503（已核实未挂任何运输任务）
UPDATE `work_transport_order` SET deleted=1, update_time=NOW()
WHERE id IN (9501,9502,9503) AND order_id IN (9401,9402,9403) AND deleted=0;
-- 主订单
UPDATE `oms_order` SET deleted=1, update_time=NOW()
WHERE id IN (9401,9402,9403) AND deleted=0;

-- ---------------------------------------------------------------------
-- C. 端到端联调/取消验证测试单 3 张（order_no 全为 NULL，会员测试客户01=9301）
--    585009074176(已取消) / 2193105227776(入库) / 3126321090560(已取消)
-- ---------------------------------------------------------------------
-- 货物 6 条（引用脏货型，必须先于货型软删）
UPDATE `oms_order_cargo` SET deleted=1, update_time=NOW()
WHERE id IN (585415921664,586749710336,2193658875904,2194447405056,3126438531072,3126887321600)
  AND order_id IN (585009074176,2193105227776,3126321090560) AND deleted=0;
-- 位置（其余 2 条已 deleted=1，仅这条仍有效）
UPDATE `oms_order_location` SET deleted=1, update_time=NOW()
WHERE id=2194225106944 AND order_id=2193105227776 AND deleted=0;
-- 取派任务 3 条
UPDATE `work_pickup_dispatch_task` SET deleted=1, update_time=NOW()
WHERE id IN (2007108816896,2938869256192,404758859776)
  AND order_id IN (585009074176,2193105227776,3126321090560) AND deleted=0;
-- 运单 3 条（已核实未挂运输任务）
UPDATE `work_transport_order` SET deleted=1, update_time=NOW()
WHERE id IN (1989643735040,2921727135744,384747835392)
  AND order_id IN (585009074176,2193105227776,3126321090560) AND deleted=0;
-- 支付单 3 条（无 deleted 列，物理删；id 与订单双重约束防误删）
DELETE FROM `pd_payment_order`
WHERE id IN ('1571236745217','2562732462081','3151797293057')
  AND order_id IN (585009074176,2193105227776,3126321090560);
-- 主订单 3 张
UPDATE `oms_order` SET deleted=1, update_time=NOW()
WHERE id IN (585009074176,2193105227776,3126321090560) AND deleted=0;

-- ---------------------------------------------------------------------
-- D. 字典孤立测试项（确认上游测试单已软删后再执行）
-- ---------------------------------------------------------------------
-- 脏货型 5 个：其中 3741126365184 / 3746763509760 被上面 6 条联调货物引用，
-- 须在 C 段货物软删之后执行；truck_type_goods 关联数=0。
UPDATE `base_goods_type` SET deleted=1, update_time=NOW()
WHERE id IN (3741126365184,3746763509760,471762866176,196247558426624,197257710407680) AND deleted=0;
-- 脏车型 2 个：已核实 base_truck / truck_type_goods 零引用
UPDATE `base_truck_type` SET deleted=1, update_time=NOW()
WHERE id IN (194480439431168,196240780431360) AND deleted=0;

-- ---------------------------------------------------------------------
-- E.（可选，确认无其他用途再跑）测试会员 测试客户01/02/03
-- ---------------------------------------------------------------------
-- UPDATE `member_info` SET deleted=1, update_time=NOW()
-- WHERE id IN (9301,9302,9303) AND deleted=0;

-- =====================================================================
-- 不要删（经核实为有效/已处理数据）：
--   * base_org_scope id 1/2 —— 引用的机构 1001「武汉江岸网点」、1002「武汉武昌网点」真实存在，不悬空。
--   * base_transport_line_type 196251777896448（测试线型Z2）、base_fleet 196232152748032（测试车队A-改）、
--     oms_order 3219052957696 —— 均已 deleted=1，无需再动。
--
-- 执行前核对（预期受影响行数）：
--   SELECT id,order_id FROM oms_order WHERE id IN (9401,9402,9403,585009074176,2193105227776,3126321090560) AND deleted=0;            -- 6
--   SELECT id FROM work_transport_order WHERE id IN (9501,9502,9503,1989643735040,2921727135744,384747835392) AND deleted=0;          -- 6
--   SELECT id FROM work_pickup_dispatch_task WHERE id IN (589182406656,589773803520,2007108816896,2938869256192,404758859776) AND deleted=0; -- 5
--   SELECT id FROM oms_order_cargo WHERE id IN (585415921664,586749710336,2193658875904,2194447405056,3126438531072,3126887321600) AND deleted=0; -- 6
--   SELECT id FROM oms_order_location WHERE id=2194225106944 AND deleted=0;                                                          -- 1
--   SELECT id FROM oms_freight_detail WHERE id=8846500302848;                                                                        -- 1
--   SELECT id FROM pd_payment_order WHERE id IN ('14351771766785','1571236745217','2562732462081','3151797293057');                  -- 4
--   SELECT id FROM oms_settlement_order WHERE id=8846559023104 AND deleted=0;                                                        -- 1
--   SELECT id FROM work_task_transport WHERE id IN (9601,9602) AND deleted=0;                                                        -- 2
--   SELECT id FROM base_goods_type WHERE id IN (3741126365184,3746763509760,471762866176,196247558426624,197257710407680) AND deleted=0; -- 5
--   SELECT id FROM base_truck_type WHERE id IN (194480439431168,196240780431360) AND deleted=0;                                      -- 2
-- =====================================================================
