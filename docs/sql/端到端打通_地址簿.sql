-- ============================================================================
-- 七段链路端到端：下单所需的两个地址簿（归属客户 customer01=9203）
--
-- 下单入参 MailingSaveDTO.sendAddress / receiptAddress 是地址簿id，
-- MailingController.buildOrderAndPrice 经 addressBookFeign.detail 取出收发信息。
--
-- 口径（2026-10-08 pd_auth.pd_area 实测）：
--   广东省=19  深圳市=1000198 罗湖区=2001852（发件）
--              广州市=1000196 天河区=2001834（收件）
--
-- 幂等：先按 id 删除（带 WHERE）再插入，可重复执行。
-- ============================================================================

DELETE FROM `pd_users`.`pd_address_book`
 WHERE `id` IN ('addr_sz_sender_9203','addr_gz_receiver_9203');

INSERT INTO `pd_users`.`pd_address_book`
 (`id`,`user_id`,`name`,`phone_number`,`province_id`,`city_id`,`county_id`,`address`,`is_default`,`create_time`)
 VALUES
 ('addr_sz_sender_9203','9203','张三','13800000001',19,1000198,2001852,'罗湖区宝安南路1001号',1,NOW()),
 ('addr_gz_receiver_9203','9203','李四','13800000002',19,1000196,2001834,'天河区体育西路1002号',0,NOW());

-- 自检：期望返回 2 行，user_id 均为 9203
SELECT `id`,`user_id`,`province_id`,`city_id`,`county_id`,`address`
  FROM `pd_users`.`pd_address_book`
 WHERE `id` IN ('addr_sz_sender_9203','addr_gz_receiver_9203');
