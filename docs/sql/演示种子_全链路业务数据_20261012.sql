-- =====================================================================
-- 品达 TMS · 全链路演示业务数据种子（dev only）
-- 库：pinda_tms（单库）  租户：tenant_id = 1（品达物流）
-- 日期：2026-10-12
-- 说明：
--   1. 仅用于内网 dev 演示与管理端页面联调，禁止用于生产，不纳入 Flyway。
--   2. 可重复执行：每张表先按“本脚本固定 ID 段”做带 WHERE 的物理清理再灌入，
--      不会删除任何既有业务数据；对既有机构/账号只做“补字段”的精准 UPDATE。
--   3. 绝不动 admin(1000)/pinda(1001) 及既有账号的 account/password；
--      新增演示账号 demo01~demo10，密码均为 123456（复用现网 BCrypt 哈希）。
--   4. 状态码全部对齐枚举（OrderStatus 1~12 / TransportOrderStatus 1~6 /
--      TaskStatus / PickupDispatchTaskStatus / PaymentStatus 等），不硬编码业务外数字。
--   5. 脏数据不在本脚本处理，另见交付说明中的“脏数据清理建议清单”，由人工执行。
-- 执行：
--   mysql --default-character-set=utf8mb4 pinda_tms < 演示种子_全链路业务数据_20261012.sql
-- =====================================================================
SET NAMES utf8mb4;
SET @tenant_id := 1;
-- 123456 的 BCrypt 哈希（与现网 admin/pinda 完全相同，保证 demo 账号可登录）
SET @pwd := '$2a$10$kChsqIGdVmTJiCHDJv/Iau0RcdB76cW.b4gWga4Pf0QvGoGhvQKES';

-- =====================================================================
-- §1 组织树（参考顺丰/德邦“总公司 → 城市一级转运中心 → 片区二级中转场 → 区县营业部”三级网络）
--   既有：100 总公司；201~207 七个城市一级转运中心（仅补省市区/地址，不改坐标/类型）
--   新增：1101~1107 七个片区二级中转场；1201~1219 十九个区县营业部
-- =====================================================================

-- 1.1 总公司补注册信息（此前省市/地址/坐标为空，导致大屏地图无总部点）
UPDATE `sys_org`
SET province_id = 110000, city_id = 110100, county_id = 110115,
    address = '北京市大兴区黄徐路品达物流园',
    contract_number = '010-88000000',
    manager_id = 1000,
    longitude = 116.403000, latitude = 39.915000,
    business_hours = '00:00-24:00',
    update_time = NOW()
WHERE id = 100 AND tenant_id = @tenant_id;

-- 1.2 七个城市一级转运中心补行政区划与地址（坐标沿用既有，不覆盖）
UPDATE `sys_org` SET province_id=110000,city_id=110100,county_id=110115,address='北京市大兴区黄徐路品达北京转运中心',contract_number='010-88000001',manager_id=9001,business_hours='00:00-24:00',update_time=NOW() WHERE id=201 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=310000,city_id=310100,county_id=310118,address='上海市青浦区华新镇品达上海转运中心',contract_number='021-58000001',manager_id=9002,business_hours='00:00-24:00',update_time=NOW() WHERE id=202 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=440000,city_id=440100,county_id=440111,address='广州市白云区石井街道品达广州转运中心',contract_number='020-86000001',business_hours='00:00-24:00',update_time=NOW() WHERE id=203 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=510000,city_id=510100,county_id=510114,address='成都市新都区物流大道品达成都转运中心',contract_number='028-85000001',business_hours='00:00-24:00',update_time=NOW() WHERE id=204 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=420000,city_id=420100,county_id=420112,address='武汉市东西湖区吴家山品达武汉转运中心',contract_number='027-83000001',business_hours='00:00-24:00',update_time=NOW() WHERE id=205 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=610000,city_id=610100,county_id=610111,address='西安市灞桥区国际港务区品达西安转运中心',contract_number='029-83000001',business_hours='00:00-24:00',update_time=NOW() WHERE id=206 AND tenant_id=@tenant_id;
UPDATE `sys_org` SET province_id=210000,city_id=210100,county_id=210114,address='沈阳市于洪区沈北物流园品达沈阳转运中心',contract_number='024-86000001',business_hours='00:00-24:00',update_time=NOW() WHERE id=207 AND tenant_id=@tenant_id;

-- 1.3 清理本段（可重复执行）
DELETE FROM `sys_org` WHERE tenant_id=@tenant_id AND id BETWEEN 1101 AND 1219;

-- 1.4 片区二级中转场（org_type=3）
INSERT INTO `sys_org`
(`id`,`tenant_id`,`name`,`abbreviation`,`parent_id`,`org_type`,`province_id`,`city_id`,`county_id`,`address`,`contract_number`,`manager_id`,`tree_path`,`sort_value`,`status`,`longitude`,`latitude`,`business_hours`,`description`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(1101,@tenant_id,'北京南城中转场','北京南城',201,3,110000,110100,110115,'北京市大兴区黄村片区品达南城中转场','010-88000011',NULL,',100,201,',1,1,116.341000,39.727000,'00:00-24:00','北京枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1102,@tenant_id,'上海青浦中转场','上海青浦',202,3,310000,310100,310118,'上海市青浦区华新镇品达青浦中转场','021-58000011',NULL,',100,202,',2,1,121.223000,31.173000,'00:00-24:00','上海枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1103,@tenant_id,'广州白云中转场','广州白云',203,3,440000,440100,440111,'广州市白云区江高镇品达白云中转场','020-86000011',NULL,',100,203,',3,1,113.273000,23.217000,'00:00-24:00','广州枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1104,@tenant_id,'成都新都中转场','成都新都',204,3,510000,510100,510114,'成都市新都区三河场品达新都中转场','028-85000011',NULL,',100,204,',4,1,104.157000,30.824000,'00:00-24:00','成都枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1105,@tenant_id,'武汉东西湖中转场','武汉东西湖',205,3,420000,420100,420112,'武汉市东西湖区吴家山品达径河中转场','027-83000011',NULL,',100,205,',5,1,114.142000,30.621000,'00:00-24:00','武汉枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1106,@tenant_id,'西安灞桥中转场','西安灞桥',206,3,610000,610100,610111,'西安市灞桥区新筑街道品达灞桥中转场','029-83000011',NULL,',100,206,',6,1,109.063000,34.367000,'00:00-24:00','西安枢纽下辖片区中转场',1000,NOW(),NOW(),0),
(1107,@tenant_id,'沈阳于洪中转场','沈阳于洪',207,3,210000,210100,210114,'沈阳市于洪区造化街道品达于洪中转场','024-86000011',NULL,',100,207,',7,1,123.315000,41.827000,'00:00-24:00','沈阳枢纽下辖片区中转场',1000,NOW(),NOW(),0);

-- 1.5 十九个区县营业部（org_type=4；行政区划码均取自库内 dict_area 已核实存在的码）
INSERT INTO `sys_org`
(`id`,`tenant_id`,`name`,`abbreviation`,`parent_id`,`org_type`,`province_id`,`city_id`,`county_id`,`address`,`contract_number`,`manager_id`,`tree_path`,`sort_value`,`status`,`longitude`,`latitude`,`business_hours`,`description`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(1201,@tenant_id,'北京东城营业部','北京东城',1101,4,110000,110100,110101,'北京市东城区安定门内大街88号','010-88001201',NULL,',100,201,1101,',101,1,116.416000,39.929000,'08:00-20:00','北京东城末端营业部',1000,NOW(),NOW(),0),
(1202,@tenant_id,'北京朝阳营业部','北京朝阳',1101,4,110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城','010-88001202',NULL,',100,201,1101,',102,1,116.486000,39.921000,'08:00-20:00','北京朝阳末端营业部',1000,NOW(),NOW(),0),
(1203,@tenant_id,'北京海淀营业部','北京海淀',1101,4,110000,110100,110108,'北京市海淀区中关村大街27号','010-88001203',NULL,',100,201,1101,',103,1,116.298000,39.959000,'08:00-20:00','北京海淀末端营业部',1000,NOW(),NOW(),0),
(1204,@tenant_id,'上海黄浦营业部','上海黄浦',1102,4,310000,310100,310101,'上海市黄浦区南京东路228号','021-58001204',NULL,',100,202,1102,',104,1,121.484000,31.231000,'08:00-20:00','上海黄浦末端营业部',1000,NOW(),NOW(),0),
(1205,@tenant_id,'上海徐汇营业部','上海徐汇',1102,4,310000,310100,310104,'上海市徐汇区漕溪北路18号','021-58001205',NULL,',100,202,1102,',105,1,121.437000,31.188000,'08:00-20:00','上海徐汇末端营业部',1000,NOW(),NOW(),0),
(1206,@tenant_id,'上海浦东营业部','上海浦东',1102,4,310000,310100,310115,'上海市浦东新区世纪大道100号','021-58001206',NULL,',100,202,1102,',106,1,121.534000,31.225000,'08:00-20:00','上海浦东末端营业部',1000,NOW(),NOW(),0),
(1207,@tenant_id,'广州荔湾营业部','广州荔湾',1103,4,440000,440100,440103,'广州市荔湾区中山八路23号','020-86001207',NULL,',100,203,1103,',107,1,113.244000,23.126000,'08:00-20:00','广州荔湾末端营业部',1000,NOW(),NOW(),0),
(1208,@tenant_id,'广州天河营业部','广州天河',1103,4,440000,440100,440106,'广州市天河区天河路208号天河城','020-86001208',NULL,',100,203,1103,',108,1,113.361000,23.125000,'08:00-20:00','广州天河末端营业部',1000,NOW(),NOW(),0),
(1209,@tenant_id,'广州白云营业部','广州白云',1103,4,440000,440100,440111,'广州市白云区机场路云霄路88号','020-86001209',NULL,',100,203,1103,',109,1,113.259000,23.158000,'08:00-20:00','广州白云末端营业部',1000,NOW(),NOW(),0),
(1210,@tenant_id,'成都锦江营业部','成都锦江',1104,4,510000,510100,510104,'成都市锦江区春熙路南段8号','028-85001210',NULL,',100,204,1104,',110,1,104.080000,30.657000,'08:00-20:00','成都锦江末端营业部',1000,NOW(),NOW(),0),
(1211,@tenant_id,'成都武侯营业部','成都武侯',1104,4,510000,510100,510107,'成都市武侯区人民南路四段12号','028-85001211',NULL,',100,204,1104,',111,1,104.043000,30.642000,'08:00-20:00','成都武侯末端营业部',1000,NOW(),NOW(),0),
(1212,@tenant_id,'成都金牛营业部','成都金牛',1104,4,510000,510100,510108,'成都市金牛区沙湾路258号','028-85001212',NULL,',100,204,1104,',112,1,104.053000,30.694000,'08:00-20:00','成都金牛末端营业部',1000,NOW(),NOW(),0),
(1213,@tenant_id,'武汉江岸营业部','武汉江岸',1105,4,420000,420100,420102,'武汉市江岸区中山大道1234号','027-83001213',NULL,',100,205,1105,',113,1,114.305000,30.595000,'08:00-20:00','武汉江岸末端营业部',1000,NOW(),NOW(),0),
(1214,@tenant_id,'武汉江汉营业部','武汉江汉',1105,4,420000,420100,420103,'武汉市江汉区解放大道688号','027-83001214',NULL,',100,205,1105,',114,1,114.273000,30.603000,'08:00-20:00','武汉江汉末端营业部',1000,NOW(),NOW(),0),
(1215,@tenant_id,'武汉武昌营业部','武汉武昌',1105,4,420000,420100,420106,'武汉市武昌区中南路99号','027-83001215',NULL,',100,205,1105,',115,1,114.337000,30.555000,'08:00-20:00','武汉武昌末端营业部',1000,NOW(),NOW(),0),
(1216,@tenant_id,'西安新城营业部','西安新城',1106,4,610000,610100,610102,'西安市新城区解放路111号','029-83001216',NULL,',100,206,1106,',116,1,108.966000,34.267000,'08:00-20:00','西安新城末端营业部',1000,NOW(),NOW(),0),
(1217,@tenant_id,'西安碑林营业部','西安碑林',1106,4,610000,610100,610103,'西安市碑林区南稍门中贸广场','029-83001217',NULL,',100,206,1106,',117,1,108.939000,34.255000,'08:00-20:00','西安碑林末端营业部',1000,NOW(),NOW(),0),
(1218,@tenant_id,'沈阳和平营业部','沈阳和平',1107,4,210000,210100,210102,'沈阳市和平区太原北街86号','024-86001218',NULL,',100,207,1107,',118,1,123.413000,41.794000,'08:00-20:00','沈阳和平末端营业部',1000,NOW(),NOW(),0),
(1219,@tenant_id,'沈阳沈河营业部','沈阳沈河',1107,4,210000,210100,210103,'沈阳市沈河区中街路128号','024-86001219',NULL,',100,207,1107,',119,1,123.450000,41.798000,'08:00-20:00','沈阳沈河末端营业部',1000,NOW(),NOW(),0);

-- =====================================================================
-- §2 岗位 sys_station（硬约束：id=2 必须“司机”、id=3 必须“快递员”，
--    被 StaticStation.DRIVER_ID/COURIER_ID 与司机/快递员下拉使用）
-- =====================================================================
DELETE FROM `sys_station` WHERE tenant_id=@tenant_id AND id BETWEEN 1 AND 9;
INSERT INTO `sys_station`
(`id`,`tenant_id`,`name`,`org_id`,`status`,`description`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(1,@tenant_id,'运营管理员',100,1,'运营/调度管理岗',1000,NOW(),NOW(),0),
(2,@tenant_id,'司机',201,1,'干线/支线司机（StaticStation.DRIVER_ID=2）',1000,NOW(),NOW(),0),
(3,@tenant_id,'快递员',1201,1,'末端取派快递员（StaticStation.COURIER_ID=3）',1000,NOW(),NOW(),0),
(4,@tenant_id,'分拣员',201,1,'转运中心分拣',1000,NOW(),NOW(),0),
(5,@tenant_id,'仓库管理员',202,1,'出入库管理',1000,NOW(),NOW(),0),
(6,@tenant_id,'客服专员',100,1,'客户服务/工单',1000,NOW(),NOW(),0),
(7,@tenant_id,'财务专员',100,1,'对账/结算',1000,NOW(),NOW(),0),
(8,@tenant_id,'车辆管理员',201,1,'车辆/证照/年检管理',1000,NOW(),NOW(),0),
(9,@tenant_id,'调度专员',201,1,'智能调度监控',1000,NOW(),NOW(),0);

-- =====================================================================
-- §3 角色 sys_role（保留内置 id=1 ADMIN；仅新增业务角色，不绑菜单/资源）
-- =====================================================================
DELETE FROM `sys_role` WHERE tenant_id=@tenant_id AND id BETWEEN 2 AND 8;
INSERT INTO `sys_role`
(`id`,`tenant_id`,`name`,`code`,`description`,`status`,`readonly`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(2,@tenant_id,'运营调度员','OPS_DISPATCHER','调度监控与异常处理',1,0,1000,NOW(),NOW(),0),
(3,@tenant_id,'司机','ROLE_DRIVER','干线司机角色',1,0,1000,NOW(),NOW(),0),
(4,@tenant_id,'快递员','ROLE_COURIER','末端取派角色',1,0,1000,NOW(),NOW(),0),
(5,@tenant_id,'仓库管理员','WAREHOUSE','出入库/分拣',1,0,1000,NOW(),NOW(),0),
(6,@tenant_id,'客服','CUSTOMER_SERVICE','工单客服',1,0,1000,NOW(),NOW(),0),
(7,@tenant_id,'财务','FINANCE','对账结算',1,0,1000,NOW(),NOW(),0),
(8,@tenant_id,'车队长','FLEET_MANAGER','车队与司机管理',1,0,1000,NOW(),NOW(),0);

-- =====================================================================
-- §4 演示账号 demo01~demo10（密码统一 123456）+ 既有司机/快递员补岗位
--   受 uk_tenant_account 约束，"demo 十条"落地为 demo01~demo10 十个独立账号：
--     demo01~demo07 = 司机（sys_user.id 与 base_truck_driver.id 同值 9111~9117）
--     demo08~demo10 = 快递员（9211~9213）
--   既有 driver01~03(9101~9103)/courier01~02(9201~9202) 仅补 station_id，不改凭据。
-- =====================================================================
DELETE FROM `sys_user_role` WHERE tenant_id=@tenant_id AND id BETWEEN 90001 AND 90020;
DELETE FROM `sys_user`      WHERE tenant_id=@tenant_id AND id BETWEEN 9111 AND 9213;

INSERT INTO `sys_user`
(`id`,`tenant_id`,`account`,`name`,`org_id`,`station_id`,`email`,`mobile`,`sex`,`status`,`avatar`,`work_describe`,`password`,`password_expire_time`,`last_login_time`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(9111,@tenant_id,'demo01','李建国',201,2,'demo01@pinda-tms.cn','18801000011',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9112,@tenant_id,'demo02','王海涛',201,2,'demo02@pinda-tms.cn','18801000012',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9113,@tenant_id,'demo03','张伟明',202,2,'demo03@pinda-tms.cn','18801000013',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9114,@tenant_id,'demo04','陈志远',202,2,'demo04@pinda-tms.cn','18801000014',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9115,@tenant_id,'demo05','刘志强',203,2,'demo05@pinda-tms.cn','18801000015',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9116,@tenant_id,'demo06','赵德柱',204,2,'demo06@pinda-tms.cn','18801000016',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9117,@tenant_id,'demo07','孙大鹏',205,2,'demo07@pinda-tms.cn','18801000017',1,1,'','干线司机',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9201,@tenant_id,'demo11','罗建军',1213,3,'demo11@pinda-tms.cn','18801000021',1,1,'','武汉江岸末端快递员',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9202,@tenant_id,'demo12','黄文斌',1214,3,'demo12@pinda-tms.cn','18801000022',1,1,'','武汉江汉末端快递员',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9211,@tenant_id,'demo08','周小勇',1201,3,'demo08@pinda-tms.cn','18801000018',1,1,'','末端快递员',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9212,@tenant_id,'demo09','吴晓军',1206,3,'demo09@pinda-tms.cn','18801000019',1,1,'','末端快递员',@pwd,NULL,NULL,1000,NOW(),NOW(),0),
(9213,@tenant_id,'demo10','郑志豪',1208,3,'demo10@pinda-tms.cn','18801000020',1,1,'','末端快递员',@pwd,NULL,NULL,1000,NOW(),NOW(),0);

-- 用户-角色（司机→3，快递员→4）
INSERT INTO `sys_user_role` (`id`,`tenant_id`,`user_id`,`role_id`) VALUES
(90001,@tenant_id,9111,3),(90002,@tenant_id,9112,3),(90003,@tenant_id,9113,3),
(90004,@tenant_id,9114,3),(90005,@tenant_id,9115,3),(90006,@tenant_id,9116,3),
(90007,@tenant_id,9117,3),
(90008,@tenant_id,9211,4),(90009,@tenant_id,9212,4),(90010,@tenant_id,9213,4),
(90011,@tenant_id,9201,4),(90012,@tenant_id,9202,4);

-- 既有一线账号仅补岗位（不动 account/password/org），让管理端司机/快递员下拉能查到人
UPDATE `sys_user` SET station_id=2, update_time=NOW() WHERE tenant_id=@tenant_id AND id IN (9101,9102,9103) AND station_id IS NULL;

-- =====================================================================
-- §5 会员 member_info（10 名实名 C 端客户）+ 会员地址簿（每人 2 条，共 20）
-- =====================================================================
DELETE FROM `member_address_book` WHERE tenant_id=@tenant_id AND id BETWEEN 1601 AND 1620;
DELETE FROM `member_info`         WHERE tenant_id=@tenant_id AND id BETWEEN 1501 AND 1510;

INSERT INTO `member_info`
(`id`,`tenant_id`,`phone`,`nickname`,`avatar`,`id_card_no`,`id_card_verify`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(1501,@tenant_id,'13710010001','林晓芳','','110101199203172421',1,1,NULL,DATE_SUB(NOW(),INTERVAL 120 DAY),NOW(),0),
(1502,@tenant_id,'13710010002','周子轩','','110105198906251231',1,1,NULL,DATE_SUB(NOW(),INTERVAL 96 DAY),NOW(),0),
(1503,@tenant_id,'13710010003','吴雅静','','310104199401120842',1,1,NULL,DATE_SUB(NOW(),INTERVAL 80 DAY),NOW(),0),
(1504,@tenant_id,'13710010004','郑昊然','','310115199107083015',1,1,NULL,DATE_SUB(NOW(),INTERVAL 64 DAY),NOW(),0),
(1505,@tenant_id,'13710010005','王诗涵','','440103199505210628',1,1,NULL,DATE_SUB(NOW(),INTERVAL 50 DAY),NOW(),0),
(1506,@tenant_id,'13710010006','冯俊杰','','440106198811034517',1,1,NULL,DATE_SUB(NOW(),INTERVAL 40 DAY),NOW(),0),
(1507,@tenant_id,'13710010007','蒋雨桐','','510104199602180046',1,1,NULL,DATE_SUB(NOW(),INTERVAL 30 DAY),NOW(),0),
(1508,@tenant_id,'13710010008','沈志强','','51010819870930551X',1,1,NULL,DATE_SUB(NOW(),INTERVAL 20 DAY),NOW(),0),
(1509,@tenant_id,'13710010009','韩雪宁','','420106199704120326',0,1,NULL,DATE_SUB(NOW(),INTERVAL 9 DAY),NOW(),0),
(1510,@tenant_id,'13710010010','曹睿明','','610103199308270914',1,1,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NOW(),0);

INSERT INTO `member_address_book`
(`id`,`tenant_id`,`member_id`,`contact_name`,`phone_number`,`extension_number`,`province_id`,`city_id`,`county_id`,`address`,`longitude`,`latitude`,`company_name`,`is_default`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
-- 林晓芳：北京东城寄 → 上海浦东收
(1601,@tenant_id,1501,'林晓芳','13710010001',NULL,110000,110100,110101,'北京市东城区安定门内大街88号院2号楼',116.416000,39.929000,'北京翰墨文化传媒有限公司',1,NULL,NOW(),NOW(),0),
(1602,@tenant_id,1501,'钱丽华','13917002011',NULL,310000,310100,310115,'上海市浦东新区世纪大道100号环球金融中心',121.534000,31.225000,'上海鼎峰贸易有限公司',0,NULL,NOW(),NOW(),0),
-- 周子轩：北京朝阳寄 → 广州天河收
(1603,@tenant_id,1502,'周子轩','13710010002',NULL,110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城B座',116.486000,39.921000,'北京云图智能科技有限公司',1,NULL,NOW(),NOW(),0),
(1604,@tenant_id,1502,'潘伟明','13922002022',NULL,440000,440100,440106,'广州市天河区天河路208号天河城写字楼',113.361000,23.125000,'广州粤海电子有限公司',0,NULL,NOW(),NOW(),0),
-- 吴雅静：上海徐汇寄 → 成都锦江收
(1605,@tenant_id,1503,'吴雅静','13710010003',NULL,310000,310100,310104,'上海市徐汇区漕溪北路18号实业大厦',121.437000,31.188000,'上海雅静服装设计有限公司',1,NULL,NOW(),NOW(),0),
(1606,@tenant_id,1503,'罗丹','13880002033',NULL,510000,510100,510104,'成都市锦江区春熙路南段8号阳光百货',104.080000,30.657000,'成都蓉锦商贸有限公司',0,NULL,NOW(),NOW(),0),
-- 郑昊然：上海浦东寄 → 武汉江岸收
(1607,@tenant_id,1504,'郑昊然','13710010004',NULL,310000,310100,310115,'上海市浦东新区张江高科技园区博云路2号',121.578000,31.207000,'上海昊然半导体有限公司',1,NULL,NOW(),NOW(),0),
(1608,@tenant_id,1504,'袁志刚','13971002044',NULL,420000,420100,420102,'武汉市江岸区中山大道1234号长江大厦',114.305000,30.595000,'武汉长江光通信有限公司',0,NULL,NOW(),NOW(),0),
-- 王诗涵：广州荔湾寄 → 西安新城收
(1609,@tenant_id,1505,'王诗涵','13710010005',NULL,440000,440100,440103,'广州市荔湾区中山八路23号富力广场',113.244000,23.126000,'广州诗涵美妆连锁',1,NULL,NOW(),NOW(),0),
(1610,@tenant_id,1505,'白亚军','13991002055',NULL,610000,610100,610102,'西安市新城区解放路111号民乐万达',108.966000,34.267000,'西安秦都百货有限公司',0,NULL,NOW(),NOW(),0),
-- 冯俊杰：广州天河寄 → 沈阳和平收
(1611,@tenant_id,1506,'冯俊杰','13710010006',NULL,440000,440100,440106,'广州市天河区体育西路103号维多利广场',113.326000,23.136000,'广州俊杰网络科技有限公司',1,NULL,NOW(),NOW(),0),
(1612,@tenant_id,1506,'高文斌','13940002066',NULL,210000,210100,210102,'沈阳市和平区太原北街86号中兴商业大厦',123.413000,41.794000,'沈阳中兴商业集团',0,NULL,NOW(),NOW(),0),
-- 蒋雨桐：成都锦江寄 → 北京海淀收
(1613,@tenant_id,1507,'蒋雨桐','13710010007',NULL,510000,510100,510104,'成都市锦江区红星路三段1号IFS国际金融中心',104.081000,30.655000,'成都雨桐文创有限公司',1,NULL,NOW(),NOW(),0),
(1614,@tenant_id,1507,'许文博','13910002077',NULL,110000,110100,110108,'北京市海淀区中关村大街27号中关村大厦',116.298000,39.959000,'北京博研信息技术研究院',0,NULL,NOW(),NOW(),0),
-- 沈志强：成都金牛寄 → 上海徐汇收
(1615,@tenant_id,1508,'沈志强','13710010008',NULL,510000,510100,510108,'成都市金牛区沙湾路258号老仓工业园',104.053000,30.694000,'成都志强精密机械厂',1,NULL,NOW(),NOW(),0),
(1616,@tenant_id,1508,'苏婉清','13917002088',NULL,310000,310100,310104,'上海市徐汇区宜山路700号普天科技园',121.428000,31.183000,'上海清研检测技术有限公司',0,NULL,NOW(),NOW(),0),
-- 韩雪宁：武汉武昌寄 → 广州白云收（未实名，演示待核验会员）
(1617,@tenant_id,1509,'韩雪宁','13710010009',NULL,420000,420100,420106,'武汉市武昌区中南路99号银泰创意城',114.337000,30.555000,'武汉雪宁个人工作室',1,NULL,NOW(),NOW(),0),
(1618,@tenant_id,1509,'罗永强','13922002099',NULL,440000,440100,440111,'广州市白云区机场路云霄路88号云港大厦',113.259000,23.158000,'广州白云永强汽配商行',0,NULL,NOW(),NOW(),0),
-- 曹睿明：西安碑林寄 → 成都武侯收
(1619,@tenant_id,1510,'曹睿明','13710010010',NULL,610000,610100,610103,'西安市碑林区南稍门中贸广场15栋',108.939000,34.255000,'西安睿明仪器设备有限公司',1,NULL,NOW(),NOW(),0),
(1620,@tenant_id,1510,'唐丽娟','13880002100',NULL,510000,510100,510107,'成都市武侯区人民南路四段12号华宇大厦',104.068000,30.635000,'成都武侯丽娟电商运营中心',0,NULL,NOW(),NOW(),0);

-- =====================================================================
-- §6 基础运力网络：车队 / 车型 / 货型 / 线型 / 线路 / 车次 / 车辆 / 司机 / 证照 / 绑定 / 作业范围
-- =====================================================================

-- 6.1 七个城市自营车队（既有 610 品达自营车队保留；老司机统一并入 610）
DELETE FROM `base_fleet` WHERE tenant_id=@tenant_id AND id BETWEEN 611 AND 617;
INSERT INTO `base_fleet`
(`id`,`tenant_id`,`name`,`fleet_number`,`org_id`,`manager`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
-- manager 契约为 sys_user.id（非姓名）：优先挂同机构司机用户，206/207 无员工挂 admin01/admin02
(611,@tenant_id,'北京自营车队','BJ-FLEET-02',201,'9111',1,1000,NOW(),NOW(),0),
(612,@tenant_id,'上海自营车队','SH-FLEET-03',202,'9113',1,1000,NOW(),NOW(),0),
(613,@tenant_id,'广州自营车队','GZ-FLEET-04',203,'9115',1,1000,NOW(),NOW(),0),
(614,@tenant_id,'成都自营车队','CD-FLEET-05',204,'9116',1,1000,NOW(),NOW(),0),
(615,@tenant_id,'武汉自营车队','WH-FLEET-06',205,'9117',1,1000,NOW(),NOW(),0),
(616,@tenant_id,'西安自营车队','XA-FLEET-07',206,'9001',1,1000,NOW(),NOW(),0),
(617,@tenant_id,'沈阳自营车队','SY-FLEET-08',207,'9002',1,1000,NOW(),NOW(),0);

-- 6.2 车辆类型（既有 601 中型厢式保留）
DELETE FROM `base_truck_type` WHERE tenant_id=@tenant_id AND id BETWEEN 602 AND 604;
INSERT INTO `base_truck_type`
(`id`,`tenant_id`,`name`,`allowable_load`,`allowable_volume`,`measure_long`,`measure_width`,`measure_high`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(602,@tenant_id,'重型厢式货车',10000.00,55.000,9.60,2.40,2.60,1,1000,NOW(),NOW(),0),
(603,@tenant_id,'轻型厢式货车',1500.00,12.000,4.20,1.80,1.80,1,1000,NOW(),NOW(),0),
(604,@tenant_id,'新能源微面',800.00,6.000,2.70,1.50,1.40,1,1000,NOW(),NOW(),0);

-- 6.3 货物类型（10 个真实常见货类；脏的 ddd/测试货型不在此处理，见清理清单）
DELETE FROM `base_goods_type` WHERE tenant_id=@tenant_id AND id BETWEEN 611 AND 620;
INSERT INTO `base_goods_type`
(`id`,`tenant_id`,`name`,`default_weight`,`default_volume`,`remark`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(611,@tenant_id,'电子产品',3.000,0.0200,'手机/电脑/配件，防震',1,1000,NOW(),NOW(),0),
(612,@tenant_id,'服装鞋帽',1.500,0.0120,'袋装/箱装纺织物',1,1000,NOW(),NOW(),0),
(613,@tenant_id,'食品饮料',10.000,0.0300,'常规预包装食品',1,1000,NOW(),NOW(),0),
(614,@tenant_id,'日用百货',5.000,0.0250,'日化/家居杂项',1,1000,NOW(),NOW(),0),
(615,@tenant_id,'生鲜冷链',8.000,0.0220,'需冷链温控运输',1,1000,NOW(),NOW(),0),
(616,@tenant_id,'家居建材',25.000,0.1500,'大件/重货',1,1000,NOW(),NOW(),0),
(617,@tenant_id,'图书文具',2.000,0.0080,'纸张印刷品',1,1000,NOW(),NOW(),0),
(618,@tenant_id,'美妆个护',1.000,0.0050,'轻小件',1,1000,NOW(),NOW(),0),
(619,@tenant_id,'工业配件',20.000,0.0600,'金属机械配件',1,1000,NOW(),NOW(),0),
(620,@tenant_id,'文件证件',0.300,0.0020,'信封/文件袋',1,1000,NOW(),NOW(),0);

-- 6.4 车型-货型适配（24 条）
DELETE FROM `base_truck_type_goods` WHERE tenant_id=@tenant_id AND id BETWEEN 7001 AND 7024;
INSERT INTO `base_truck_type_goods` (`id`,`tenant_id`,`truck_type_id`,`goods_type_id`) VALUES
(7001,@tenant_id,601,611),(7002,@tenant_id,601,612),(7003,@tenant_id,601,613),(7004,@tenant_id,601,614),
(7005,@tenant_id,601,617),(7006,@tenant_id,601,618),(7007,@tenant_id,601,620),
(7008,@tenant_id,602,613),(7009,@tenant_id,602,614),(7010,@tenant_id,602,615),(7011,@tenant_id,602,616),(7012,@tenant_id,602,619),
(7013,@tenant_id,603,611),(7014,@tenant_id,603,612),(7015,@tenant_id,603,613),(7016,@tenant_id,603,614),
(7017,@tenant_id,603,617),(7018,@tenant_id,603,618),(7019,@tenant_id,603,620),
(7020,@tenant_id,604,611),(7021,@tenant_id,604,612),(7022,@tenant_id,604,617),(7023,@tenant_id,604,618),(7024,@tenant_id,604,620);

-- 6.5 线路类型（补建：100 城际干线 / 101 区域支线 / 102 末端配送）
DELETE FROM `base_transport_line_type` WHERE tenant_id=@tenant_id AND id BETWEEN 100 AND 102;
INSERT INTO `base_transport_line_type`
(`id`,`tenant_id`,`name`,`type_number`,`start_agency_type`,`end_agency_type`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(100,@tenant_id,'城际干线','LINE_TRUNK',2,2,1,1000,NOW(),NOW(),0),
(101,@tenant_id,'区域支线','LINE_BRANCH',2,3,1,1000,NOW(),NOW(),0),
(102,@tenant_id,'末端配送','LINE_FEEDER',3,4,1,1000,NOW(),NOW(),0);

-- 既有 6 条干线线型原为 NULL，回填为城际干线（仅补字段）
UPDATE `base_transport_line` SET transport_line_type_id=100, update_time=NOW()
WHERE tenant_id=@tenant_id AND id BETWEEN 301 AND 306 AND transport_line_type_id IS NULL;

-- 6.6 新业务线路 311~320（3 条城际干线 + 7 条枢纽-片区接驳支线）
DELETE FROM `base_transport_line` WHERE tenant_id=@tenant_id AND id BETWEEN 311 AND 320;
INSERT INTO `base_transport_line`
(`id`,`tenant_id`,`name`,`line_number`,`org_id`,`transport_line_type_id`,`start_org_id`,`end_org_id`,`distance`,`cost`,`estimated_time`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(311,@tenant_id,'沪蓉干线','LINE-SH-CD',100,100,202,204,1960.00,9800.00,1480,1,1000,NOW(),NOW(),0),
(312,@tenant_id,'沪穗干线','LINE-SH-GZ',100,100,202,203,1450.00,7250.00,1090,1,1000,NOW(),NOW(),0),
(313,@tenant_id,'京蓉干线','LINE-BJ-CD',100,100,201,204,1810.00,9050.00,1360,1,1000,NOW(),NOW(),0),
(314,@tenant_id,'京枢-南城接驳','BR-BJ-01',201,101,201,1101,45.00,260.00,70,1,1000,NOW(),NOW(),0),
(315,@tenant_id,'沪枢-青浦接驳','BR-SH-01',202,101,202,1102,38.00,220.00,60,1,1000,NOW(),NOW(),0),
(316,@tenant_id,'穗枢-白云接驳','BR-GZ-01',203,101,203,1103,30.00,180.00,50,1,1000,NOW(),NOW(),0),
(317,@tenant_id,'蓉枢-新都接驳','BR-CD-01',204,101,204,1104,42.00,240.00,65,1,1000,NOW(),NOW(),0),
(318,@tenant_id,'汉枢-径河接驳','BR-WH-01',205,101,205,1105,35.00,200.00,55,1,1000,NOW(),NOW(),0),
(319,@tenant_id,'陕枢-灞桥接驳','BR-XA-01',206,101,206,1106,40.00,230.00,60,1,1000,NOW(),NOW(),0),
(320,@tenant_id,'沈枢-于洪接驳','BR-SY-01',207,101,207,1107,36.00,210.00,58,1,1000,NOW(),NOW(),0);

-- 6.7 新车次 411~420（period=1 天班）
DELETE FROM `base_transport_trips` WHERE tenant_id=@tenant_id AND id BETWEEN 411 AND 420;
INSERT INTO `base_transport_trips`
(`id`,`tenant_id`,`name`,`departure_time`,`transport_line_id`,`period`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(411,@tenant_id,'沪蓉早班','09:30',311,1,1,1000,NOW(),NOW(),0),
(412,@tenant_id,'沪穗午班','13:30',312,1,1,1000,NOW(),NOW(),0),
(413,@tenant_id,'京蓉早班','08:20',313,1,1,1000,NOW(),NOW(),0),
(414,@tenant_id,'北京南城接驳早班','06:30',314,1,1,1000,NOW(),NOW(),0),
(415,@tenant_id,'上海青浦接驳早班','06:00',315,1,1,1000,NOW(),NOW(),0),
(416,@tenant_id,'广州白云接驳早班','07:00',316,1,1,1000,NOW(),NOW(),0),
(417,@tenant_id,'成都新都接驳早班','06:20',317,1,1,1000,NOW(),NOW(),0),
(418,@tenant_id,'武汉径河接驳早班','06:40',318,1,1,1000,NOW(),NOW(),0),
(419,@tenant_id,'西安灞桥接驳早班','06:50',319,1,1,1000,NOW(),NOW(),0),
(420,@tenant_id,'沈阳于洪接驳早班','07:10',320,1,1,1000,NOW(),NOW(),0);

-- 6.8 新车辆 521~530（在途干线/接驳车置在线 + 心跳）
DELETE FROM `base_truck` WHERE tenant_id=@tenant_id AND id BETWEEN 521 AND 530;
INSERT INTO `base_truck`
(`id`,`tenant_id`,`truck_type_id`,`fleet_id`,`brand`,`license_plate`,`device_gps_id`,`allowable_load`,`allowable_volume`,`truck_license_id`,`status`,`online_status`,`last_heartbeat_time`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(521,@tenant_id,602,614,'东风天龙KL','川A7F208','GPS621',10000.00,55.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(522,@tenant_id,602,615,'东风天龙KL','鄂A9K516','GPS622',10000.00,55.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(523,@tenant_id,602,616,'解放J6P','陕A6G128','GPS623',10000.00,55.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(524,@tenant_id,602,617,'解放J6P','辽A8H336','GPS624',10000.00,55.000,NULL,1,0,NULL,1000,NOW(),NOW(),0),
(525,@tenant_id,603,611,'江铃顺达','京AA2098','GPS625',1500.00,12.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(526,@tenant_id,603,612,'江铃顺达','沪B55168','GPS626',1500.00,12.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(527,@tenant_id,603,613,'江铃顺达','粤A3T268','GPS627',1500.00,12.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0),
(528,@tenant_id,603,615,'江淮帅铃','鄂A2K608','GPS628',1500.00,12.000,NULL,1,0,NULL,1000,NOW(),NOW(),0),
(529,@tenant_id,603,616,'江淮帅铃','陕A5D718','GPS629',1500.00,12.000,NULL,1,0,NULL,1000,NOW(),NOW(),0),
(530,@tenant_id,604,614,'瑞驰EC35','川A2N706','GPS630',800.00,6.000,NULL,1,1,NOW(),1000,NOW(),NOW(),0);

-- 6.9 新司机档案 9111~9117（id 与 sys_user 同值；运行时 driver_id 即 sys_user.id）；老司机并入自营车队
DELETE FROM `base_truck_driver` WHERE tenant_id=@tenant_id AND id BETWEEN 9111 AND 9117;
INSERT INTO `base_truck_driver`
(`id`,`tenant_id`,`user_id`,`fleet_id`,`age`,`driving_age`,`picture`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(9111,@tenant_id,9111,611,36,12,'',1000,NOW(),NOW(),0),
(9112,@tenant_id,9112,611,31,7,'',1000,NOW(),NOW(),0),
(9113,@tenant_id,9113,612,38,14,'',1000,NOW(),NOW(),0),
(9114,@tenant_id,9114,612,29,5,'',1000,NOW(),NOW(),0),
(9115,@tenant_id,9115,613,41,18,'',1000,NOW(),NOW(),0),
(9116,@tenant_id,9116,614,34,10,'',1000,NOW(),NOW(),0),
(9117,@tenant_id,9117,615,45,22,'',1000,NOW(),NOW(),0);
UPDATE `base_truck_driver` SET fleet_id=610, update_time=NOW()
WHERE tenant_id=@tenant_id AND id IN (9101,9102,9103) AND (fleet_id IS NULL OR fleet_id=0);

-- 6.10 车次-车辆-司机绑定（含既有 401~406；driver_id 存 sys_user.id）
DELETE FROM `base_trips_truck_driver` WHERE tenant_id=@tenant_id AND id BETWEEN 8001 AND 8016;
INSERT INTO `base_trips_truck_driver` (`id`,`tenant_id`,`trips_id`,`truck_id`,`driver_id`) VALUES
(8001,@tenant_id,401,501,9111),(8002,@tenant_id,402,502,9112),(8003,@tenant_id,403,503,9113),
(8004,@tenant_id,404,507,9115),(8005,@tenant_id,405,508,9116),(8006,@tenant_id,406,511,9101),
(8007,@tenant_id,411,521,9114),(8008,@tenant_id,412,522,9102),(8009,@tenant_id,413,523,9117),
(8010,@tenant_id,414,525,9111),(8011,@tenant_id,415,526,9113),(8012,@tenant_id,416,527,9115),
(8013,@tenant_id,417,530,9116),(8014,@tenant_id,418,528,9117),(8015,@tenant_id,419,529,9103),(8016,@tenant_id,420,524,9101);

-- 6.11 车辆行驶证（老 12 台 + 新 10 台，共 22 本），并回填 base_truck.truck_license_id
DELETE FROM `base_truck_license` WHERE tenant_id=@tenant_id AND id BETWEEN 6001 AND 6022;
INSERT INTO `base_truck_license`
(`id`,`tenant_id`,`truck_id`,`engine_number`,`registration_date`,`mandatory_scrap`,`expiration_date`,`overall_quality`,`allowable_weight`,`outside_dimensions`,`validity_period`,`transport_certificate_number`,`picture`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(6001,@tenant_id,501,'ENG501A0001','2020-03-12','2035-03-12','2027-03-31',4200.00,5000.00,'6.00×2.20×2.20','2027-03-31','道货证京1100501','',1000,NOW(),NOW(),0),
(6002,@tenant_id,502,'ENG502A0002','2020-04-18','2035-04-18','2027-04-30',4200.00,5000.00,'6.00×2.20×2.20','2027-04-30','道货证京1100502','',1000,NOW(),NOW(),0),
(6003,@tenant_id,503,'ENG503B0003','2020-06-22','2035-06-22','2027-06-30',4200.00,5000.00,'6.00×2.20×2.20','2027-06-30','道货证沪3100503','',1000,NOW(),NOW(),0),
(6004,@tenant_id,504,'ENG504A0004','2020-08-09','2035-08-09','2027-08-31',4200.00,5000.00,'6.00×2.20×2.20','2027-08-31','道货证京1100504','',1000,NOW(),NOW(),0),
(6005,@tenant_id,505,'ENG505A0005','2020-09-25','2035-09-25','2027-09-30',4200.00,5000.00,'6.00×2.20×2.20','2027-09-30','道货证京1100505','',1000,NOW(),NOW(),0),
(6006,@tenant_id,506,'ENG506B0006','2020-11-11','2035-11-11','2027-11-30',4200.00,5000.00,'6.00×2.20×2.20','2027-11-30','道货证沪3100506','',1000,NOW(),NOW(),0),
(6007,@tenant_id,507,'ENG507C0007','2021-01-15','2036-01-15','2028-01-31',4200.00,5000.00,'6.00×2.20×2.20','2028-01-31','道货证粤4400507','',1000,NOW(),NOW(),0),
(6008,@tenant_id,508,'ENG508D0008','2021-02-20','2036-02-20','2028-02-28',4200.00,5000.00,'6.00×2.20×2.20','2028-02-28','道货证川5100508','',1000,NOW(),NOW(),0),
(6009,@tenant_id,509,'ENG509E0009','2021-03-30','2036-03-30','2028-03-31',4200.00,5000.00,'6.00×2.20×2.20','2028-03-31','道货证鄂4200509','',1000,NOW(),NOW(),0),
(6010,@tenant_id,510,'ENG510F0010','2021-05-08','2036-05-08','2028-05-31',4200.00,5000.00,'6.00×2.20×2.20','2028-05-31','道货证陕6100510','',1000,NOW(),NOW(),0),
(6011,@tenant_id,511,'ENG511G0011','2021-06-19','2036-06-19','2028-06-30',4200.00,5000.00,'6.00×2.20×2.20','2028-06-30','道货证辽2100511','',1000,NOW(),NOW(),0),
(6012,@tenant_id,512,'ENG512H0012','2021-07-27','2036-07-27','2028-07-31',4200.00,5000.00,'6.00×2.20×2.20','2028-07-31','道货证浙3300512','',1000,NOW(),NOW(),0),
(6013,@tenant_id,521,'ENG521D0013','2022-03-09','2037-03-09','2028-03-31',9800.00,10000.00,'9.60×2.40×2.60','2028-03-31','道货证川5100521','',1000,NOW(),NOW(),0),
(6014,@tenant_id,522,'ENG522E0014','2022-04-14','2037-04-14','2028-04-30',9800.00,10000.00,'9.60×2.40×2.60','2028-04-30','道货证鄂4200522','',1000,NOW(),NOW(),0),
(6015,@tenant_id,523,'ENG523F0015','2022-05-21','2037-05-21','2028-05-31',9800.00,10000.00,'9.60×2.40×2.60','2028-05-31','道货证陕6100523','',1000,NOW(),NOW(),0),
(6016,@tenant_id,524,'ENG524G0016','2022-06-17','2037-06-17','2028-06-30',9800.00,10000.00,'9.60×2.40×2.60','2028-06-30','道货证辽2100524','',1000,NOW(),NOW(),0),
(6017,@tenant_id,525,'ENG525A0017','2022-08-03','2037-08-03','2028-08-31',2600.00,1500.00,'4.20×1.80×1.80','2028-08-31','道货证京1100525','',1000,NOW(),NOW(),0),
(6018,@tenant_id,526,'ENG526B0018','2022-09-12','2037-09-12','2028-09-30',2600.00,1500.00,'4.20×1.80×1.80','2028-09-30','道货证沪3100526','',1000,NOW(),NOW(),0),
(6019,@tenant_id,527,'ENG527C0019','2022-10-22','2037-10-22','2028-10-31',2600.00,1500.00,'4.20×1.80×1.80','2028-10-31','道货证粤4400527','',1000,NOW(),NOW(),0),
(6020,@tenant_id,528,'ENG528E0020','2022-11-18','2037-11-18','2028-11-30',2600.00,1500.00,'4.20×1.80×1.80','2028-11-30','道货证鄂4200528','',1000,NOW(),NOW(),0),
(6021,@tenant_id,529,'ENG529F0021','2022-12-06','2037-12-06','2028-12-31',2600.00,1500.00,'4.20×1.80×1.80','2028-12-31','道货证陕6100529','',1000,NOW(),NOW(),0),
(6022,@tenant_id,530,'ENG530D0022','2023-02-14','2038-02-14','2029-02-28',1500.00,800.00,'2.70×1.50×1.40','2029-02-28','道货证川5100530','',1000,NOW(),NOW(),0);
UPDATE `base_truck` SET truck_license_id = CASE id
  WHEN 501 THEN 6001 WHEN 502 THEN 6002 WHEN 503 THEN 6003 WHEN 504 THEN 6004
  WHEN 505 THEN 6005 WHEN 506 THEN 6006 WHEN 507 THEN 6007 WHEN 508 THEN 6008
  WHEN 509 THEN 6009 WHEN 510 THEN 6010 WHEN 511 THEN 6011 WHEN 512 THEN 6012
  WHEN 521 THEN 6013 WHEN 522 THEN 6014 WHEN 523 THEN 6015 WHEN 524 THEN 6016
  WHEN 525 THEN 6017 WHEN 526 THEN 6018 WHEN 527 THEN 6019 WHEN 528 THEN 6020
  WHEN 529 THEN 6021 WHEN 530 THEN 6022 END,
  update_time=NOW()
WHERE tenant_id=@tenant_id AND id BETWEEN 501 AND 530;

-- 6.12 司机驾驶证（老 3 + 新 7，共 10 本）
DELETE FROM `base_driver_license` WHERE tenant_id=@tenant_id AND id BETWEEN 5101 AND 5110;
INSERT INTO `base_driver_license`
(`id`,`tenant_id`,`driver_id`,`allowable_type`,`initial_certificate_date`,`valid_period`,`license_number`,`driver_age`,`license_type`,`qualification_certificate`,`pass_certificate`,`picture`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(5101,@tenant_id,9101,'A2','2008-05-20','2020-05-20至2030-05-20','110101199001011231',8,'A2','道路运输从业资格证','京交运入场证01','',1000,NOW(),NOW(),0),
(5102,@tenant_id,9102,'A2','2011-07-08','2021-07-08至2031-07-08','110102199502022342',5,'A2','道路运输从业资格证','京交运入场证02','',1000,NOW(),NOW(),0),
(5103,@tenant_id,9103,'A2','2003-09-15','2019-09-15至2029-09-15','110103198303033453',15,'A2','道路运输从业资格证','京交运入场证03','',1000,NOW(),NOW(),0),
(5104,@tenant_id,9111,'A2','2010-04-12','2020-04-12至2030-04-12','110105198904044564',12,'A2','道路运输从业资格证','京交运入场证04','',1000,NOW(),NOW(),0),
(5105,@tenant_id,9112,'B2','2017-06-22','2023-06-22至2033-06-22','110106199405055675',7,'B2','道路运输从业资格证','京交运入场证05','',1000,NOW(),NOW(),0),
(5106,@tenant_id,9113,'A2','2008-10-09','2020-10-09至2030-10-09','310104198706066786',14,'A2','道路运输从业资格证','沪交运入场证01','',1000,NOW(),NOW(),0),
(5107,@tenant_id,9114,'B2','2019-03-18','2025-03-18至2035-03-18','310105199607077897',5,'B2','道路运输从业资格证','沪交运入场证02','',1000,NOW(),NOW(),0),
(5108,@tenant_id,9115,'A2','2006-11-25','2018-11-25至2028-11-25','440103198408088908',18,'A2','道路运输从业资格证','粤交运入场证01','',1000,NOW(),NOW(),0),
(5109,@tenant_id,9116,'A2','2014-02-27','2024-02-27至2034-02-27','510104199109099019',10,'A2','道路运输从业资格证','川交运入场证01','',1000,NOW(),NOW(),0),
(5110,@tenant_id,9117,'A2','2002-12-03','2018-12-03至2028-12-03','420102198010100120',22,'A2','道路运输从业资格证','鄂交运入场证01','',1000,NOW(),NOW(),0);

-- 6.13 机构作业范围（按 19 个区县挂到所属城市一级枢纽；polygon 现网本就为空，按 area_id 行政区划生效）
DELETE FROM `base_org_scope` WHERE tenant_id=@tenant_id AND id BETWEEN 3001 AND 3019;
INSERT INTO `base_org_scope` (`id`,`tenant_id`,`org_id`,`area_id`,`polygon_points`) VALUES
(3001,@tenant_id,201,110101,NULL),(3002,@tenant_id,201,110105,NULL),(3003,@tenant_id,201,110108,NULL),
(3004,@tenant_id,202,310101,NULL),(3005,@tenant_id,202,310104,NULL),(3006,@tenant_id,202,310115,NULL),
(3007,@tenant_id,203,440103,NULL),(3008,@tenant_id,203,440106,NULL),(3009,@tenant_id,203,440111,NULL),
(3010,@tenant_id,204,510104,NULL),(3011,@tenant_id,204,510107,NULL),(3012,@tenant_id,204,510108,NULL),
(3013,@tenant_id,205,420102,NULL),(3014,@tenant_id,205,420103,NULL),(3015,@tenant_id,205,420106,NULL),
(3016,@tenant_id,206,610102,NULL),(3017,@tenant_id,206,610103,NULL),
(3018,@tenant_id,207,210102,NULL),(3019,@tenant_id,207,210103,NULL);

-- 6.14 快递员作业范围（9211 北京东城 / 9212 上海浦东 / 9213 广州天河 / 9201 武汉江岸 / 9202 武汉江汉）
DELETE FROM `base_courier_scope` WHERE tenant_id=@tenant_id AND id BETWEEN 3101 AND 3105;
INSERT INTO `base_courier_scope` (`id`,`tenant_id`,`courier_id`,`area_id`,`polygon_points`) VALUES
(3101,@tenant_id,9211,110101,NULL),
(3102,@tenant_id,9212,310115,NULL),
(3103,@tenant_id,9213,440106,NULL),
(3104,@tenant_id,9201,420102,NULL),
(3105,@tenant_id,9202,420103,NULL);

-- =====================================================================
-- §7 订单全链路（20 单，910001~910020）：今日 10 单 + 近 7 天 10 单，覆盖订单 12 态
--   每单配套：1 条货物 / 1 条位置 / 1 条 FREIGHT 运费 / 1 条支付单 / 1 条运单
--   仅 O5/O6/O7/O8/O11/O12/O14 挂干线运输任务（work_transport_order_task）
-- =====================================================================
DELETE FROM `oms_order_cargo`       WHERE tenant_id=@tenant_id AND id BETWEEN 930001 AND 930020;
DELETE FROM `oms_order_location`    WHERE tenant_id=@tenant_id AND id BETWEEN 912001 AND 912020;
DELETE FROM `oms_freight_detail`    WHERE tenant_id=@tenant_id AND id BETWEEN 913001 AND 913020;
DELETE FROM `pd_payment_order`      WHERE tenant_id=@tenant_id AND order_id BETWEEN 910001 AND 910020;
DELETE FROM `work_transport_order_task` WHERE tenant_id=@tenant_id AND id BETWEEN 940001 AND 940007;
DELETE FROM `work_transport_order`  WHERE tenant_id=@tenant_id AND id BETWEEN 920001 AND 920020;
DELETE FROM `oms_order`             WHERE tenant_id=@tenant_id AND id BETWEEN 910001 AND 910020;

-- 7.1 订单（时间：今日单为今天各时刻，历史单为 T-1~T-6；金额对齐计费规则量级）
INSERT INTO `oms_order`
(`id`,`tenant_id`,`order_no`,`order_type`,`pickup_type`,`status`,`cancel_reason`,`cancel_time`,`member_id`,
 `sender_name`,`sender_phone`,`sender_province_id`,`sender_city_id`,`sender_county_id`,`sender_address`,`sender_address_id`,
 `receiver_name`,`receiver_phone`,`receiver_province_id`,`receiver_city_id`,`receiver_county_id`,`receiver_address`,`receiver_address_id`,
 `current_org_id`,`payment_method`,`payment_status`,`amount`,`distance`,`estimated_arrival_time`,
 `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
-- ===== 今日 10 单 =====
(910001,@tenant_id,'PD910001',2,2,1,NULL,NULL,1501,'林晓芳','13710010001',110000,110100,110101,'北京市东城区安定门内大街88号院2号楼',1601,'钱丽华','13917002011',310000,310100,310115,'上海市浦东新区世纪大道100号环球金融中心',1602,1201,1,1,50.00,1213.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),0),
(910002,@tenant_id,'PD910002',2,2,2,NULL,NULL,1502,'周子轩','13710010002',110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城B座',1603,'潘伟明','13922002022',440000,440100,440106,'广州市天河区天河路208号天河城写字楼',1604,1202,1,2,35.00,2120.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(8,55,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,55,0)),NULL,NOW(),0),
(910003,@tenant_id,'PD910003',1,1,3,NULL,NULL,1502,'周子轩','13710010002',110000,110100,110108,'北京市海淀区中关村大街27号',NULL,'孟晨','13910010303',110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城',NULL,1203,1,2,44.00,32.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,10,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(9,10,0)),NULL,NOW(),0),
(910004,@tenant_id,'PD910004',2,2,4,NULL,NULL,1504,'郑昊然','13710010004',310000,310100,310115,'上海市浦东新区张江高科技园区博云路2号',1607,'袁志刚','13971002044',420000,420100,420102,'武汉市江岸区中山大道1234号长江大厦',1608,202,1,2,185.00,830.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),NULL,NOW(),0),
(910005,@tenant_id,'PD910005',2,1,5,NULL,NULL,1503,'吴雅静','13710010003',310000,310100,310104,'上海市徐汇区漕溪北路18号实业大厦',1605,'罗丹','13880002033',510000,510100,510104,'成都市锦江区春熙路南段8号阳光百货',1606,202,1,2,65.00,1960.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,50,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(9,50,0)),NULL,NOW(),0),
(910006,@tenant_id,'PD910006',2,2,6,NULL,NULL,1508,'沈志强','13710010008',310000,310100,310115,'上海市浦东新区康桥工业区',NULL,'唐丽娟','13880002100',510000,510100,510107,'成都市武侯区人民南路四段12号华宇大厦',1620,202,1,2,230.00,1960.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(10,15,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(10,15,0)),NULL,NOW(),0),
(910007,@tenant_id,'PD910007',2,2,6,NULL,NULL,1505,'王诗涵','13710010005',440000,440100,440103,'广州市荔湾区中山八路23号富力广场',1609,'白亚军','13991002055',610000,610100,610102,'西安市新城区解放路111号民乐万达',1610,1103,1,2,35.00,1720.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(10,40,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(10,40,0)),NULL,NOW(),0),
(910008,@tenant_id,'PD910008',2,2,7,NULL,NULL,1509,'韩雪宁','13710010009',420000,420100,420106,'武汉市武昌区中南路99号银泰创意城',1617,'罗丹','13880002033',510000,510100,510104,'成都市锦江区春熙路南段8号阳光百货',NULL,1104,2,1,275.00,1200.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(7,30,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(7,30,0)),NULL,NOW(),0),
(910009,@tenant_id,'PD910009',1,2,9,NULL,NULL,1501,'林晓芳','13710010001',110000,110100,110108,'北京市海淀区中关村大街1号',NULL,'孟晨','13910010303',110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城',NULL,1202,1,2,20.00,18.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(11,5,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(11,5,0)),NULL,NOW(),0),
(910010,@tenant_id,'PD910010',2,2,12,'客户临时改期，申请取消',TIMESTAMP(CURDATE(),MAKETIME(8,35,0)),1507,'蒋雨桐','13710010007',110000,110100,110101,'北京市东城区东直门外大街',NULL,'钱丽华','13917002011',310000,310100,310115,'上海市浦东新区世纪大道100号',1602,1201,1,1,35.00,1213.00,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),INTERVAL 2 DAY),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,35,0)),0),
-- ===== 近 7 天 10 单（含待派送/派送中/签收/拒收）=====
(910011,@tenant_id,'PD910011',2,2,8,NULL,NULL,1502,'周子轩','13710010002',110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城',1603,'潘伟明','13922002022',440000,440100,440106,'广州市天河区天河路208号天河城写字楼',1604,1208,1,2,50.00,2120.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 2 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 HOUR),0),
(910012,@tenant_id,'PD910012',2,2,9,NULL,NULL,1504,'郑昊然','13710010004',310000,310100,310115,'上海市浦东新区张江高科技园区博云路2号',1607,'袁志刚','13971002044',420000,420100,420102,'武汉市江岸区中山大道1234号长江大厦',1608,1213,2,1,125.00,830.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 2 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 HOUR),0),
(910013,@tenant_id,'PD910013',2,2,10,NULL,NULL,1501,'林晓芳','13710010001',110000,110100,110101,'北京市东城区安定门内大街88号院',1601,'苏婉清','13917002013',310000,310100,310101,'上海市黄浦区南京东路228号',NULL,1204,1,2,20.00,1213.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 5 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(910014,@tenant_id,'PD910014',2,2,10,NULL,NULL,1507,'蒋雨桐','13710010007',110000,110100,110108,'北京市海淀区中关村大街27号',NULL,'罗丹','13880002033',510000,510100,510104,'成都市锦江区春熙路南段8号阳光百货',NULL,1210,1,2,95.00,1810.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 1 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(910015,@tenant_id,'PD910015',2,2,10,NULL,NULL,1503,'吴雅静','13710010003',310000,310100,310104,'上海市徐汇区漕溪北路18号实业大厦',1605,'许文博','13910002077',110000,110100,110101,'北京市东城区安定门内大街90号',NULL,1201,1,2,65.00,1213.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 6 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(910016,@tenant_id,'PD910016',2,2,10,NULL,NULL,1509,'韩雪宁','13710010009',420000,420100,420102,'武汉市江岸区中山大道1200号',NULL,'钱丽华','13917002011',310000,310100,310115,'上海市浦东新区世纪大道100号',1602,1206,1,2,155.00,830.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 4 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(910017,@tenant_id,'PD910017',2,2,10,NULL,NULL,1506,'冯俊杰','13710010006',440000,440100,440106,'广州市天河区体育西路103号维多利广场',1611,'袁志刚','13971002047',420000,420100,420103,'武汉市江汉区解放大道688号',NULL,1214,2,2,95.00,1070.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 3 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(910018,@tenant_id,'PD910018',2,2,10,NULL,NULL,1502,'周子轩','13710010002',110000,110100,110105,'北京市朝阳区建国路88号SOHO现代城',1603,'袁志刚','13971002048',420000,420100,420106,'武汉市武昌区中南路99号银泰创意城',NULL,1215,1,2,140.00,1160.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 3 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(910019,@tenant_id,'PD910019',2,2,11,NULL,NULL,1503,'吴雅静','13710010003',310000,310100,310104,'上海市徐汇区漕溪北路18号实业大厦',1605,'罗永强','13922002099',440000,440100,440111,'广州市白云区机场路云霄路88号云港大厦',1618,1209,1,3,80.00,1450.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 1 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(910020,@tenant_id,'PD910020',2,2,10,NULL,NULL,1505,'王诗涵','13710010005',440000,440100,440103,'广州市荔湾区中山八路23号富力广场',1609,'许文博','13910002079',110000,110100,110108,'北京市海淀区中关村大街27号中关村大厦',NULL,1203,1,2,50.00,1880.00,DATE_ADD(DATE_SUB(NOW(),INTERVAL 5 DAY),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0);

-- 7.2 订单货物明细（transport_order_id 回填对应运单；下单链路本置空，种子补全以成链）
INSERT INTO `oms_order_cargo`
(`id`,`tenant_id`,`order_id`,`transport_order_id`,`goods_type_id`,`name`,`unit`,`cargo_value`,`cargo_barcode`,`quantity`,`volume`,`weight`,`total_volume`,`total_weight`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
(930001,@tenant_id,910001,920001,611,'智能手机（防震包装）','箱',5000.00,'PD910001001',1,0.0200,3.000,0.0200,3.000,'易碎品轻拿轻放',NULL,NOW(),NULL,NOW(),0),
(930002,@tenant_id,910002,920002,612,'秋冬服装','箱',1200.00,'PD910002001',2,0.0120,1.500,0.0240,3.000,NULL,NULL,NOW(),NULL,NOW(),0),
(930003,@tenant_id,910003,920003,612,'鞋帽礼盒','件',600.00,'PD910003001',1,0.0100,2.000,0.0100,2.000,NULL,NULL,NOW(),NULL,NOW(),0),
(930004,@tenant_id,910004,920004,613,'包装食品饮料','箱',800.00,'PD910004001',4,0.0300,10.000,0.1200,40.000,'常规预包装',NULL,NOW(),NULL,NOW(),0),
(930005,@tenant_id,910005,920005,611,'笔记本电脑','台',12000.00,'PD910005001',2,0.0100,2.000,0.0200,4.000,'高价值保价件',NULL,NOW(),NULL,NOW(),0),
(930006,@tenant_id,910006,920006,614,'日化日用百货','箱',1500.00,'PD910006001',5,0.0250,5.000,0.1250,25.000,NULL,NULL,NOW(),NULL,NOW(),0),
(930007,@tenant_id,910007,920007,618,'美妆护肤品','箱',900.00,'PD910007001',2,0.0050,1.000,0.0100,2.000,NULL,NULL,NOW(),NULL,NOW(),0),
(930008,@tenant_id,910008,920008,619,'机械工业配件','件',6000.00,'PD910008001',1,0.0600,18.000,0.0600,18.000,'重货',NULL,NOW(),NULL,NOW(),0),
(930009,@tenant_id,910009,920009,620,'合同文件资料','袋',200.00,'PD910009001',1,0.0020,0.300,0.0020,0.300,'当日达急件',NULL,NOW(),NULL,NOW(),0),
(930010,@tenant_id,910010,920010,611,'电子配件','箱',800.00,'PD910010001',1,0.0100,2.000,0.0100,2.000,NULL,NULL,NOW(),NULL,NOW(),0),
(930011,@tenant_id,910011,920011,612,'春夏服装','箱',1800.00,'PD910011001',3,0.0120,1.500,0.0360,4.500,NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(930012,@tenant_id,910012,920012,613,'休闲食品','箱',600.00,'PD910012001',1,0.0250,8.000,0.0250,8.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(930013,@tenant_id,910013,920013,620,'法律文书','袋',100.00,'PD910013001',1,0.0020,0.300,0.0020,0.300,NULL,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(930014,@tenant_id,910014,920014,611,'通讯设备','箱',8000.00,'PD910014001',2,0.0200,3.000,0.0400,6.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(930015,@tenant_id,910015,920015,617,'图书','箱',500.00,'PD910015001',2,0.0080,2.000,0.0160,4.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(930016,@tenant_id,910016,920016,614,'家居日用品','箱',1100.00,'PD910016001',2,0.0250,5.000,0.0500,10.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(930017,@tenant_id,910017,920017,612,'品牌服饰','箱',2400.00,'PD910017001',6,0.0100,1.000,0.0600,6.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(930018,@tenant_id,910018,920018,613,'礼盒食品','箱',900.00,'PD910018001',3,0.0200,3.000,0.0600,9.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(930019,@tenant_id,910019,920019,611,'智能硬件','箱',3000.00,'PD910019001',1,0.0120,5.000,0.0120,5.000,'客户拒收',NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(930020,@tenant_id,910020,920020,618,'个护美妆','箱',700.00,'PD910020001',3,0.0050,1.000,0.0150,3.000,NULL,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0);

-- 7.3 订单位置（一单一位置；坐标取寄/收网点 BD09 坐标）
INSERT INTO `oms_order_location`
(`id`,`tenant_id`,`order_id`,`send_longitude`,`send_latitude`,`receive_longitude`,`receive_latitude`,`send_org_id`,`receive_org_id`,`status`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
(912001,@tenant_id,910001,116.416000,39.929000,121.534000,31.225000,1201,1206,1,NULL,NOW(),NULL,NOW(),0),
(912002,@tenant_id,910002,116.486000,39.921000,113.361000,23.125000,1202,1208,1,NULL,NOW(),NULL,NOW(),0),
(912003,@tenant_id,910003,116.298000,39.959000,116.486000,39.921000,1203,1202,1,NULL,NOW(),NULL,NOW(),0),
(912004,@tenant_id,910004,121.534000,31.225000,114.305000,30.595000,1206,1213,1,NULL,NOW(),NULL,NOW(),0),
(912005,@tenant_id,910005,121.437000,31.188000,104.080000,30.657000,1205,1210,1,NULL,NOW(),NULL,NOW(),0),
(912006,@tenant_id,910006,121.534000,31.225000,104.043000,30.642000,1206,1211,1,NULL,NOW(),NULL,NOW(),0),
(912007,@tenant_id,910007,113.244000,23.126000,108.966000,34.267000,1207,1216,1,NULL,NOW(),NULL,NOW(),0),
(912008,@tenant_id,910008,114.337000,30.555000,104.080000,30.657000,1215,1210,1,NULL,NOW(),NULL,NOW(),0),
(912009,@tenant_id,910009,116.298000,39.959000,116.486000,39.921000,1203,1202,1,NULL,NOW(),NULL,NOW(),0),
(912010,@tenant_id,910010,116.416000,39.929000,121.534000,31.225000,1201,1206,1,NULL,NOW(),NULL,NOW(),0),
(912011,@tenant_id,910011,116.486000,39.921000,113.361000,23.125000,1202,1208,1,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(912012,@tenant_id,910012,121.534000,31.225000,114.305000,30.595000,1206,1213,1,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(912013,@tenant_id,910013,116.416000,39.929000,121.484000,31.231000,1201,1204,1,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(912014,@tenant_id,910014,116.298000,39.959000,104.080000,30.657000,1203,1210,1,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(912015,@tenant_id,910015,121.437000,31.188000,116.416000,39.929000,1205,1201,1,NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(912016,@tenant_id,910016,114.305000,30.595000,121.534000,31.225000,1213,1206,1,NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(912017,@tenant_id,910017,113.361000,23.125000,114.273000,30.603000,1208,1214,1,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(912018,@tenant_id,910018,116.486000,39.921000,114.337000,30.555000,1202,1215,1,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(912019,@tenant_id,910019,121.437000,31.188000,113.259000,23.158000,1205,1209,1,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(912020,@tenant_id,910020,113.244000,23.126000,116.298000,39.959000,1207,1203,1,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0);

-- 7.4 运费明细（现网仅 FREIGHT 一个真实费用项，与 SettlementServiceImpl 口径一致）
INSERT INTO `oms_freight_detail`
(`id`,`tenant_id`,`order_id`,`transport_order_id`,`fee_item`,`fee_item_name`,`quantity`,`unit`,`unit_price`,`amount`,`create_by`,`create_time`)
VALUES
(913001,@tenant_id,910001,920001,'FREIGHT','运费',1.00,'单',50.00,50.00,NULL,NOW()),
(913002,@tenant_id,910002,920002,'FREIGHT','运费',1.00,'单',35.00,35.00,NULL,NOW()),
(913003,@tenant_id,910003,920003,'FREIGHT','运费',1.00,'单',44.00,44.00,NULL,NOW()),
(913004,@tenant_id,910004,920004,'FREIGHT','运费',1.00,'单',185.00,185.00,NULL,NOW()),
(913005,@tenant_id,910005,920005,'FREIGHT','运费',1.00,'单',65.00,65.00,NULL,NOW()),
(913006,@tenant_id,910006,920006,'FREIGHT','运费',1.00,'单',230.00,230.00,NULL,NOW()),
(913007,@tenant_id,910007,920007,'FREIGHT','运费',1.00,'单',35.00,35.00,NULL,NOW()),
(913008,@tenant_id,910008,920008,'FREIGHT','运费',1.00,'单',275.00,275.00,NULL,NOW()),
(913009,@tenant_id,910009,920009,'FREIGHT','运费',1.00,'单',20.00,20.00,NULL,NOW()),
(913010,@tenant_id,910010,920010,'FREIGHT','运费',1.00,'单',35.00,35.00,NULL,NOW()),
(913011,@tenant_id,910011,920011,'FREIGHT','运费',1.00,'单',50.00,50.00,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(913012,@tenant_id,910012,920012,'FREIGHT','运费',1.00,'单',125.00,125.00,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(913013,@tenant_id,910013,920013,'FREIGHT','运费',1.00,'单',20.00,20.00,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY)),
(913014,@tenant_id,910014,920014,'FREIGHT','运费',1.00,'单',95.00,95.00,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(913015,@tenant_id,910015,920015,'FREIGHT','运费',1.00,'单',65.00,65.00,NULL,DATE_SUB(NOW(),INTERVAL 6 DAY)),
(913016,@tenant_id,910016,920016,'FREIGHT','运费',1.00,'单',155.00,155.00,NULL,DATE_SUB(NOW(),INTERVAL 4 DAY)),
(913017,@tenant_id,910017,920017,'FREIGHT','运费',1.00,'单',95.00,95.00,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY)),
(913018,@tenant_id,910018,920018,'FREIGHT','运费',1.00,'单',140.00,140.00,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY)),
(913019,@tenant_id,910019,920019,'FREIGHT','运费',1.00,'单',80.00,80.00,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(913020,@tenant_id,910020,920020,'FREIGHT','运费',1.00,'单',50.00,50.00,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY));

-- 7.5 支付单（一单一单；渠道 wechat/alipay/mock；0待支付1已支付2已关闭3已退款；dev mock 无真实凭证）
INSERT INTO `pd_payment_order`
(`id`,`tenant_id`,`order_id`,`pay_no`,`pay_channel`,`amount`,`status`,`prepay_params`,`channel_trade_no`,`pay_time`,`create_time`,`update_time`)
VALUES
('PAY910001',@tenant_id,910001,'PN20261012001','mock',50.00,0,NULL,NULL,NULL,TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),TIMESTAMP(CURDATE(),MAKETIME(8,40,0))),
('PAY910002',@tenant_id,910002,'PN20261012002','wechat',35.00,1,'{"prepayId":"dev-wx-002"}','WX2026101208560002',TIMESTAMP(CURDATE(),MAKETIME(8,56,0)),TIMESTAMP(CURDATE(),MAKETIME(8,55,0)),TIMESTAMP(CURDATE(),MAKETIME(8,56,0))),
('PAY910003',@tenant_id,910003,'PN20261012003','mock',44.00,1,NULL,'MOCK2026101209110003',TIMESTAMP(CURDATE(),MAKETIME(9,11,0)),TIMESTAMP(CURDATE(),MAKETIME(9,10,0)),TIMESTAMP(CURDATE(),MAKETIME(9,11,0))),
('PAY910004',@tenant_id,910004,'PN20261012004','alipay',185.00,1,'{"tradeNo":"dev-zfb-004"}','ZFB2026101209310004',TIMESTAMP(CURDATE(),MAKETIME(9,31,0)),TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),TIMESTAMP(CURDATE(),MAKETIME(9,31,0))),
('PAY910005',@tenant_id,910005,'PN20261012005','wechat',65.00,1,'{"prepayId":"dev-wx-005"}','WX2026101209510005',TIMESTAMP(CURDATE(),MAKETIME(9,51,0)),TIMESTAMP(CURDATE(),MAKETIME(9,50,0)),TIMESTAMP(CURDATE(),MAKETIME(9,51,0))),
('PAY910006',@tenant_id,910006,'PN20261012006','mock',230.00,1,NULL,'MOCK2026101210160006',TIMESTAMP(CURDATE(),MAKETIME(10,16,0)),TIMESTAMP(CURDATE(),MAKETIME(10,15,0)),TIMESTAMP(CURDATE(),MAKETIME(10,16,0))),
('PAY910007',@tenant_id,910007,'PN20261012007','alipay',35.00,1,'{"tradeNo":"dev-zfb-007"}','ZFB2026101210410007',TIMESTAMP(CURDATE(),MAKETIME(10,41,0)),TIMESTAMP(CURDATE(),MAKETIME(10,40,0)),TIMESTAMP(CURDATE(),MAKETIME(10,41,0))),
('PAY910008',@tenant_id,910008,'PN20261012008','mock',275.00,0,NULL,NULL,NULL,TIMESTAMP(CURDATE(),MAKETIME(7,30,0)),TIMESTAMP(CURDATE(),MAKETIME(7,30,0))),
('PAY910009',@tenant_id,910009,'PN20261012009','mock',20.00,1,NULL,'MOCK2026101211060009',TIMESTAMP(CURDATE(),MAKETIME(11,6,0)),TIMESTAMP(CURDATE(),MAKETIME(11,5,0)),TIMESTAMP(CURDATE(),MAKETIME(11,6,0))),
('PAY910010',@tenant_id,910010,'PN20261012010','mock',35.00,2,NULL,NULL,NULL,TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),TIMESTAMP(CURDATE(),MAKETIME(8,35,0))),
('PAY910011',@tenant_id,910011,'PN20261010011','wechat',50.00,1,'{"prepayId":"dev-wx-011"}','WX2026101009010011',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY)),
('PAY910012',@tenant_id,910012,'PN20261010012','mock',125.00,0,NULL,NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY)),
('PAY910013',@tenant_id,910013,'PN20261007013','mock',20.00,1,NULL,'MOCK202610070013',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY)),
('PAY910014',@tenant_id,910014,'PN20261011014','alipay',95.00,1,'{"tradeNo":"dev-zfb-014"}','ZFB2026101112010014',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
('PAY910015',@tenant_id,910015,'PN20261006015','wechat',65.00,1,'{"prepayId":"dev-wx-015"}','WX202610060015',DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY)),
('PAY910016',@tenant_id,910016,'PN20261008016','mock',155.00,1,NULL,'MOCK202610080016',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY)),
('PAY910017',@tenant_id,910017,'PN20261009017','mock',95.00,1,NULL,'MOCK202610090017',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY)),
('PAY910018',@tenant_id,910018,'PN20261009018','wechat',140.00,1,'{"prepayId":"dev-wx-018"}','WX202610090018',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY)),
('PAY910019',@tenant_id,910019,'PN20261011019','alipay',80.00,3,'{"tradeNo":"dev-zfb-019"}','ZFB202610110019',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
('PAY910020',@tenant_id,910020,'PN20261007020','mock',50.00,1,NULL,'MOCK202610070020',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY));

-- 7.6 运单（下单预生成，随订单推进更新 status/scheduling_status）
INSERT INTO `work_transport_order`
(`id`,`tenant_id`,`order_id`,`status`,`scheduling_status`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
(920001,@tenant_id,910001,1,1,NULL,NOW(),NULL,NOW(),0),
(920002,@tenant_id,910002,1,1,NULL,NOW(),NULL,NOW(),0),
(920003,@tenant_id,910003,1,1,NULL,NOW(),NULL,NOW(),0),
(920004,@tenant_id,910004,1,1,NULL,NOW(),NULL,NOW(),0),
(920005,@tenant_id,910005,1,1,NULL,NOW(),NULL,NOW(),0),
(920006,@tenant_id,910006,2,3,NULL,NOW(),NULL,NOW(),0),
(920007,@tenant_id,910007,2,3,NULL,NOW(),NULL,NOW(),0),
(920008,@tenant_id,910008,3,3,NULL,NOW(),NULL,NOW(),0),
(920009,@tenant_id,910009,4,3,NULL,NOW(),NULL,NOW(),0),
(920010,@tenant_id,910010,1,1,NULL,NOW(),NULL,NOW(),0),
(920011,@tenant_id,910011,4,3,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 HOUR),0),
(920012,@tenant_id,910012,4,3,NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 HOUR),0),
(920013,@tenant_id,910013,5,3,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(920014,@tenant_id,910014,5,3,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(920015,@tenant_id,910015,5,3,NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(920016,@tenant_id,910016,5,3,NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(920017,@tenant_id,910017,5,3,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(920018,@tenant_id,910018,5,3,NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(920019,@tenant_id,910019,6,3,NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(920020,@tenant_id,910020,5,3,NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0);

-- 7.7 运单-运输任务关联（仅挂了干线/接驳段的 7 单）
-- 任务起讫严格等于运单寄/收城市一级枢纽；仅挂"已实际进入干线段"的 7 单：
--   920006 沪蓉在途 / 920007 穗陕在途 / 920008 汉蓉已到 / 920011 京穗已到
--   920012 沪汉已到 / 920014 京蓉签收 / 920020 穗京签收
INSERT INTO `work_transport_order_task` (`id`,`tenant_id`,`transport_order_id`,`task_id`) VALUES
(940001,@tenant_id,920006,9701),(940002,@tenant_id,920007,9702),(940003,@tenant_id,920008,9703),
(940004,@tenant_id,920011,9704),(940005,@tenant_id,920012,9705),(940006,@tenant_id,920014,9706),
(940007,@tenant_id,920020,9707);

-- 7.8 补建 5 条业务需要的城际干线（321~325，线型 100）+ 对应天班车次（421~425）
--   仅补线路/车次；长途车在途可跨城，实际运输任务在 §7.9 直接指派 truck/driver。
DELETE FROM `base_transport_trips` WHERE tenant_id=@tenant_id AND id BETWEEN 421 AND 425;
DELETE FROM `base_transport_line`  WHERE tenant_id=@tenant_id AND id BETWEEN 321 AND 325;
INSERT INTO `base_transport_line`
(`id`,`tenant_id`,`name`,`line_number`,`org_id`,`transport_line_type_id`,`start_org_id`,`end_org_id`,`distance`,`cost`,`estimated_time`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(321,@tenant_id,'穗陕干线','LINE-GZ-XA',100,100,203,206,1720.00,8600.00,1380,1,1000,NOW(),NOW(),0),
(322,@tenant_id,'沪汉干线','LINE-SH-WH',100,100,202,205,830.00,4150.00,620,1,1000,NOW(),NOW(),0),
(323,@tenant_id,'汉蓉干线','LINE-WH-CD',100,100,205,204,1200.00,6000.00,900,1,1000,NOW(),NOW(),0),
(324,@tenant_id,'京穗干线','LINE-BJ-GZ',100,100,201,203,2120.00,10600.00,1600,1,1000,NOW(),NOW(),0),
(325,@tenant_id,'穗京干线','LINE-GZ-BJ',100,100,203,201,2120.00,10600.00,1600,1,1000,NOW(),NOW(),0);
INSERT INTO `base_transport_trips`
(`id`,`tenant_id`,`name`,`departure_time`,`transport_line_id`,`period`,`status`,`create_by`,`create_time`,`update_time`,`deleted`)
VALUES
(421,@tenant_id,'穗陕夜班','20:00',321,1,1,1000,NOW(),NOW(),0),
(422,@tenant_id,'沪汉午班','12:30',322,1,1,1000,NOW(),NOW(),0),
(423,@tenant_id,'汉蓉晚班','18:00',323,1,1,1000,NOW(),NOW(),0),
(424,@tenant_id,'京穗白班','08:00',324,1,1,1000,NOW(),NOW(),0),
(425,@tenant_id,'穗京夜班','19:00',325,1,1,1000,NOW(),NOW(),0);

-- 7.9 干线运输任务 9701~9710（五态：在途2/完成4/待执行/改派/作废；truck_id 可空=尚未派车）
DELETE FROM `work_task_transport` WHERE tenant_id=@tenant_id AND id BETWEEN 9701 AND 9710;
INSERT INTO `work_task_transport`
(`id`,`tenant_id`,`trips_id`,`start_org_id`,`end_org_id`,`status`,`assigned_status`,`loading_status`,`truck_id`,
 `pickup_picture`,`cargo_picture`,`certificate_picture`,`deliver_picture`,
 `pickup_longitude`,`pickup_latitude`,`deliver_longitude`,`deliver_latitude`,
 `plan_departure_time`,`actual_departure_time`,`plan_arrival_time`,`actual_arrival_time`,
 `plan_pick_up_time`,`actual_pick_up_time`,`plan_delivery_time`,`actual_delivery_time`,
 `create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
-- 9701 沪蓉在途（车521/司机9114；GPS 轨迹主对象）
(9701,@tenant_id,411,202,204,2,2,3,521,'/static/demo/tt/9701-pickup.jpg','/static/demo/tt/9701-cargo.jpg',NULL,NULL,
 121.223000,31.173000,104.157000,30.824000,
 TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),TIMESTAMP(CURDATE(),MAKETIME(9,35,0)),DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 1 DAY),NULL,
 TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),TIMESTAMP(CURDATE(),MAKETIME(9,20,0)),DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(10,0,0)),INTERVAL 1 DAY),NULL,
 1000,NOW(),NULL,NOW(),0),
-- 9702 穗陕在途（车523/司机9115；GPS 轨迹主对象，昨夜发预计今夜到）
(9702,@tenant_id,421,203,206,2,2,2,523,'/static/demo/tt/9702-pickup.jpg','/static/demo/tt/9702-cargo.jpg',NULL,NULL,
 113.273000,23.217000,109.063000,34.367000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(20,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(20,10,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(20,0,0)),NULL,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,40,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(21,0,0)),NULL,
 1000,NOW(),NULL,NOW(),0),
-- 9703 汉蓉完成（车522/司机9117，今晨到成都新都）
(9703,@tenant_id,423,205,204,4,2,3,522,'/static/demo/tt/9703-pickup.jpg','/static/demo/tt/9703-cargo.jpg','/static/demo/tt/9703-cert.jpg','/static/demo/tt/9703-deliver.jpg',
 114.142000,30.621000,104.157000,30.824000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,5,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),TIMESTAMP(CURDATE(),MAKETIME(6,10,0)),
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,30,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),TIMESTAMP(CURDATE(),MAKETIME(7,40,0)),
 1000,NOW(),NULL,NOW(),0),
-- 9704 京穗完成（车524/司机9111，货已交广州白云，末端待派送→运单920011）
(9704,@tenant_id,424,201,203,4,2,3,524,'/static/demo/tt/9704-pickup.jpg','/static/demo/tt/9704-cargo.jpg','/static/demo/tt/9704-cert.jpg','/static/demo/tt/9704-deliver.jpg',
 116.403000,39.915000,113.273000,23.217000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,10,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,40,0)),INTERVAL 1 DAY),
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,20,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,30,0)),INTERVAL 1 DAY),
 1000,NOW(),NULL,NOW(),0),
-- 9705 沪汉完成（车521/司机9113，货交武汉东西湖，末端派送中→920012）
(9705,@tenant_id,422,202,205,4,2,2,521,'/static/demo/tt/9705-pickup.jpg','/static/demo/tt/9705-cargo.jpg','/static/demo/tt/9705-cert.jpg','/static/demo/tt/9705-deliver.jpg',
 121.223000,31.173000,114.142000,30.621000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,30,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,40,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(23,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(22,20,0)),INTERVAL 1 DAY),
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(11,30,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,0,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(23,30,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(22,50,0)),INTERVAL 1 DAY),
 1000,NOW(),NULL,NOW(),0),
-- 9706 京蓉完成（车523/司机9112，昨日签收→920014）
(9706,@tenant_id,413,201,204,4,2,3,523,'/static/demo/tt/9706-pickup.jpg','/static/demo/tt/9706-cargo.jpg','/static/demo/tt/9706-cert.jpg','/static/demo/tt/9706-deliver.jpg',
 116.403000,39.915000,104.157000,30.824000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,30,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(6,40,0)),INTERVAL 1 DAY),
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,20,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,40,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),INTERVAL 1 DAY),
 1000,NOW(),NULL,NOW(),0),
-- 9707 穗京完成（车524/司机9115，5 天前签收→920020）
(9707,@tenant_id,425,203,201,4,2,2,524,'/static/demo/tt/9707-pickup.jpg','/static/demo/tt/9707-cargo.jpg','/static/demo/tt/9707-cert.jpg','/static/demo/tt/9707-deliver.jpg',
 113.273000,23.217000,116.403000,39.915000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,0,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,10,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 5 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,30,0)),INTERVAL 5 DAY),
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,0,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,30,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,0,0)),INTERVAL 5 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,20,0)),INTERVAL 5 DAY),
 1000,NOW(),NULL,NOW(),0),
-- 9708 沪蓉待执行（明日班，车未派 truck_id=NULL，司机预排 9114）
(9708,@tenant_id,411,202,204,1,1,1,NULL,NULL,NULL,NULL,NULL,
 121.223000,31.173000,104.157000,30.824000,
 DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),INTERVAL 1 DAY),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 2 DAY),NULL,
 DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 1 DAY),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(10,0,0)),INTERVAL 2 DAY),NULL,
 1000,NOW(),NULL,NOW(),0),
-- 9709 沪穗待发但改派（车暂挂522，原司机9113改派、改派给9111，待人工重新分配）
(9709,@tenant_id,412,202,203,1,3,1,522,NULL,NULL,NULL,NULL,
 121.223000,31.173000,113.273000,23.217000,
 TIMESTAMP(CURDATE(),MAKETIME(13,30,0)),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(6,0,0)),INTERVAL 1 DAY),NULL,
 TIMESTAMP(CURDATE(),MAKETIME(12,0,0)),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 1 DAY),NULL,
 1000,NOW(),NULL,NOW(),0),
-- 9710 京穗作废（昨日班，因货源不足取消，未派车）
(9710,@tenant_id,424,201,203,5,1,1,NULL,NULL,NULL,NULL,NULL,
 116.403000,39.915000,113.273000,23.217000,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 1 DAY),NULL,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 1 DAY),NULL,
 DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),INTERVAL 1 DAY),NULL,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,0,0)),INTERVAL 1 DAY),NULL,
 1000,NOW(),NULL,NOW(),0);

-- 7.10 司机作业单 9801~9811（driver_id=sys_user.id；改派产生两条作业，作废一条）
DELETE FROM `work_driver_job` WHERE tenant_id=@tenant_id AND id BETWEEN 9801 AND 9811;
INSERT INTO `work_driver_job`
(`id`,`tenant_id`,`driver_id`,`task_transport_id`,`start_org_id`,`end_org_id`,`status`,`start_handover`,`finish_handover`,`plan_departure_time`,`actual_departure_time`,`plan_arrival_time`,`actual_arrival_time`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
(9801,@tenant_id,9114,9701,202,204,2,'上海青浦中转场 王仓管 021-58000011','成都新都中转场（待交接）',TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),TIMESTAMP(CURDATE(),MAKETIME(9,35,0)),DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 1 DAY),NULL,1000,NOW(),NULL,NOW(),0),
(9802,@tenant_id,9115,9702,203,206,2,'广州白云中转场 李仓管 020-86000011','西安灞桥中转场（待交接）',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(20,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(20,10,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(20,0,0)),NULL,1000,NOW(),NULL,NOW(),0),
(9803,@tenant_id,9117,9703,205,204,4,'武汉径河中转场 周仓管 027-83000011','成都新都中转场 张仓管 028-85000011',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,5,0)),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),TIMESTAMP(CURDATE(),MAKETIME(6,10,0)),1000,NOW(),NULL,NOW(),0),
(9804,@tenant_id,9111,9704,201,203,4,'北京南城中转场 赵仓管 010-88000011','广州白云中转场 李仓管 020-86000011',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,10,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,40,0)),INTERVAL 1 DAY),1000,NOW(),NULL,NOW(),0),
(9805,@tenant_id,9113,9705,202,205,4,'上海青浦中转场 王仓管 021-58000011','武汉径河中转场 周仓管 027-83000011',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,30,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,40,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(23,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(22,20,0)),INTERVAL 1 DAY),1000,NOW(),NULL,NOW(),0),
(9806,@tenant_id,9112,9706,201,204,4,'北京南城中转场 赵仓管 010-88000011','成都新都中转场 张仓管 028-85000011',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,30,0)),INTERVAL 2 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(7,0,0)),INTERVAL 1 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(6,40,0)),INTERVAL 1 DAY),1000,NOW(),NULL,NOW(),0),
(9807,@tenant_id,9115,9707,203,201,4,'广州白云中转场 李仓管 020-86000011','北京南城中转场 赵仓管 010-88000011',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,0,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,10,0)),INTERVAL 6 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 5 DAY),DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,30,0)),INTERVAL 5 DAY),1000,NOW(),NULL,NOW(),0),
(9808,@tenant_id,9114,9708,202,204,1,'上海青浦中转场 王仓管 021-58000011','成都新都中转场（待交接）',DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),INTERVAL 1 DAY),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 2 DAY),NULL,1000,NOW(),NULL,NOW(),0),
(9809,@tenant_id,9113,9709,202,203,3,'上海青浦中转场 王仓管 021-58000011','广州白云中转场（待交接）',TIMESTAMP(CURDATE(),MAKETIME(13,30,0)),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(6,0,0)),INTERVAL 1 DAY),NULL,1000,NOW(),NULL,NOW(),0),
(9810,@tenant_id,9111,9709,202,203,1,'上海青浦中转场 王仓管 021-58000011','广州白云中转场（待交接）',TIMESTAMP(CURDATE(),MAKETIME(13,30,0)),NULL,DATE_ADD(TIMESTAMP(CURDATE(),MAKETIME(6,0,0)),INTERVAL 1 DAY),NULL,1000,NOW(),NULL,NOW(),0),
(9811,@tenant_id,9116,9710,201,203,5,'北京南城中转场 赵仓管 010-88000011','广州白云中转场（待交接）',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),INTERVAL 1 DAY),NULL,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(16,0,0)),INTERVAL 1 DAY),NULL,1000,NOW(),NULL,NOW(),0);

-- 7.11 末端取派任务 950001~950030（19 取件 + 11 派件；按订单推进决定有无/状态）
--   快递员：9211 北京 / 9212 上海 / 9213 广州 / 9201·9202 武汉；成都等无账号城市待派或外协
DELETE FROM `work_pickup_dispatch_task` WHERE tenant_id=@tenant_id AND id BETWEEN 950001 AND 950030;
INSERT INTO `work_pickup_dispatch_task`
(`id`,`tenant_id`,`order_id`,`task_type`,`status`,`sign_status`,`org_id`,`courier_id`,`assigned_status`,`estimated_start_time`,`actual_start_time`,`estimated_end_time`,`actual_end_time`,`confirm_time`,`cancel_time`,`mark`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
-- ===== 取件 task_type=1 =====
(950001,@tenant_id,910001,1,1,NULL,1201,9211,2,TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),NULL,TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),NULL,NULL,NULL,'东城上门取件',1000,TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),NULL,NOW(),0),
(950002,@tenant_id,910002,1,4,NULL,1202,9211,2,TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),TIMESTAMP(CURDATE(),MAKETIME(9,5,0)),TIMESTAMP(CURDATE(),MAKETIME(9,40,0)),TIMESTAMP(CURDATE(),MAKETIME(9,25,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(8,55,0)),NULL,NOW(),0),
(950003,@tenant_id,910004,1,4,NULL,1206,9212,2,TIMESTAMP(CURDATE(),MAKETIME(9,40,0)),TIMESTAMP(CURDATE(),MAKETIME(9,45,0)),TIMESTAMP(CURDATE(),MAKETIME(10,20,0)),TIMESTAMP(CURDATE(),MAKETIME(10,10,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(9,30,0)),NULL,NOW(),0),
(950004,@tenant_id,910005,1,4,NULL,1205,9212,2,TIMESTAMP(CURDATE(),MAKETIME(10,0,0)),TIMESTAMP(CURDATE(),MAKETIME(10,2,0)),TIMESTAMP(CURDATE(),MAKETIME(10,40,0)),TIMESTAMP(CURDATE(),MAKETIME(10,35,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(9,50,0)),NULL,NOW(),0),
(950005,@tenant_id,910006,1,4,NULL,1206,9212,2,TIMESTAMP(CURDATE(),MAKETIME(10,25,0)),TIMESTAMP(CURDATE(),MAKETIME(10,30,0)),TIMESTAMP(CURDATE(),MAKETIME(11,5,0)),TIMESTAMP(CURDATE(),MAKETIME(11,0,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(10,15,0)),NULL,NOW(),0),
(950006,@tenant_id,910007,1,4,NULL,1207,9213,2,TIMESTAMP(CURDATE(),MAKETIME(10,50,0)),TIMESTAMP(CURDATE(),MAKETIME(10,55,0)),TIMESTAMP(CURDATE(),MAKETIME(11,30,0)),TIMESTAMP(CURDATE(),MAKETIME(11,20,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(10,40,0)),NULL,NOW(),0),
(950007,@tenant_id,910008,1,4,NULL,1215,9201,2,TIMESTAMP(CURDATE(),MAKETIME(8,0,0)),TIMESTAMP(CURDATE(),MAKETIME(8,5,0)),TIMESTAMP(CURDATE(),MAKETIME(8,45,0)),TIMESTAMP(CURDATE(),MAKETIME(8,40,0)),NULL,NULL,NULL,1000,TIMESTAMP(CURDATE(),MAKETIME(7,30,0)),NULL,NOW(),0),
(950008,@tenant_id,910009,1,4,NULL,1203,9211,2,TIMESTAMP(CURDATE(),MAKETIME(11,15,0)),TIMESTAMP(CURDATE(),MAKETIME(11,18,0)),TIMESTAMP(CURDATE(),MAKETIME(11,45,0)),TIMESTAMP(CURDATE(),MAKETIME(11,40,0)),NULL,NULL,'同城急件取件',1000,TIMESTAMP(CURDATE(),MAKETIME(11,5,0)),NULL,NOW(),0),
(950009,@tenant_id,910010,1,5,NULL,1201,NULL,1,TIMESTAMP(CURDATE(),MAKETIME(8,50,0)),NULL,TIMESTAMP(CURDATE(),MAKETIME(9,20,0)),NULL,NULL,TIMESTAMP(CURDATE(),MAKETIME(8,35,0)),'客户临时改期取消',1000,TIMESTAMP(CURDATE(),MAKETIME(8,20,0)),NULL,TIMESTAMP(CURDATE(),MAKETIME(8,35,0)),0),
(950010,@tenant_id,910011,1,4,NULL,1202,9211,2,DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(950011,@tenant_id,910012,1,4,NULL,1206,9212,2,DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,DATE_SUB(NOW(),INTERVAL 2 DAY),0),
(950012,@tenant_id,910013,1,4,NULL,1201,9211,2,DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(950013,@tenant_id,910014,1,4,NULL,1203,9211,2,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(950014,@tenant_id,910015,1,4,NULL,1205,9212,2,DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(950015,@tenant_id,910016,1,4,NULL,1213,9201,2,DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(950016,@tenant_id,910017,1,4,NULL,1208,9213,2,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(950017,@tenant_id,910018,1,4,NULL,1202,9211,2,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(950018,@tenant_id,910019,1,4,NULL,1205,9212,2,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(950019,@tenant_id,910020,1,4,NULL,1207,9213,2,DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,NULL,NULL,1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
-- ===== 派件 task_type=2 =====
(950020,@tenant_id,910009,2,2,NULL,1202,9211,2,TIMESTAMP(CURDATE(),MAKETIME(13,0,0)),TIMESTAMP(CURDATE(),MAKETIME(13,10,0)),TIMESTAMP(CURDATE(),MAKETIME(15,0,0)),NULL,NULL,NULL,'同城当日达派送中',1000,TIMESTAMP(CURDATE(),MAKETIME(11,45,0)),NULL,NOW(),0),
(950021,@tenant_id,910011,2,1,NULL,1208,NULL,1,DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,DATE_ADD(NOW(),INTERVAL 3 HOUR),NULL,NULL,NULL,'干线已到天河，等待派工',1000,DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,NOW(),0),
(950022,@tenant_id,910012,2,2,NULL,1213,9201,2,DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,NULL,NULL,'江岸段派送中',1000,DATE_SUB(NOW(),INTERVAL 3 HOUR),NULL,NOW(),0),
(950023,@tenant_id,910013,2,4,1,1204,9212,2,DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,'本人签收',1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0),
(950024,@tenant_id,910014,2,4,1,1210,NULL,2,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,'成都片区外协配送员代收',1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,DATE_SUB(NOW(),INTERVAL 1 DAY),0),
(950025,@tenant_id,910015,2,4,1,1201,9211,2,DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,'前台签收',1000,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,DATE_SUB(NOW(),INTERVAL 6 DAY),0),
(950026,@tenant_id,910016,2,4,1,1206,9212,2,DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,'本人签收',1000,DATE_SUB(NOW(),INTERVAL 4 DAY),NULL,DATE_SUB(NOW(),INTERVAL 4 DAY),0),
(950027,@tenant_id,910017,2,4,1,1214,9202,2,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,'本人签收',1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(950028,@tenant_id,910018,2,4,1,1215,9201,2,DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,'物业代收',1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,DATE_SUB(NOW(),INTERVAL 3 DAY),0),
(950029,@tenant_id,910019,2,3,2,1209,9213,2,DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NULL,'收件人拒收，外包装破损，待客服介入退回',1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NOW(),0),
(950030,@tenant_id,910020,2,4,1,1203,9211,2,DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,'本人签收',1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,DATE_SUB(NOW(),INTERVAL 5 DAY),0);

-- 7.12 运输任务状态流转历史 990001~990018（business_type=1；后台排车=1，司机发车/到达=2）
DELETE FROM `work_status_transition_history` WHERE tenant_id=@tenant_id AND id BETWEEN 990001 AND 990030;
INSERT INTO `work_status_transition_history`
(`id`,`tenant_id`,`business_type`,`business_id`,`business_no`,`operation_type`,`before_status`,`after_status`,`operator_id`,`operator_name`,`operator_type`,`remark`,`operate_time`,`create_time`)
VALUES
(990001,@tenant_id,1,9701,'YS9701',1,NULL,1,1000,'系统调度',1,'沪蓉早班排车，指派 川A7F208/陈志远',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(9,0,0)),INTERVAL 12 HOUR),NOW()),
(990002,@tenant_id,1,9701,'YS9701',1,1,2,9114,'陈志远',2,'上海青浦发车',TIMESTAMP(CURDATE(),MAKETIME(9,35,0)),NOW()),
(990003,@tenant_id,1,9702,'YS9702',1,NULL,1,1000,'系统调度',1,'穗陕夜班排车，指派 陕A6G128/刘志强',DATE_SUB(NOW(),INTERVAL 1 DAY),NOW()),
(990004,@tenant_id,1,9702,'YS9702',1,1,2,9115,'刘志强',2,'广州白云发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(20,10,0)),INTERVAL 1 DAY),NOW()),
(990005,@tenant_id,1,9703,'YS9703',1,NULL,1,1000,'系统调度',1,'汉蓉晚班排车，指派 鄂A9K516/孙大鹏',DATE_SUB(NOW(),INTERVAL 2 DAY),NOW()),
(990006,@tenant_id,1,9703,'YS9703',1,1,2,9117,'孙大鹏',2,'武汉径河发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,5,0)),INTERVAL 1 DAY),NOW()),
(990007,@tenant_id,1,9703,'YS9703',1,2,4,9117,'孙大鹏',2,'到达成都新都并完成交接',TIMESTAMP(CURDATE(),MAKETIME(6,10,0)),NOW()),
(990008,@tenant_id,1,9704,'YS9704',1,NULL,1,1000,'系统调度',1,'京穗白班排车，指派 辽A8H336/李建国',DATE_SUB(NOW(),INTERVAL 3 DAY),NOW()),
(990009,@tenant_id,1,9704,'YS9704',1,1,2,9111,'李建国',2,'北京南城发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,10,0)),INTERVAL 2 DAY),NOW()),
(990010,@tenant_id,1,9704,'YS9704',1,2,4,9111,'李建国',2,'到达广州白云交接',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,40,0)),INTERVAL 1 DAY),NOW()),
(990011,@tenant_id,1,9705,'YS9705',1,NULL,1,1000,'系统调度',1,'沪汉午班排车，指派 川A7F208/张伟明',DATE_SUB(NOW(),INTERVAL 3 DAY),NOW()),
(990012,@tenant_id,1,9705,'YS9705',1,1,2,9113,'张伟明',2,'上海青浦发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,40,0)),INTERVAL 2 DAY),NOW()),
(990013,@tenant_id,1,9705,'YS9705',1,2,4,9113,'张伟明',2,'到达武汉径河交接',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(22,20,0)),INTERVAL 1 DAY),NOW()),
(990014,@tenant_id,1,9706,'YS9706',1,NULL,1,1000,'系统调度',1,'京蓉早班排车，指派 陕A6G128/王海涛',DATE_SUB(NOW(),INTERVAL 3 DAY),NOW()),
(990015,@tenant_id,1,9706,'YS9706',1,1,2,9112,'王海涛',2,'北京南城发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,30,0)),INTERVAL 2 DAY),NOW()),
(990016,@tenant_id,1,9706,'YS9706',1,2,4,9112,'王海涛',2,'到达成都新都交接',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(6,40,0)),INTERVAL 1 DAY),NOW()),
(990017,@tenant_id,1,9707,'YS9707',1,NULL,1,1000,'系统调度',1,'穗京夜班排车，指派 辽A8H336/刘志强',DATE_SUB(NOW(),INTERVAL 7 DAY),NOW()),
(990018,@tenant_id,1,9707,'YS9707',1,1,2,9115,'刘志强',2,'广州白云发车',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(19,10,0)),INTERVAL 6 DAY),NOW()),
(990019,@tenant_id,1,9707,'YS9707',1,2,4,9115,'刘志强',2,'到达北京南城交接',DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,30,0)),INTERVAL 5 DAY),NOW()),
(990020,@tenant_id,1,9709,'YS9709',1,1,1,1000,'调度员',1,'原司机张伟明临时换班，作业改派李建国，待人工确认',TIMESTAMP(CURDATE(),MAKETIME(12,0,0)),NOW());

-- 7.13 结算单 960001~960017（10 应收[对客户] + 7 应付[对司机]；0待对账/1已对账/2已结算）
DELETE FROM `oms_settlement_order` WHERE tenant_id=@tenant_id AND id BETWEEN 960001 AND 960017;
INSERT INTO `oms_settlement_order`
(`id`,`tenant_id`,`settlement_no`,`order_id`,`transport_order_id`,`settle_object_type`,`settle_object_id`,`direction`,`period_start`,`period_end`,`order_count`,`receivable_amount`,`payable_amount`,`settled_amount`,`status`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
-- 应收（对客户/会员）
(960001,@tenant_id,'STL960001',910002,920002,1,'1502',1,NULL,NULL,1,35.00,0.00,0.00,0,'在途，待对账',1000,NOW(),NULL,NOW(),0),
(960002,@tenant_id,'STL960002',910004,920004,1,'1504',1,NULL,NULL,1,185.00,0.00,0.00,0,'在途，待对账',1000,NOW(),NULL,NOW(),0),
(960003,@tenant_id,'STL960003',910006,920006,1,'1508',1,NULL,NULL,1,230.00,0.00,0.00,0,'在途，待对账',1000,NOW(),NULL,NOW(),0),
(960004,@tenant_id,'STL960004',910012,920012,1,'1504',1,NULL,NULL,1,125.00,0.00,0.00,0,'派送中，待对账',1000,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,NOW(),0),
(960005,@tenant_id,'STL960005',910013,920013,1,'1501',1,NULL,NULL,1,20.00,0.00,0.00,1,'已对账',1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,NOW(),0),
(960006,@tenant_id,'STL960006',910015,920015,1,'1503',1,NULL,NULL,1,65.00,0.00,0.00,1,'已对账',1000,DATE_SUB(NOW(),INTERVAL 6 DAY),NULL,NOW(),0),
(960007,@tenant_id,'STL960007',910017,920017,1,'1506',1,NULL,NULL,1,95.00,0.00,0.00,1,'已对账',1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,NOW(),0),
(960008,@tenant_id,'STL960008',910014,920014,1,'1507',1,DATE_SUB(CURDATE(),INTERVAL 1 MONTH),CURDATE(),1,95.00,0.00,95.00,2,'账期内已结清',1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NOW(),0),
(960009,@tenant_id,'STL960009',910018,920018,1,'1502',1,DATE_SUB(CURDATE(),INTERVAL 1 MONTH),CURDATE(),1,140.00,0.00,140.00,2,'账期内已结清',1000,DATE_SUB(NOW(),INTERVAL 3 DAY),NULL,NOW(),0),
(960010,@tenant_id,'STL960010',910020,920020,1,'1505',1,DATE_SUB(CURDATE(),INTERVAL 1 MONTH),CURDATE(),1,50.00,0.00,50.00,2,'账期内已结清',1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,NOW(),0),
-- 应付（对司机；按干线成本口径估算，约运费 65%~75%）
(960011,@tenant_id,'STL960011',910006,920006,2,'9114',2,NULL,NULL,1,0.00,150.00,0.00,0,'沪蓉在途，待对账',1000,NOW(),NULL,NOW(),0),
(960012,@tenant_id,'STL960012',910007,920007,2,'9115',2,NULL,NULL,1,0.00,24.00,0.00,0,'穗陕在途，待对账',1000,NOW(),NULL,NOW(),0),
(960013,@tenant_id,'STL960013',910008,920008,2,'9117',2,NULL,NULL,1,0.00,190.00,0.00,1,'汉蓉已到，已对账',1000,NOW(),NULL,NOW(),0),
(960014,@tenant_id,'STL960014',910011,920011,2,'9111',2,NULL,NULL,1,0.00,35.00,0.00,1,'京穗已到，已对账',1000,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,NOW(),0),
(960015,@tenant_id,'STL960015',910012,920012,2,'9113',2,NULL,NULL,1,0.00,85.00,0.00,1,'沪汉已到，已对账',1000,DATE_SUB(NOW(),INTERVAL 2 DAY),NULL,NOW(),0),
(960016,@tenant_id,'STL960016',910014,920014,2,'9112',2,DATE_SUB(CURDATE(),INTERVAL 1 MONTH),CURDATE(),1,0.00,65.00,65.00,2,'账期内已结司机运费',1000,DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,NOW(),0),
(960017,@tenant_id,'STL960017',910020,920020,2,'9115',2,DATE_SUB(CURDATE(),INTERVAL 1 MONTH),CURDATE(),1,0.00,35.00,35.00,2,'账期内已结司机运费',1000,DATE_SUB(NOW(),INTERVAL 5 DAY),NULL,NOW(),0);

-- =====================================================================
-- §8 调度域 disp_*（9 张表；现网全空。围绕 §7 已挂干线的 7 票货还原
--    “定时分类 → 算路缓存 → 派配车次/车/司机”的调度留痕）
--    分类串真值 = OrderClassifyDTO.groupBy() = startOrgId#endOrgId#currentOrgId
--    （起讫/当前均用城市一级枢纽 201~207，与干线算路、车次起讫口径一致）
--    执行器 bean_name='dispatchTask'（@XxlJob("dispatchTask")）；
--    params 取 ScheduleParams 四纯串：distance / cost / estimatedTime / transfer。
-- =====================================================================

-- 8.1 定时调度任务（七个城市一级枢纽各一条；成都 74 暂停；Quartz 6 位 cron）
DELETE FROM `disp_schedule_job` WHERE tenant_id=@tenant_id AND id BETWEEN 71 AND 77;
INSERT INTO `disp_schedule_job`
(`id`,`tenant_id`,`business_id`,`bean_name`,`params`,`cron_expression`,`status`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`,`deleted`)
VALUES
(71,@tenant_id,201,'dispatchTask','distance',     '0 30 6 * * ?',1,'北京枢纽-出港订单自动分类调度（按最短里程）',1000,NOW(),NULL,NOW(),0),
(72,@tenant_id,202,'dispatchTask','cost',         '0 0 7 * * ?',1,'上海枢纽-出港订单自动分类调度（按最低成本）',1000,NOW(),NULL,NOW(),0),
(73,@tenant_id,203,'dispatchTask','estimatedTime','0 30 7 * * ?',1,'广州枢纽-出港订单自动分类调度（按最短时效）',1000,NOW(),NULL,NOW(),0),
(74,@tenant_id,204,'dispatchTask','transfer',     '0 0 8 * * ?',0,'成都枢纽-出港调度（出港量小，暂人工触发，暂停）',1000,NOW(),NULL,NOW(),0),
(75,@tenant_id,205,'dispatchTask','distance',     '0 30 6 * * ?',1,'武汉枢纽-出港订单自动分类调度（按最短里程）',1000,NOW(),NULL,NOW(),0),
(76,@tenant_id,206,'dispatchTask','cost',         '0 0 8 * * ?',1,'西安枢纽-出港订单自动分类调度（按最低成本）',1000,NOW(),NULL,NOW(),0),
(77,@tenant_id,207,'dispatchTask','estimatedTime','0 30 7 * * ?',1,'沈阳枢纽-出港订单自动分类调度（按最短时效）',1000,NOW(),NULL,NOW(),0);

-- 8.2 调度执行日志（12 条：11 成功 + 1 失败；每条挂所属 job）
DELETE FROM `disp_schedule_job_log` WHERE tenant_id=@tenant_id AND id BETWEEN 801 AND 812;
INSERT INTO `disp_schedule_job_log`
(`id`,`tenant_id`,`job_id`,`bean_name`,`params`,`status`,`error`,`times`,`create_time`)
VALUES
-- 今日三趟在途/已到批次
(801,@tenant_id,72,'dispatchTask','cost',         1,NULL,1820,TIMESTAMP(CURDATE(),MAKETIME(7,2,0))),
(802,@tenant_id,73,'dispatchTask','estimatedTime',1,NULL,1650,TIMESTAMP(CURDATE(),MAKETIME(7,33,0))),
(803,@tenant_id,75,'dispatchTask','distance',     1,NULL,980, TIMESTAMP(CURDATE(),MAKETIME(6,31,0))),
-- 历史批次（对应已签收/在途的 4 票）
(804,@tenant_id,71,'dispatchTask','distance',     1,NULL,2010,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(805,@tenant_id,72,'dispatchTask','cost',         1,NULL,1760,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(806,@tenant_id,71,'dispatchTask','distance',     1,NULL,1880,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(807,@tenant_id,73,'dispatchTask','estimatedTime',1,NULL,1720,DATE_SUB(NOW(),INTERVAL 5 DAY)),
-- 例行空跑/其他批次
(808,@tenant_id,72,'dispatchTask','cost',         1,NULL,640, DATE_SUB(NOW(),INTERVAL 1 DAY)),
(809,@tenant_id,71,'dispatchTask','distance',     1,NULL,720, DATE_SUB(NOW(),INTERVAL 3 DAY)),
(810,@tenant_id,73,'dispatchTask','estimatedTime',1,NULL,810, DATE_SUB(NOW(),INTERVAL 3 DAY)),
(811,@tenant_id,75,'dispatchTask','distance',     1,NULL,560, DATE_SUB(NOW(),INTERVAL 4 DAY)),
-- 一条失败：西安出港无可用运力，算路后无匹配车次
(812,@tenant_id,76,'dispatchTask','cost',         0,'分类完成但无可用车次/车辆资源，调度中断：No available trips for classify 206#203#206',0,
   TIMESTAMP(CURDATE(),MAKETIME(8,1,0)));

-- 8.3 订单分类（7 组，各 1 单；classify=起#讫#当前；current=发运枢纽）
DELETE FROM `disp_order_classify` WHERE tenant_id=@tenant_id AND id BETWEEN 701 AND 707;
INSERT INTO `disp_order_classify`
(`id`,`tenant_id`,`job_id`,`job_log_id`,`start_org_id`,`end_org_id`,`classify`,`total`,`create_time`)
VALUES
(701,@tenant_id,72,801,202,204,'202#204#202',1,TIMESTAMP(CURDATE(),MAKETIME(7,2,0))),  -- 沪蓉 在途 910006
(702,@tenant_id,73,802,203,206,'203#206#203',1,TIMESTAMP(CURDATE(),MAKETIME(7,33,0))), -- 穗陕 在途 910007
(703,@tenant_id,75,803,205,204,'205#204#205',1,TIMESTAMP(CURDATE(),MAKETIME(6,31,0))), -- 汉蓉 今晨到 910008
(704,@tenant_id,71,804,201,203,'201#203#201',1,DATE_SUB(NOW(),INTERVAL 2 DAY)),        -- 京穗 待派送 910011
(705,@tenant_id,72,805,202,205,'202#205#202',1,DATE_SUB(NOW(),INTERVAL 2 DAY)),        -- 沪汉 派送中 910012
(706,@tenant_id,71,806,201,204,'201#204#201',1,DATE_SUB(NOW(),INTERVAL 1 DAY)),        -- 京蓉 已签收 910014
(707,@tenant_id,73,807,203,201,'203#201#203',1,DATE_SUB(NOW(),INTERVAL 5 DAY));        -- 穗京 已签收 910020

-- 8.4 分类-订单（uk tenant+classify+order）
DELETE FROM `disp_classify_order` WHERE tenant_id=@tenant_id AND id BETWEEN 7101 AND 7107;
INSERT INTO `disp_classify_order`
(`id`,`tenant_id`,`classify_id`,`order_id`)
VALUES
(7101,@tenant_id,701,910006),
(7102,@tenant_id,702,910007),
(7103,@tenant_id,703,910008),
(7104,@tenant_id,704,910011),
(7105,@tenant_id,705,910012),
(7106,@tenant_id,706,910014),
(7107,@tenant_id,707,910020);

-- 8.5 算路缓存（7 条当前直达 + 1 条历史中转方案沪蓉经武汉；口径取 base_transport_line）
DELETE FROM `disp_cache_line_detail` WHERE tenant_id=@tenant_id AND id BETWEEN 6101 AND 6109;
DELETE FROM `disp_cache_line`        WHERE tenant_id=@tenant_id AND id BETWEEN 601 AND 608;
INSERT INTO `disp_cache_line`
(`id`,`tenant_id`,`start_org_id`,`end_org_id`,`verify_key`,`distance`,`cost`,`estimated_time`,`transfer_count`,`version`,`is_current`,`create_time`)
VALUES
(601,@tenant_id,202,204,'a91c2e7f01',1960.00,9800.00,1480,0,2,1,NOW()),  -- 沪蓉直达（当前）
(602,@tenant_id,203,206,'b82d3f8012',1720.00,8600.00,1380,0,1,1,NOW()),  -- 穗陕直达
(603,@tenant_id,205,204,'c73e409123',1200.00,6000.00,900,0,1,1,NOW()),   -- 汉蓉直达
(604,@tenant_id,201,203,'d64f510234',2120.00,10600.00,1600,0,1,1,NOW()), -- 京穗直达
(605,@tenant_id,202,205,'e550621345',830.00,4150.00,620,0,1,1,NOW()),   -- 沪汉直达
(606,@tenant_id,201,204,'f461732456',1810.00,9050.00,1360,0,1,1,NOW()), -- 京蓉直达
(607,@tenant_id,203,201,'0372843567',2120.00,10600.00,1600,0,1,1,NOW()), -- 穗京直达
(608,@tenant_id,202,204,'1283954678',2030.00,10150.00,1520,1,1,0,DATE_SUB(NOW(),INTERVAL 20 DAY)); -- 历史：沪蓉经武汉中转
-- 明细（608 两段：沪汉 322 + 汉蓉 323；其余一段直达）
INSERT INTO `disp_cache_line_detail`
(`id`,`tenant_id`,`cache_line_id`,`sort`,`start_org_id`,`end_org_id`,`transport_line_id`,`create_time`)
VALUES
(6101,@tenant_id,601,1,202,204,311,NOW()),
(6102,@tenant_id,602,1,203,206,321,NOW()),
(6103,@tenant_id,603,1,205,204,323,NOW()),
(6104,@tenant_id,604,1,201,203,324,NOW()),
(6105,@tenant_id,605,1,202,205,322,NOW()),
(6106,@tenant_id,606,1,201,204,313,NOW()),
(6107,@tenant_id,607,1,203,201,325,NOW()),
(6108,@tenant_id,608,1,202,205,322,DATE_SUB(NOW(),INTERVAL 20 DAY)),
(6109,@tenant_id,608,2,205,204,323,DATE_SUB(NOW(),INTERVAL 20 DAY));

-- 8.6 算路缓存使用记录（7 个当前分类各采用其直达方案；历史 608 无当前引用）
DELETE FROM `disp_cache_line_use` WHERE tenant_id=@tenant_id AND id BETWEEN 6201 AND 6207;
INSERT INTO `disp_cache_line_use`
(`id`,`tenant_id`,`cache_line_id`,`classify_id`,`create_time`)
VALUES
(6201,@tenant_id,601,701,TIMESTAMP(CURDATE(),MAKETIME(7,3,0))),
(6202,@tenant_id,602,702,TIMESTAMP(CURDATE(),MAKETIME(7,34,0))),
(6203,@tenant_id,603,703,TIMESTAMP(CURDATE(),MAKETIME(6,32,0))),
(6204,@tenant_id,604,704,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(6205,@tenant_id,605,705,DATE_SUB(NOW(),INTERVAL 2 DAY)),
(6206,@tenant_id,606,706,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(6207,@tenant_id,607,707,DATE_SUB(NOW(),INTERVAL 5 DAY));

-- 8.7 分类-派配（车次/车/实际承运司机，严格对齐 §7.9/§7.10；driver_id 口径=sys_user.id）
DELETE FROM `disp_classify_attach` WHERE tenant_id=@tenant_id AND id BETWEEN 7201 AND 7207;
INSERT INTO `disp_classify_attach`
(`id`,`tenant_id`,`classify_id`,`trips_id`,`truck_id`,`driver_id`,`create_time`)
VALUES
(7201,@tenant_id,701,411,521,9114,TIMESTAMP(CURDATE(),MAKETIME(7,20,0))), -- 沪蓉早班/川A7F208/陈志远
(7202,@tenant_id,702,421,523,9115,TIMESTAMP(CURDATE(),MAKETIME(7,45,0))), -- 穗陕夜班/陕A6G128/刘志强
(7203,@tenant_id,703,423,522,9117,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(17,40,0)),INTERVAL 1 DAY)), -- 汉蓉晚班/鄂A9K516/孙大鹏
(7204,@tenant_id,704,424,524,9111,DATE_SUB(NOW(),INTERVAL 2 DAY)),        -- 京穗白班/辽A8H336/李建国
(7205,@tenant_id,705,422,521,9113,DATE_SUB(NOW(),INTERVAL 2 DAY)),        -- 沪汉午班/川A7F208/张伟明
(7206,@tenant_id,706,413,523,9112,DATE_SUB(NOW(),INTERVAL 1 DAY)),        -- 京蓉早班/陕A6G128/王海涛
(7207,@tenant_id,707,425,524,9115,DATE_SUB(NOW(),INTERVAL 5 DAY));        -- 穗京夜班/辽A8H336/刘志强

-- 8.8 异常调度订单（5 条：3 待处理 + 2 已处理；reason 必填）
DELETE FROM `disp_exception_order` WHERE tenant_id=@tenant_id AND id BETWEEN 6301 AND 6305;
INSERT INTO `disp_exception_order`
(`id`,`tenant_id`,`order_id`,`org_id`,`reason`,`status`,`remark`,`create_time`,`handle_time`)
VALUES
(6301,@tenant_id,910019,1209,'收件客户当面验货后以"外箱挤压变形、内件智能硬件外壳划伤"为由拒收，单标为高价值件。',1,'已引导客户发起退款（支付单 PAY910019 已退款），货物登记退回，班车捎回。',DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(6302,@tenant_id,910008,1104,'到付件已到达成都新都中转场并出库，但到付款 ¥275.00 尚未收回，派送前需确认收款方式。',0,NULL,TIMESTAMP(CURDATE(),MAKETIME(7,50,0)),NULL),
(6303,@tenant_id,910012,1213,'到付件派送中，派送员两次上门收件人均不在，到付款 ¥125.00 未收回，需预约第三次派送。',0,NULL,DATE_SUB(NOW(),INTERVAL 3 HOUR),NULL),
(6304,@tenant_id,910009,1202,'同城派送地址为在建写字楼，登记门牌已变更，收件人电话多次拨打无人接听，暂存网点待联系。',0,NULL,TIMESTAMP(CURDATE(),MAKETIME(11,40,0)),NULL),
(6305,@tenant_id,910006,202,'沪蓉干线车辆川A7F208 在黄冈服务区胎压报警，临时检修约 1 小时，预计到达成都延误 2 小时。',1,'服务区快修点更换气门嘴后恢复正常，已电话告知收件客户并更新预计到达时间。',TIMESTAMP(CURDATE(),MAKETIME(13,10,0)),TIMESTAMP(CURDATE(),MAKETIME(14,5,0)));

-- =====================================================================
-- §9 IoT 轨迹与告警（iot_*；硬事实3：轨迹只认 business_id+business_type；
--    车辆 business_id=base_truck.id、business_type=1；快递员 business_id=sys_user.id、business_type=2；
--    坐标一律 BD09=coord_system 3。在途车 521/523 各造连续轨迹，最新点近实时，供大屏/回放）
-- =====================================================================

-- 9.1 在途车 521 沪蓉 9701（14 点，上海青浦→武汉方向，约 5.5h，最新点约 3 分钟前）
DELETE FROM `iot_truck_location` WHERE tenant_id=@tenant_id AND id BETWEEN 92101 AND 92114;
INSERT INTO `iot_truck_location`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`)
VALUES
(92101,@tenant_id,521,1,'陈志远','18801000014','川A7F208',121.223000,31.173000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 330 MINUTE),NOW()),
(92102,@tenant_id,521,1,'陈志远','18801000014','川A7F208',121.050000,31.240000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 305 MINUTE),NOW()),
(92103,@tenant_id,521,1,'陈志远','18801000014','川A7F208',120.620000,31.300000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 280 MINUTE),NOW()),
(92104,@tenant_id,521,1,'陈志远','18801000014','川A7F208',120.300000,31.570000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 255 MINUTE),NOW()),
(92105,@tenant_id,521,1,'陈志远','18801000014','川A7F208',119.970000,31.780000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 230 MINUTE),NOW()),
(92106,@tenant_id,521,1,'陈志远','18801000014','川A7F208',119.450000,32.020000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 205 MINUTE),NOW()),
(92107,@tenant_id,521,1,'陈志远','18801000014','川A7F208',118.860000,32.060000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 180 MINUTE),NOW()),
(92108,@tenant_id,521,1,'陈志远','18801000014','川A7F208',118.220000,31.840000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 155 MINUTE),NOW()),
(92109,@tenant_id,521,1,'陈志远','18801000014','川A7F208',117.300000,31.820000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 130 MINUTE),NOW()),
(92110,@tenant_id,521,1,'陈志远','18801000014','川A7F208',116.520000,31.740000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 105 MINUTE),NOW()),
(92111,@tenant_id,521,1,'陈志远','18801000014','川A7F208',115.900000,31.100000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 80 MINUTE),NOW()),
(92112,@tenant_id,521,1,'陈志远','18801000014','川A7F208',115.050000,30.620000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 55 MINUTE),NOW()),
(92113,@tenant_id,521,1,'陈志远','18801000014','川A7F208',114.870000,30.450000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 30 MINUTE),NOW()),
(92114,@tenant_id,521,1,'陈志远','18801000014','川A7F208',114.400000,30.600000,3,'JT808','成都自营车队',9701,DATE_SUB(NOW(),INTERVAL 3 MINUTE),NOW());

-- 9.2 在途车 523 穗陕 9702（16 点，昨夜广州白云→今午逼近西安，约 16h，最新点约 4 分钟前）
DELETE FROM `iot_truck_location` WHERE tenant_id=@tenant_id AND id BETWEEN 92115 AND 92130;
INSERT INTO `iot_truck_location`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`)
VALUES
(92115,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.273000,23.217000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 960 MINUTE),NOW()),
(92116,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.050000,23.680000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 896 MINUTE),NOW()),
(92117,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.600000,24.810000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 832 MINUTE),NOW()),
(92118,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.020000,25.790000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 768 MINUTE),NOW()),
(92119,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',112.570000,26.900000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 704 MINUTE),NOW()),
(92120,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.130000,27.830000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 640 MINUTE),NOW()),
(92121,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',112.940000,28.230000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 576 MINUTE),NOW()),
(92122,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.130000,29.370000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 512 MINUTE),NOW()),
(92123,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.550000,30.050000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 448 MINUTE),NOW()),
(92124,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',114.310000,30.590000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 384 MINUTE),NOW()),
(92125,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',114.070000,32.130000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 320 MINUTE),NOW()),
(92126,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',114.020000,32.980000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 256 MINUTE),NOW()),
(92127,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',113.620000,34.750000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 192 MINUTE),NOW()),
(92128,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',112.450000,34.620000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 128 MINUTE),NOW()),
(92129,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',111.200000,34.770000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 64 MINUTE),NOW()),
(92130,@tenant_id,523,1,'刘志强','18801000015','陕A6G128',110.200000,34.550000,3,'JT808','西安自营车队',9702,DATE_SUB(NOW(),INTERVAL 4 MINUTE),NOW());

-- 9.3 三名末端快递员派件轨迹（business_type=2，business_id=sys_user.id；无车牌/车队/干线任务；PDA）
-- 9211 周小勇 北京东城-朝阳片区
DELETE FROM `iot_truck_location` WHERE tenant_id=@tenant_id AND id BETWEEN 92201 AND 92210;
INSERT INTO `iot_truck_location`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`)
VALUES
(92201,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.418000,39.928000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 110 MINUTE),NOW()),
(92202,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.432000,39.925000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 98 MINUTE),NOW()),
(92203,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.456000,39.921000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 86 MINUTE),NOW()),
(92204,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.480000,39.930000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 74 MINUTE),NOW()),
(92205,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.492000,39.945000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 62 MINUTE),NOW()),
(92206,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.470000,39.955000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 50 MINUTE),NOW()),
(92207,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.440000,39.950000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 38 MINUTE),NOW()),
(92208,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.420000,39.942000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 26 MINUTE),NOW()),
(92209,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.408000,39.935000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 14 MINUTE),NOW()),
(92210,@tenant_id,9211,2,'周小勇','18801000018',NULL,116.415000,39.930000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 MINUTE),NOW());
-- 9212 吴晓军 上海浦东片区
DELETE FROM `iot_truck_location` WHERE tenant_id=@tenant_id AND id BETWEEN 92211 AND 92220;
INSERT INTO `iot_truck_location`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`)
VALUES
(92211,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.505000,31.245000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 110 MINUTE),NOW()),
(92212,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.515000,31.230000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 98 MINUTE),NOW()),
(92213,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.530000,31.225000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 86 MINUTE),NOW()),
(92214,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.545000,31.235000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 74 MINUTE),NOW()),
(92215,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.560000,31.220000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 62 MINUTE),NOW()),
(92216,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.540000,31.205000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 50 MINUTE),NOW()),
(92217,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.515000,31.200000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 38 MINUTE),NOW()),
(92218,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.500000,31.215000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 26 MINUTE),NOW()),
(92219,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.490000,31.235000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 14 MINUTE),NOW()),
(92220,@tenant_id,9212,2,'吴晓军','18801000019',NULL,121.500000,31.240000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 MINUTE),NOW());
-- 9213 郑志豪 广州天河片区
DELETE FROM `iot_truck_location` WHERE tenant_id=@tenant_id AND id BETWEEN 92221 AND 92230;
INSERT INTO `iot_truck_location`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`)
VALUES
(92221,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.325000,23.135000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 110 MINUTE),NOW()),
(92222,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.335000,23.130000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 98 MINUTE),NOW()),
(92223,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.348000,23.135000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 86 MINUTE),NOW()),
(92224,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.360000,23.128000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 74 MINUTE),NOW()),
(92225,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.355000,23.145000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 62 MINUTE),NOW()),
(92226,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.340000,23.150000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 50 MINUTE),NOW()),
(92227,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.325000,23.148000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 38 MINUTE),NOW()),
(92228,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.315000,23.138000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 26 MINUTE),NOW()),
(92229,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.320000,23.128000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 14 MINUTE),NOW()),
(92230,@tenant_id,9213,2,'郑志豪','18801000020',NULL,113.327000,23.132000,3,'PDA',NULL,NULL,DATE_SUB(NOW(),INTERVAL 2 MINUTE),NOW());

-- 9.4 历史轨迹归档（已完成任务片段，8 点，含 archive_time）
DELETE FROM `iot_truck_location_archive` WHERE tenant_id=@tenant_id AND id BETWEEN 92301 AND 92308;
INSERT INTO `iot_truck_location_archive`
(`id`,`tenant_id`,`business_id`,`business_type`,`name`,`phone`,`license_plate`,`longitude`,`latitude`,`coord_system`,`source`,`team`,`transport_task_id`,`report_time`,`create_time`,`archive_time`)
VALUES
(92301,@tenant_id,524,1,'李建国','18801000011','辽A8H336',116.403000,39.915000,3,'JT808','沈阳自营车队',9704,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,10,0)),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92302,@tenant_id,524,1,'李建国','18801000011','辽A8H336',113.273000,23.217000,3,'JT808','沈阳自营车队',9704,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(15,35,0)),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92303,@tenant_id,521,1,'张伟明','18801000013','川A7F208',121.223000,31.173000,3,'JT808','成都自营车队',9705,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(12,45,0)),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92304,@tenant_id,521,1,'张伟明','18801000013','川A7F208',114.142000,30.621000,3,'JT808','成都自营车队',9705,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(22,15,0)),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92305,@tenant_id,523,1,'王海涛','18801000012','陕A6G128',116.415000,39.915000,3,'JT808','西安自营车队',9706,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(8,35,0)),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92306,@tenant_id,523,1,'王海涛','18801000012','陕A6G128',104.157000,30.824000,3,'JT808','西安自营车队',9706,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(6,35,0)),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY)),
(92307,@tenant_id,522,1,'孙大鹏','18801000017','鄂A9K516',114.142000,30.621000,3,'JT808','武汉自营车队',9703,DATE_SUB(TIMESTAMP(CURDATE(),MAKETIME(18,10,0)),INTERVAL 1 DAY),DATE_SUB(NOW(),INTERVAL 1 DAY),TIMESTAMP(CURDATE(),MAKETIME(7,0,0))),
(92308,@tenant_id,522,1,'孙大鹏','18801000017','鄂A9K516',104.157000,30.824000,3,'JT808','武汉自营车队',9703,TIMESTAMP(CURDATE(),MAKETIME(6,5,0)),NOW(),TIMESTAMP(CURDATE(),MAKETIME(7,0,0)));

-- 9.5 GPS / 人工告警（AlarmTypeEnum：自动 1超速 2长停 3偏航 4离线；人工 10货损 11拒收 12地址错 13车况 15延误 16其他）
--     未处理 status=0 6 条，已处理 6 条；车辆类挂 truck/driver，快递员类（10/11/12）driver_id=快递员 sys_user.id
DELETE FROM `iot_alarm_record` WHERE tenant_id=@tenant_id AND id BETWEEN 93101 AND 93112;
INSERT INTO `iot_alarm_record`
(`id`,`tenant_id`,`alarm_type`,`transport_task_id`,`truck_id`,`driver_id`,`longitude`,`latitude`,`alarm_content`,`alarm_time`,`status`,`create_time`)
VALUES
(93101,@tenant_id,1,9701,521,9114,114.400000,30.600000,'沪蓉高速武汉段超速：限速100km/h，车机上报瞬时速度118km/h。',DATE_SUB(NOW(),INTERVAL 30 MINUTE),0,NOW()),
(93102,@tenant_id,1,9702,523,9115,110.200000,34.550000,'连霍高速灵宝段超速：限速100km/h，实测112km/h。',TIMESTAMP(CURDATE(),MAKETIME(5,20,0)),1,TIMESTAMP(CURDATE(),MAKETIME(5,25,0))),
(93103,@tenant_id,2,9701,521,9114,116.520000,31.740000,'六安服务区长时间停留46分钟（双驾换班+用午餐），已核实为正常停靠。',TIMESTAMP(CURDATE(),MAKETIME(11,40,0)),1,TIMESTAMP(CURDATE(),MAKETIME(11,42,0))),
(93104,@tenant_id,3,9702,523,9115,113.130000,28.000000,'长沙枢纽附近偏离规划路线约6km，因京港澳高速施工绕行，导航重新规划后回到主线。',DATE_SUB(NOW(),INTERVAL 12 HOUR),1,DATE_SUB(NOW(),INTERVAL 12 HOUR)),
(93105,@tenant_id,3,9704,524,9111,113.500000,23.500000,'京穗干线清远段偏航约4km下高速补油，随后返回。',DATE_SUB(NOW(),INTERVAL 1 DAY),1,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(93106,@tenant_id,4,NULL,524,9111,123.400000,41.800000,'车辆辽A8H336定位设备超过30分钟无心跳，最后位置沈阳于洪停车场，疑似设备断电。',DATE_SUB(NOW(),INTERVAL 120 MINUTE),0,NOW()),
(93107,@tenant_id,4,NULL,528,NULL,114.300000,30.550000,'车辆鄂A2K608离线超过60分钟，停于武汉东西湖车队，GPS 终端无响应。',DATE_SUB(NOW(),INTERVAL 50 MINUTE),0,NOW()),
(93108,@tenant_id,11,NULL,NULL,9213,113.259000,23.158000,'客户当面拒收：外箱挤压变形、内件智能硬件外壳划伤（订单910019）。',DATE_SUB(NOW(),INTERVAL 1 DAY),1,DATE_SUB(NOW(),INTERVAL 1 DAY)),
(93109,@tenant_id,10,NULL,NULL,9211,116.480000,39.930000,'派送途中发现一件货物外箱破损，已拍照取证并暂存网点，待与寄方核责任（订单910009关联批次）。',TIMESTAMP(CURDATE(),MAKETIME(10,20,0)),0,NOW()),
(93110,@tenant_id,12,NULL,NULL,9211,116.490000,39.920000,'收件地址为在建写字楼、门牌已变更，收件人电话多次无人接听，快件暂存网点（订单910009）。',TIMESTAMP(CURDATE(),MAKETIME(11,20,0)),0,NOW()),
(93111,@tenant_id,13,9701,521,9114,114.870000,30.450000,'黄冈服务区胎压报警，临时检修更换气门嘴后恢复，已更新预计到达时间。',TIMESTAMP(CURDATE(),MAKETIME(13,10,0)),1,TIMESTAMP(CURDATE(),MAKETIME(13,15,0))),
(93112,@tenant_id,15,9702,523,9115,111.200000,34.770000,'三门峡段因事故拥堵缓行约90分钟，预计到达西安灞桥延误约1.5小时。',DATE_SUB(NOW(),INTERVAL 180 MINUTE),0,NOW());

-- =====================================================================
-- §10 系统参数 sys_config（平台全局 tenant_id=0；避开已存在的 id=1 baidu.map.ak；
--     只补非敏感业务参数，is_sensitive=0；真实第三方密钥不入库）
-- =====================================================================
DELETE FROM `sys_config` WHERE tenant_id=0 AND id BETWEEN 2 AND 6;
INSERT INTO `sys_config`
(`id`,`tenant_id`,`config_name`,`config_key`,`config_value`,`value_type`,`is_sensitive`,`status`,`remark`,`create_by`,`create_time`,`update_by`,`update_time`)
VALUES
(2,0,'订单自动调度开关','order.auto.dispatch.enabled','true','boolean',0,1,'开启后到点由 dispatchTask 自动分类调度在库订单',1000,NOW(),NULL,NOW()),
(3,0,'默认预计送达天数','order.estimated.arrival.days','2','number',0,1,'下单时默认预计 N 天后送达（日历天）',1000,NOW(),NULL,NOW()),
(4,0,'保价服务费率(%)','order.insurance.rate','1.00','number',0,1,'按声明价值的百分比计收保价费，预留参数（现网运费仅 FREIGHT）',1000,NOW(),NULL,NOW()),
(5,0,'车辆离线判定阈值(分钟)','gps.offline.timeout.minutes','30','number',0,1,'GPS 心跳超过该时长无上报判定为离线并触发离线告警',1000,NOW(),NULL,NOW()),
(6,0,'干线超速阈值(km/h)','gps.speed.over.limit','100','number',0,1,'干线路面超速判定基准速度',1000,NOW(),NULL,NOW());

-- ##ANCHOR##
