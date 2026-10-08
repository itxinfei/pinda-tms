-- ==================================================================
-- 管理端接口资源注册（修复 http://192.168.20.130:8080 后台全部功能 401）
-- 根因: 网关 AccessFilter 要求请求 method+url 必须在 pd_auth_resource 中,
--       管理端接口从未注册, 导致 count==0 直接判 401 未知请求。
-- 生成时间 : 2026-10-07 20:25:00
-- 新增资源 : 293 条
-- 新增授权 : 293 条 (role_id=100 PT_ADMIN)
-- 备份表   : pd_auth_resource_bak_20261007_201923 (90 行)
--            pd_auth_role_authority_bak_20261007_201923 (144 行)
-- ==================================================================
USE pd_auth;

-- ---------- 步骤 1: 插入资源（已存在的 method+url 自动跳过） ----------
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (666389235553277985,'agency:tree','查询',NULL,'GET','/agency/tree','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665247015844214379,'agency','查询',NULL,'GET','/agency/{id}','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665045126719322905,'agency:user','查询',NULL,'GET','/agency/user/{id}','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669018856347263421,'agency:user:page','查询',NULL,'GET','/agency/user/page','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661834057343384189,'agency:scope','查询',NULL,'GET','/agency/{id}/scope','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661858955568418469,'agency:scope:post1','添加',NULL,'POST','/agency/scope','pd-web-manager AgencyController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660103225219118640,'business:hall:goodstype:page','查询',NULL,'GET','/business-hall/goodsType/page','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663430947144671367,'business:hall:goodstype','查询',NULL,'GET','/business-hall/goodsType/{id}','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669763860807564791,'business:hall:courier:page','查询',NULL,'GET','/business-hall/courier/page','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663171623608235945,'business:hall:courier','查询',NULL,'GET','/business-hall/courier/{id}','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665685132717758644,'business:hall:courier:scope','查询',NULL,'GET','/business-hall/courier/scope/{id}','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663550171412489832,'business:hall:goodstype:post1','添加',NULL,'POST','/business-hall/goodsType','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666531665938802834,'business:hall:courier:scope:post1','添加',NULL,'POST','/business-hall/courier/scope','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668708164734762873,'business:hall:goodstype:put2','修改',NULL,'PUT','/business-hall/goodsType/{id}','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664225968183611691,'business:hall:goodstype:delete3','删除',NULL,'DELETE','/business-hall/goodsType/{id}','pd-web-manager BusinessHallController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664777658645809108,'order:manager:cargo','查询',NULL,'GET','/order-manager/cargo','pd-web-manager CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669828838269247835,'order:manager:cargo:post1','添加',NULL,'POST','/order-manager/cargo','pd-web-manager CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660852575311892272,'order:manager:cargo:put2','修改',NULL,'PUT','/order-manager/cargo/{id}','pd-web-manager CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669848910682347207,'order:manager:cargo:delete3','删除',NULL,'DELETE','/order-manager/cargo/{id}','pd-web-manager CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660872166373219267,'common:area:simple','查询',NULL,'GET','/common/area/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (661569693415184851,'common:user:simple','查询',NULL,'GET','/common/user/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660303508330434669,'common:fleet:simple','查询',NULL,'GET','/common/fleet/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660660767782519920,'common:trucktype:simple','查询',NULL,'GET','/common/truckType/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660474829916341593,'common:transportlinetype:simple','查询',NULL,'GET','/common/transportLineType/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662065795322148401,'common:goodstype:simple','查询',NULL,'GET','/common/goodsType/simple','pd-web-manager CommonController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660868190224751887,'driver:job:manager:page','添加',NULL,'POST','/driver-job-manager/page','pd-web-manager DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665821449764615294,'order:manager:order','查询',NULL,'GET','/order-manager/order/{id}','pd-web-manager OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660954460396200252,'order:manager:order:page','添加',NULL,'POST','/order-manager/order/page','pd-web-manager OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663466315464101481,'order:manager:order:post1','添加',NULL,'POST','/order-manager/order/{id}','pd-web-manager OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669064551588163546,'orderlocus','查询',NULL,'GET','/orderLocus/{id}','pd-web-manager OrderLocusController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662369913111059657,'pickup:dispatch:task:manager:page','添加',NULL,'POST','/pickup-dispatch-task-manager/page','pd-web-manager PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667028093565982378,'pickup:dispatch:task:manager','修改',NULL,'PUT','/pickup-dispatch-task-manager/{id}','pd-web-manager PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669073428031541693,'transfor:center:bussiness:trucktype:page','查询',NULL,'GET','/transfor-center/bussiness/truckType/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660572756961230765,'transfor:center:bussiness:trucktype','查询',NULL,'GET','/transfor-center/bussiness/truckType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668328809033859305,'transfor:center:bussiness:transportlinetype:page','查询',NULL,'GET','/transfor-center/bussiness/transportLineType/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666258215230134363,'transfor:center:bussiness:transportlinetype','查询',NULL,'GET','/transfor-center/bussiness/transportLineType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661093515695567863,'transfor:center:bussiness:fleet:page','查询',NULL,'GET','/transfor-center/bussiness/fleet/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667334098105851054,'transfor:center:bussiness:fleet','查询',NULL,'GET','/transfor-center/bussiness/fleet/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666272290115916668,'transfor:center:bussiness:truck:page','查询',NULL,'GET','/transfor-center/bussiness/truck/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664187519560680978,'transfor:center:bussiness:truck','查询',NULL,'GET','/transfor-center/bussiness/truck/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (668544474367291347,'transfor:center:bussiness:truck:license','查询',NULL,'GET','/transfor-center/bussiness/truck/{id}/license','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665339293729199255,'transfor:center:bussiness:truck:transporttrips','查询',NULL,'GET','/transfor-center/bussiness/truck/{id}/transportTrips','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667218241828042911,'transfor:center:bussiness:transportline:page','查询',NULL,'GET','/transfor-center/bussiness/transportLine/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661350597201717712,'transfor:center:bussiness:transportline','查询',NULL,'GET','/transfor-center/bussiness/transportLine/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667674902785887452,'transfor:center:bussiness:transportline:trips','查询',NULL,'GET','/transfor-center/bussiness/transportLine/trips','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660862338426481535,'transfor:center:bussiness:transportline:trips:get1','查询',NULL,'GET','/transfor-center/bussiness/transportLine/trips/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666033222743811201,'transfor:center:bussiness:driver:page','查询',NULL,'GET','/transfor-center/bussiness/driver/page','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661667742412560656,'transfor:center:bussiness:driver','查询',NULL,'GET','/transfor-center/bussiness/driver/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660858724988637405,'transfor:center:bussiness:driver:truck','查询',NULL,'GET','/transfor-center/bussiness/driver/{id}/truck','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661941018630005579,'transfor:center:bussiness:driverlicense','查询',NULL,'GET','/transfor-center/bussiness/driverLicense/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664854671066300902,'transfor:center:bussiness:trucktype:post1','添加',NULL,'POST','/transfor-center/bussiness/truckType','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666707565583145564,'transfor:center:bussiness:transportlinetype:post1','添加',NULL,'POST','/transfor-center/bussiness/transportLineType','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669150749287261345,'transfor:center:bussiness:fleet:post1','添加',NULL,'POST','/transfor-center/bussiness/fleet','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665334797175629212,'transfor:center:bussiness:truck:post1','添加',NULL,'POST','/transfor-center/bussiness/truck','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662117990732364200,'transfor:center:bussiness:truck:license:post1','添加',NULL,'POST','/transfor-center/bussiness/truck/{id}/license','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662464507068602077,'transfor:center:bussiness:transportline:post1','添加',NULL,'POST','/transfor-center/bussiness/transportLine','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669138911452250249,'transfor:center:bussiness:transportline:trips:post2','添加',NULL,'POST','/transfor-center/bussiness/transportLine/trips','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667559244661515542,'transfor:center:bussiness:transportline:trips:truckdriver','添加',NULL,'POST','/transfor-center/bussiness/transportLine/trips/{id}/truckDriver','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667099159895519366,'transfor:center:bussiness:driverlicense:post1','添加',NULL,'POST','/transfor-center/bussiness/driverLicense','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669444132119525966,'transfor:center:bussiness:trucktype:put2','修改',NULL,'PUT','/transfor-center/bussiness/truckType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (660490278237650050,'transfor:center:bussiness:transportlinetype:put2','修改',NULL,'PUT','/transfor-center/bussiness/transportLineType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663090951766146011,'transfor:center:bussiness:fleet:put2','修改',NULL,'PUT','/transfor-center/bussiness/fleet/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660235050305804518,'transfor:center:bussiness:truck:put2','修改',NULL,'PUT','/transfor-center/bussiness/truck/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664939816090445035,'transfor:center:bussiness:transportline:put2','修改',NULL,'PUT','/transfor-center/bussiness/transportLine/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662118467119728979,'transfor:center:bussiness:transportline:trips:put3','修改',NULL,'PUT','/transfor-center/bussiness/transportLine/trips/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667162046603310034,'transfor:center:bussiness:driver:put1','修改',NULL,'PUT','/transfor-center/bussiness/driver/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665298289444233510,'transfor:center:bussiness:trucktype:delete3','删除',NULL,'DELETE','/transfor-center/bussiness/truckType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667911299167441074,'transfor:center:bussiness:transportlinetype:delete3','删除',NULL,'DELETE','/transfor-center/bussiness/transportLineType/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661016382358230515,'transfor:center:bussiness:fleet:delete3','删除',NULL,'DELETE','/transfor-center/bussiness/fleet/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666385048970589430,'transfor:center:bussiness:truck:delete3','删除',NULL,'DELETE','/transfor-center/bussiness/truck/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664720109343569296,'transfor:center:bussiness:transportline:delete3','删除',NULL,'DELETE','/transfor-center/bussiness/transportLine/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667225284294153252,'transfor:center:bussiness:transportline:trips:delete4','删除',NULL,'DELETE','/transfor-center/bussiness/transportLine/trips/{id}','pd-web-manager TransforCenterBusinessController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663126476170359351,'transport:order:manager','查询',NULL,'GET','/transport-order-manager/{id}','pd-web-manager TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664340311037621363,'transport:order:manager:page','添加',NULL,'POST','/transport-order-manager/page','pd-web-manager TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665326161437961241,'transport:task:manager','查询',NULL,'GET','/transport-task-manager/{id}','pd-web-manager TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668298104612814832,'transport:task:manager:point','查询',NULL,'GET','/transport-task-manager/point/{id}','pd-web-manager TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660245408970730164,'transport:task:manager:page','添加',NULL,'POST','/transport-task-manager/page','pd-web-manager TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663886856094616183,'transport:task:manager:put1','修改',NULL,'PUT','/transport-task-manager/{id}','pd-web-manager TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664786915835388289,'transfor:center:truck:place:info','查询',NULL,'GET','/transfor-center/truck-place-info/{id}','pd-web-manager TruckLocationController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667727811818856625,'usercenter:info','查询',NULL,'GET','/userCenter/info','pd-web-manager UserCenterController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (666292235594099792,'usercenter:message','查询',NULL,'GET','/userCenter/message','pd-web-manager UserCenterController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668713108520151738,'usercenter:message:put1','修改',NULL,'PUT','/userCenter/message/{id}','pd-web-manager UserCenterController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666577868613994155,'base:goodstype','查询',NULL,'GET','/base/goodsType/{id}','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664540346715510177,'base:goodstype:all','查询',NULL,'GET','/base/goodsType/all','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667990652566313963,'base:goodstype:page','查询',NULL,'GET','/base/goodsType/page','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662595929491385138,'base:goodstype:get1','查询',NULL,'GET','/base/goodsType','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666205917268800516,'base:goodstype:post2','添加',NULL,'POST','/base/goodsType','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663884883243300245,'base:goodstype:put3','修改',NULL,'PUT','/base/goodsType/{id}','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668421425154052237,'base:goodstype:disable','修改',NULL,'PUT','/base/goodsType/{id}/disable','pd-base GoodsTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668635646178178982,'sys:agency:fleet','查询',NULL,'GET','/sys/agency/fleet/{id}','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660446814596513774,'sys:agency:fleet:page','查询',NULL,'GET','/sys/agency/fleet/page','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665224463949281457,'sys:agency:fleet:get1','查询',NULL,'GET','/sys/agency/fleet','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669214479420430598,'sys:agency:fleet:post2','添加',NULL,'POST','/sys/agency/fleet','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668690248858130191,'sys:agency:fleet:put3','修改',NULL,'PUT','/sys/agency/fleet/{id}','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660524768401110534,'sys:agency:fleet:disable','修改',NULL,'PUT','/sys/agency/fleet/{id}/disable','pd-base FleetController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660213600870168271,'scope:agency','查询',NULL,'GET','/scope/agency','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665411283754839775,'scope:courier','查询',NULL,'GET','/scope/courier','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660365382272134898,'scope:agency:batch','添加',NULL,'POST','/scope/agency/batch','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664825466118197938,'scope:courier:batch','添加',NULL,'POST','/scope/courier/batch','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665721610303792613,'scope:agency:delete1','删除',NULL,'DELETE','/scope/agency','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (668264945922450808,'scope:courier:delete1','删除',NULL,'DELETE','/scope/courier','pd-base ScopeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665372842697458270,'base:transportline','查询',NULL,'GET','/base/transportLine/{id}','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669857201146558081,'base:transportline:page','查询',NULL,'GET','/base/transportLine/page','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661892543867581202,'base:transportline:get1','查询',NULL,'GET','/base/transportLine','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661066050290977163,'base:transportline:post2','添加',NULL,'POST','/base/transportLine','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668921646865923125,'base:transportline:list','添加',NULL,'POST','/base/transportLine/list','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663275659070322517,'base:transportline:put3','修改',NULL,'PUT','/base/transportLine/{id}','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665954819953869204,'base:transportline:disable','修改',NULL,'PUT','/base/transportLine/{id}/disable','pd-base TransportLineController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664045350445716108,'base:transportline:type','查询',NULL,'GET','/base/transportLine/type/{id}','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662153988581249729,'base:transportline:type:page','查询',NULL,'GET','/base/transportLine/type/page','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663860676592401658,'base:transportline:type:get1','查询',NULL,'GET','/base/transportLine/type','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663452994200950662,'base:transportline:type:post2','添加',NULL,'POST','/base/transportLine/type','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667943559264822277,'base:transportline:type:put3','修改',NULL,'PUT','/base/transportLine/type/{id}','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664885642316892535,'base:transportline:type:disable','修改',NULL,'PUT','/base/transportLine/type/{id}/disable','pd-base TransportLineTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661970775881336151,'base:transportline:trips','查询',NULL,'GET','/base/transportLine/trips/{id}','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661025100428997328,'base:transportline:trips:get1','查询',NULL,'GET','/base/transportLine/trips','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663548027249706152,'base:transportline:trips:truckdriver','查询',NULL,'GET','/base/transportLine/trips/truckDriver','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664505687202984697,'base:transportline:trips:post2','添加',NULL,'POST','/base/transportLine/trips','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667756902961187556,'base:transportline:trips:truckdriver:post1','添加',NULL,'POST','/base/transportLine/trips/{id}/truckDriver','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667028916706779128,'base:transportline:trips:put3','修改',NULL,'PUT','/base/transportLine/trips/{id}','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (663158011926063815,'base:transportline:trips:disable','修改',NULL,'PUT','/base/transportLine/trips/{id}/disable','pd-base TransportTripsController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669892924364316647,'base:truck','查询',NULL,'GET','/base/truck/{id}','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669080504408860061,'base:truck:page','查询',NULL,'GET','/base/truck/page','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667354799974409963,'base:truck:count','查询',NULL,'GET','/base/truck/count','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667806502084233844,'base:truck:get1','查询',NULL,'GET','/base/truck','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664507968539379002,'base:truck:post2','添加',NULL,'POST','/base/truck','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669190265682263191,'base:truck:put3','修改',NULL,'PUT','/base/truck/{id}','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669459933738453239,'base:truck:disable','修改',NULL,'PUT','/base/truck/{id}/disable','pd-base TruckController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668549713025775808,'base:truck:license','查询',NULL,'GET','/base/truck/license/{id}','pd-base TruckLicenseController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668068519160529583,'base:truck:license:post1','添加',NULL,'POST','/base/truck/license','pd-base TruckLicenseController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668715714853112479,'base:truck:type','查询',NULL,'GET','/base/truck/type/{id}','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668406117203308173,'base:truck:type:page','查询',NULL,'GET','/base/truck/type/page','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667259590589756876,'base:truck:type:get1','查询',NULL,'GET','/base/truck/type','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669450356346769783,'base:truck:type:post2','添加',NULL,'POST','/base/truck/type','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667323193295976529,'base:truck:type:put3','修改',NULL,'PUT','/base/truck/type/{id}','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660838232192149193,'base:truck:type:disable','修改',NULL,'PUT','/base/truck/type/{id}/disable','pd-base TruckTypeController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667456640799251446,'sys:driver','查询',NULL,'GET','/sys/driver','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667043094765922093,'sys:driver:get1','查询',NULL,'GET','/sys/driver/{id}','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662176977463680776,'sys:driver:driverlicense','查询',NULL,'GET','/sys/driver/{id}/driverLicense','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666770957976495250,'sys:driver:count','查询',NULL,'GET','/sys/driver/count','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (661906840156975649,'sys:driver:page','查询',NULL,'GET','/sys/driver/page','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669416580931119127,'sys:driver:findall','查询',NULL,'GET','/sys/driver/findAll','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662196798736825642,'sys:driver:post2','添加',NULL,'POST','/sys/driver','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667210341023143727,'sys:driver:driverlicense:post1','添加',NULL,'POST','/sys/driver/driverLicense','pd-base DriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668467356328686453,'driver:job','查询',NULL,'GET','/driver-job/{id}','pd-work DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669619871371544552,'driver:job:post1','添加',NULL,'POST','/driver-job','pd-work DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669030309787480008,'driver:job:page','添加',NULL,'POST','/driver-job/page','pd-work DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667281404781179830,'driver:job:findall','添加',NULL,'POST','/driver-job/findAll','pd-work DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667093490717104372,'driver:job:put2','修改',NULL,'PUT','/driver-job/{id}','pd-work DriverJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667466178372743427,'pickup:dispatch:task','查询',NULL,'GET','/pickup-dispatch-task/{id}','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663810251917964428,'pickup:dispatch:task:orderid','查询',NULL,'GET','/pickup-dispatch-task/orderId/{orderId}/{taskType}','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666546201048295788,'pickup:dispatch:task:post1','添加',NULL,'POST','/pickup-dispatch-task','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662194633177441357,'pickup:dispatch:task:page','添加',NULL,'POST','/pickup-dispatch-task/page','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665008635230925533,'pickup:dispatch:task:list','添加',NULL,'POST','/pickup-dispatch-task/list','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662164488750379231,'pickup:dispatch:task:put2','修改',NULL,'PUT','/pickup-dispatch-task/{id}','pd-work PickupDispatchTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666908711401421004,'transport:order:page','查询',NULL,'GET','/transport-order/page','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667500383259402428,'transport:order','查询',NULL,'GET','/transport-order/{id}','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665105689624721816,'transport:order:orderid','查询',NULL,'GET','/transport-order/orderId/{orderId}','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662147781546498476,'transport:order:orderids','查询',NULL,'GET','/transport-order/orderIds','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665685408143933828,'transport:order:post1','添加',NULL,'POST','/transport-order','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (665650250835801369,'transport:order:list','添加',NULL,'POST','/transport-order/list','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663100707187446713,'transport:order:put2','修改',NULL,'PUT','/transport-order/{id}','pd-work TransportOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660390389393165364,'transport:task:listbyorderidortaskid','查询',NULL,'GET','/transport-task/listByOrderIdOrTaskId','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660369073349852134,'transport:task','查询',NULL,'GET','/transport-task/{id}','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660210137449106525,'transport:task:post1','添加',NULL,'POST','/transport-task','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669410206208968949,'transport:task:page','添加',NULL,'POST','/transport-task/page','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664792326636481995,'transport:task:list','添加',NULL,'POST','/transport-task/list','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660550341604161323,'transport:task:put2','修改',NULL,'PUT','/transport-task/{id}','pd-work TransportTaskController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667252304658669364,'cargo','查询',NULL,'GET','/cargo','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660823413579138199,'cargo:list','查询',NULL,'GET','/cargo/list','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667232192385060005,'cargo:get1','查询',NULL,'GET','/cargo/{id}','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662382691123691543,'cargo:post2','添加',NULL,'POST','/cargo','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667178369682360134,'cargo:put3','修改',NULL,'PUT','/cargo/{id}','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661868564938480630,'cargo:delete4','删除',NULL,'DELETE','/cargo/{id}','pd-oms CargoController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666406775263471284,'order','查询',NULL,'GET','/order/{id}','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662865970390130945,'order:ids','查询',NULL,'GET','/order/ids','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661366223083219148,'order:orderid','查询',NULL,'GET','/order/orderId','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668440315199092118,'order:post1','添加',NULL,'POST','/order','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666828931980144792,'order:ordermsg','添加',NULL,'POST','/order/orderMsg','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669581269017315399,'order:page','添加',NULL,'POST','/order/page','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (664181291958619225,'order:list','添加',NULL,'POST','/order/list','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667132991558699220,'order:location:saveorupdate','添加',NULL,'POST','/order/location/saveOrUpdate','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668146931940320399,'order:del','添加',NULL,'POST','/order/del','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669370314517955059,'order:put2','修改',NULL,'PUT','/order/{id}','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667045344432752504,'order:pay','修改',NULL,'PUT','/order/{id}/pay','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669067604427580566,'order:reprice','修改',NULL,'PUT','/order/{id}/reprice','pd-oms OrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667210211495953045,'pay:query','查询',NULL,'GET','/pay/query/{orderId}','pd-oms PayController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664018156874135805,'pay:create','添加',NULL,'POST','/pay/create/{orderId}','pd-oms PayController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662632755969653703,'pay:callback','添加',NULL,'POST','/pay/callback/{channel}','pd-oms PayController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664324207760586929,'pay:refund','添加',NULL,'POST','/pay/refund/{orderId}','pd-oms PayController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665788919637257540,'addressbook:detail','查询',NULL,'GET','/addressBook/detail/{id}','pd-user AddressBookController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666368990193743620,'addressbook:page','查询',NULL,'GET','/addressBook/page','pd-user AddressBookController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664324067810864784,'addressbook','添加',NULL,'POST','/addressBook','pd-user AddressBookController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660134059178084312,'addressbook:put1','修改',NULL,'PUT','/addressBook/{id}','pd-user AddressBookController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666446258178729087,'addressbook:delete2','删除',NULL,'DELETE','/addressBook/{id}','pd-user AddressBookController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662516162461457646,'cache:detail','查询',NULL,'GET','/cache/detail/{id}','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663564172939770681,'cache:getcachedata','查询',NULL,'GET','/cache/getCacheData/{id}','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660027875445979104,'cache:getcachedatabody','查询',NULL,'GET','/cache/getCacheDataBody','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667681209975548584,'cache:getcachedataparams','查询',NULL,'GET','/cache/getCacheDataParams','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669239550646691067,'cache:getalldata','查询',NULL,'GET','/cache/getAllData','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (664943560010817624,'cache:evict','查询',NULL,'GET','/cache/evict/{id}','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664634249734863424,'cache:evict:get1','查询',NULL,'GET','/cache/evict','pd-user J2cacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660770040063063214,'member:detail','查询',NULL,'GET','/member/detail/{id}','pd-user MemberController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668405057434371693,'member:page','查询',NULL,'GET','/member/page','pd-user MemberController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668853511840330081,'member','添加',NULL,'POST','/member','pd-user MemberController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663687037582279077,'member:put1','修改',NULL,'PUT','/member/{id}','pd-user MemberController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661147939134692348,'member:delete2','删除',NULL,'DELETE','/member/{id}','pd-user MemberController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662111270330089217,'orderlocus:point','查询',NULL,'GET','/orderLocus/point/{id}','pd-dispatch OrderLocusController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669513654707128230,'scheduleexceptionorder:page','查询',NULL,'GET','/scheduleExceptionOrder/page','pd-dispatch ScheduleExceptionOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661428632525475637,'scheduleexceptionorder:handle','修改',NULL,'PUT','/scheduleExceptionOrder/{id}/handle','pd-dispatch ScheduleExceptionOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663774576750848388,'scheduleexceptionorder:retry','修改',NULL,'PUT','/scheduleExceptionOrder/retry/{agencyId}','pd-dispatch ScheduleExceptionOrderController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665880624313487133,'schedule:page','查询',NULL,'GET','/schedule/page','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660962139617314578,'schedule','查询',NULL,'GET','/schedule/{id}','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667088726076206963,'schedule:dispatch','查询',NULL,'GET','/schedule/dispatch/{id}','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666922772957864275,'schedule:dispatch:post1','添加',NULL,'POST','/schedule/dispatch','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667096672631573868,'schedule:run','修改',NULL,'PUT','/schedule/run/{id}','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660943436062838386,'schedule:run:put1','修改',NULL,'PUT','/schedule/run','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661684244932254175,'schedule:pause','修改',NULL,'PUT','/schedule/pause/{id}','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664407793004187689,'schedule:pause:put1','修改',NULL,'PUT','/schedule/pause','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664022845136708785,'schedule:resume','修改',NULL,'PUT','/schedule/resume/{id}','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (660962325159423940,'schedule:resume:put1','修改',NULL,'PUT','/schedule/resume','pd-dispatch ScheduleJobController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665342875798012683,'schedulelog:page','查询',NULL,'GET','/scheduleLog/page','pd-dispatch ScheduleJobLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666312788038537197,'schedulelog','查询',NULL,'GET','/scheduleLog/{id}','pd-dispatch ScheduleJobLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662048892823631658,'appcourier:page','添加',NULL,'POST','/appCourier/page','pd-aggregation AppCourierController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660510103211818000,'appdriver:page','添加',NULL,'POST','/appDriver/page','pd-aggregation AppDriverController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660020087328906642,'webmanager:driverjob:page','添加',NULL,'POST','/webManager/driverJob/page','pd-aggregation WebManagerController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660806100712154666,'webmanager:taskpickupdispatchjob:page','添加',NULL,'POST','/webManager/taskPickupDispatchJob/page','pd-aggregation WebManagerController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660104662034976379,'webmanager:transportorder:page','添加',NULL,'POST','/webManager/transportOrder/page','pd-aggregation WebManagerController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663870339379720617,'webmanager:tasktransport:page','添加',NULL,'POST','/webManager/taskTransport/page','pd-aggregation WebManagerController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663629029511545883,'enums','查询',NULL,'GET','/enums','pd-auth AuthorityGeneralController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663682835450977678,'orgs','查询',NULL,'GET','/orgs','pd-auth AuthorityGeneralController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663205119442904996,'anno:token','查询',NULL,'GET','/anno/token','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664362582013709828,'anno:verify','查询',NULL,'GET','/anno/verify','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665480803265836565,'anno:check','查询',NULL,'GET','/anno/check','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664458818852243555,'anno:captcha','查询',NULL,'GET','/anno/captcha','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668967718099741790,'anno:login','添加',NULL,'POST','/anno/login','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662018679203607521,'anno:logintx','添加',NULL,'POST','/anno/loginTx','pd-auth LoginController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660412035796385654,'menu:page','查询',NULL,'GET','/menu/page','pd-auth MenuController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667127329171760260,'menu','查询',NULL,'GET','/menu/{id}','pd-auth MenuController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664066394502881340,'menu:router','查询',NULL,'GET','/menu/router','pd-auth MenuController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (669494700107737395,'menu:admin:router','查询',NULL,'GET','/menu/admin/router','pd-auth MenuController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660834870756328650,'menu:tree','查询',NULL,'GET','/menu/tree','pd-auth MenuController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666700814691331070,'resource:page','查询',NULL,'GET','/resource/page','pd-auth ResourceController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665933954801116213,'resource','查询',NULL,'GET','/resource/{id}','pd-auth ResourceController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665713887208372810,'resource:list','查询',NULL,'GET','/resource/list','pd-auth ResourceController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664077244463051370,'roleauthority','查询',NULL,'GET','/roleAuthority/{roleId}','pd-auth RoleAuthorityController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665204353889117770,'role:page','查询',NULL,'GET','/role/page','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668423318403820131,'role','查询',NULL,'GET','/role/{id}','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666658655953697107,'role:check','查询',NULL,'GET','/role/check/{code}','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667009317596502398,'role:user','查询',NULL,'GET','/role/user/{roleId}','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665223103106643343,'role:authority','查询',NULL,'GET','/role/authority/{roleId}','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663729735864042528,'role:codes','查询',NULL,'GET','/role/codes','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666387513452209174,'role:findrolebyuserid','查询',NULL,'GET','/role/findRoleByUserId/{id}','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666692400296755126,'role:findallroles','查询',NULL,'GET','/role/findAllRoles','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665236271759596058,'role:user:post1','添加',NULL,'POST','/role/user','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663880032653925278,'role:authority:post1','添加',NULL,'POST','/role/authority','pd-auth RoleController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (661602334347452337,'user:page','查询',NULL,'GET','/user/page','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662009213541572815,'user','查询',NULL,'GET','/user','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666952267707619512,'user:get1','查询',NULL,'GET','/user/{id}','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666574856521664478,'user:find','查询',NULL,'GET','/user/find','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (661097332314965557,'user:ds','查询',NULL,'GET','/user/ds/{id}','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662131006218690651,'user:role','查询',NULL,'GET','/user/role/{roleId}','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667672198144709941,'user:reset','添加',NULL,'POST','/user/reset','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664377842721053906,'user:anno:id','添加',NULL,'POST','/user/anno/id/{id}','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663215245006545264,'user:avatar','修改',NULL,'PUT','/user/avatar','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662167167075847717,'user:password','修改',NULL,'PUT','/user/password','pd-auth UserController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668930980201663640,'area','查询',NULL,'GET','/area/{id}','pd-auth AreaController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663878255651904174,'area:code','查询',NULL,'GET','/area/code/{code}','pd-auth AreaController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667548259065781110,'dashboard:visit','查询',NULL,'GET','/dashboard/visit','pd-auth DashboardController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668220414172325762,'common:generateid','查询',NULL,'GET','/common/generateId','pd-auth DashboardController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666689242708134353,'j2cache:set','查询',NULL,'GET','/j2cache/set','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669158150780275739,'j2cache:get','查询',NULL,'GET','/j2cache/get','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666030440458333104,'j2cache:evict','查询',NULL,'GET','/j2cache/evict','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663731046652139397,'j2cache:check','查询',NULL,'GET','/j2cache/check','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665871747497635363,'j2cache:exists','查询',NULL,'GET','/j2cache/exists','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (668800475224849703,'j2cache:clear','查询',NULL,'GET','/j2cache/clear','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660388603080135147,'j2cache:keys','查询',NULL,'GET','/j2cache/keys','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667626241676572773,'j2cache:regions','查询',NULL,'GET','/j2cache/regions','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (660791279768268807,'j2cache:removeregion','查询',NULL,'GET','/j2cache/removeRegion','pd-auth J2CacheController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (664676462461676233,'loginlog:anno:login','查询',NULL,'GET','/loginLog/anno/login/{account}','pd-auth LoginLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');
INSERT INTO pd_auth_resource
  (id,code,name,menu_id,method,url,describe_,create_user,create_time,update_user,update_time)
VALUES
  (667473874305976341,'loginlog:page','查询',NULL,'GET','/loginLog/page','pd-auth LoginLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663236815697155123,'loginlog','查询',NULL,'GET','/loginLog/{id}','pd-auth LoginLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667326462871196866,'loginlog:anno:internal','添加',NULL,'POST','/loginLog/anno/internal/{account}','pd-auth LoginLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (666864439796255664,'optlog:page','查询',NULL,'GET','/optLog/page','pd-auth OptLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (662099626964240734,'optlog','查询',NULL,'GET','/optLog/{id}','pd-auth OptLogController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667741358960326435,'org:page','查询',NULL,'GET','/org/page','pd-auth OrgController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663445890826610393,'org:pagelike','查询',NULL,'GET','/org/pageLike','pd-auth OrgController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665126202131430504,'org','查询',NULL,'GET','/org/{id}','pd-auth OrgController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (669503640982921761,'org:tree','查询',NULL,'GET','/org/tree','pd-auth OrgController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (667226016720773611,'org:listbycountyids','查询',NULL,'GET','/org/listByCountyIds','pd-auth OrgController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (665820130457948477,'station:page','查询',NULL,'GET','/station/page','pd-auth StationController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663207463204940768,'station:list','查询',NULL,'GET','/station/list','pd-auth StationController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00'),
  (663211182826550953,'station','查询',NULL,'GET','/station/{id}','pd-auth StationController',3,'2026-10-07 20:25:00',3,'2026-10-07 20:25:00');

-- ---------- 步骤 2: 把新增资源授权给平台管理员 role_id=100 ----------
-- 按 code 精确定位本次新增资源, 逐条授权 (幂等, 兼容 MySQL 5.7)
-- 授权批次 1：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (666389235553277985,665247015844214379,665045126719322905,669018856347263421,661834057343384189,661858955568418469,660103225219118640,663430947144671367,669763860807564791,663171623608235945,665685132717758644,663550171412489832,666531665938802834,668708164734762873,664225968183611691,664777658645809108,669828838269247835,660852575311892272,669848910682347207,660872166373219267,661569693415184851,660303508330434669,660660767782519920,660474829916341593,662065795322148401,660868190224751887,665821449764615294,660954460396200252,663466315464101481,669064551588163546,662369913111059657,667028093565982378,669073428031541693,660572756961230765,668328809033859305,666258215230134363,661093515695567863,667334098105851054,666272290115916668,664187519560680978,668544474367291347,665339293729199255,667218241828042911,661350597201717712,667674902785887452,660862338426481535,666033222743811201,661667742412560656,660858724988637405,661941018630005579);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('660799211763949421','100',666389235553277985,'RESOURCE'),('667216464549841597','100',665247015844214379,'RESOURCE'),('661258936370910315','100',665045126719322905,'RESOURCE'),('664775861498462206','100',669018856347263421,'RESOURCE'),('663558063546311812','100',661834057343384189,'RESOURCE'),('666356749595637200','100',661858955568418469,'RESOURCE'),('668364766409405806','100',660103225219118640,'RESOURCE'),('663603408371864448','100',663430947144671367,'RESOURCE'),('667505242715105298','100',669763860807564791,'RESOURCE'),('667731708191957961','100',663171623608235945,'RESOURCE'),('666093645289283566','100',665685132717758644,'RESOURCE'),('662693877838393163','100',663550171412489832,'RESOURCE'),('667839682930201731','100',666531665938802834,'RESOURCE'),('662014154510449560','100',668708164734762873,'RESOURCE'),('662076746206521157','100',664225968183611691,'RESOURCE'),('668144153885577476','100',664777658645809108,'RESOURCE'),('666223861913557106','100',669828838269247835,'RESOURCE'),('669113988567776338','100',660852575311892272,'RESOURCE'),('667745267734732522','100',669848910682347207,'RESOURCE'),('667480828227794994','100',660872166373219267,'RESOURCE'),('660917283306273803','100',661569693415184851,'RESOURCE'),('666693293995935946','100',660303508330434669,'RESOURCE'),('666225806721648101','100',660660767782519920,'RESOURCE'),('668074300355559121','100',660474829916341593,'RESOURCE'),('662647409571850299','100',662065795322148401,'RESOURCE'),('664236501824235882','100',660868190224751887,'RESOURCE'),('660609867683136505','100',665821449764615294,'RESOURCE'),('663174485272903590','100',660954460396200252,'RESOURCE'),('668364226150283219','100',663466315464101481,'RESOURCE'),('663908529209167410','100',669064551588163546,'RESOURCE'),('663237058871137123','100',662369913111059657,'RESOURCE'),('669392834493408638','100',667028093565982378,'RESOURCE'),('663046007107372441','100',669073428031541693,'RESOURCE'),('669782718372222840','100',660572756961230765,'RESOURCE'),('660872231343246756','100',668328809033859305,'RESOURCE'),('669670229048421497','100',666258215230134363,'RESOURCE'),('665773494084354037','100',661093515695567863,'RESOURCE'),('665655550457864679','100',667334098105851054,'RESOURCE'),('669767775801306131','100',666272290115916668,'RESOURCE'),('661924604311295505','100',664187519560680978,'RESOURCE'),('665046760138499560','100',668544474367291347,'RESOURCE'),('663719207378274124','100',665339293729199255,'RESOURCE'),('661627885891914673','100',667218241828042911,'RESOURCE'),('667111023728210640','100',661350597201717712,'RESOURCE'),('668351032556691578','100',667674902785887452,'RESOURCE'),('663348674606965173','100',660862338426481535,'RESOURCE'),('667786294552349965','100',666033222743811201,'RESOURCE'),('666639580746095004','100',661667742412560656,'RESOURCE'),('669818362067506869','100',660858724988637405,'RESOURCE'),('661906587892438795','100',661941018630005579,'RESOURCE');
-- 授权批次 2：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (664854671066300902,666707565583145564,669150749287261345,665334797175629212,662117990732364200,662464507068602077,669138911452250249,667559244661515542,667099159895519366,669444132119525966,660490278237650050,663090951766146011,660235050305804518,664939816090445035,662118467119728979,667162046603310034,665298289444233510,667911299167441074,661016382358230515,666385048970589430,664720109343569296,667225284294153252,663126476170359351,664340311037621363,665326161437961241,668298104612814832,660245408970730164,663886856094616183,664786915835388289,667727811818856625,666292235594099792,668713108520151738,666577868613994155,664540346715510177,667990652566313963,662595929491385138,666205917268800516,663884883243300245,668421425154052237,668635646178178982,660446814596513774,665224463949281457,669214479420430598,668690248858130191,660524768401110534,660213600870168271,665411283754839775,660365382272134898,664825466118197938,665721610303792613);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('668118911572704632','100',664854671066300902,'RESOURCE'),('664339931433443622','100',666707565583145564,'RESOURCE'),('663050171807037629','100',669150749287261345,'RESOURCE'),('663763791762852726','100',665334797175629212,'RESOURCE'),('667539915621409171','100',662117990732364200,'RESOURCE'),('667000296032284825','100',662464507068602077,'RESOURCE'),('660248944364051606','100',669138911452250249,'RESOURCE'),('663401612842197287','100',667559244661515542,'RESOURCE'),('668903700620213515','100',667099159895519366,'RESOURCE'),('660217993551133726','100',669444132119525966,'RESOURCE'),('660688136423149702','100',660490278237650050,'RESOURCE'),('662819589661558103','100',663090951766146011,'RESOURCE'),('664727202351647093','100',660235050305804518,'RESOURCE'),('665010123159296483','100',664939816090445035,'RESOURCE'),('661220853603620940','100',662118467119728979,'RESOURCE'),('665675855532276928','100',667162046603310034,'RESOURCE'),('669053505880136135','100',665298289444233510,'RESOURCE'),('667490734718440938','100',667911299167441074,'RESOURCE'),('667869074843495695','100',661016382358230515,'RESOURCE'),('662826195824281013','100',666385048970589430,'RESOURCE'),('669119626420734354','100',664720109343569296,'RESOURCE'),('662003986503756469','100',667225284294153252,'RESOURCE'),('664441276199389345','100',663126476170359351,'RESOURCE'),('660927624788320805','100',664340311037621363,'RESOURCE'),('667958478005016493','100',665326161437961241,'RESOURCE'),('663961371504126041','100',668298104612814832,'RESOURCE'),('665902232375460177','100',660245408970730164,'RESOURCE'),('663146171508211364','100',663886856094616183,'RESOURCE'),('669294354049375038','100',664786915835388289,'RESOURCE'),('666415365408970336','100',667727811818856625,'RESOURCE'),('663207156887714704','100',666292235594099792,'RESOURCE'),('669601786351959944','100',668713108520151738,'RESOURCE'),('668642878082577861','100',666577868613994155,'RESOURCE'),('669472564547683423','100',664540346715510177,'RESOURCE'),('667712759150636798','100',667990652566313963,'RESOURCE'),('662987243380951248','100',662595929491385138,'RESOURCE'),('662927501372709009','100',666205917268800516,'RESOURCE'),('662334938090506997','100',663884883243300245,'RESOURCE'),('669192139050112816','100',668421425154052237,'RESOURCE'),('668544909169103295','100',668635646178178982,'RESOURCE'),('660767831641570777','100',660446814596513774,'RESOURCE'),('663703296911396049','100',665224463949281457,'RESOURCE'),('660301561491603350','100',669214479420430598,'RESOURCE'),('666009107519833661','100',668690248858130191,'RESOURCE'),('667849231215978708','100',660524768401110534,'RESOURCE'),('660297046230322776','100',660213600870168271,'RESOURCE'),('662153637492459304','100',665411283754839775,'RESOURCE'),('665674095474759657','100',660365382272134898,'RESOURCE'),('661505978592438971','100',664825466118197938,'RESOURCE'),('666172487826190945','100',665721610303792613,'RESOURCE');
-- 授权批次 3：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (668264945922450808,665372842697458270,669857201146558081,661892543867581202,661066050290977163,668921646865923125,663275659070322517,665954819953869204,664045350445716108,662153988581249729,663860676592401658,663452994200950662,667943559264822277,664885642316892535,661970775881336151,661025100428997328,663548027249706152,664505687202984697,667756902961187556,667028916706779128,663158011926063815,669892924364316647,669080504408860061,667354799974409963,667806502084233844,664507968539379002,669190265682263191,669459933738453239,668549713025775808,668068519160529583,668715714853112479,668406117203308173,667259590589756876,669450356346769783,667323193295976529,660838232192149193,667456640799251446,667043094765922093,662176977463680776,666770957976495250,661906840156975649,669416580931119127,662196798736825642,667210341023143727,668467356328686453,669619871371544552,669030309787480008,667281404781179830,667093490717104372,667466178372743427);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('668976567853233097','100',668264945922450808,'RESOURCE'),('665296402799742077','100',665372842697458270,'RESOURCE'),('662161853738041729','100',669857201146558081,'RESOURCE'),('669314133252810366','100',661892543867581202,'RESOURCE'),('669455850781328440','100',661066050290977163,'RESOURCE'),('668548863502016358','100',668921646865923125,'RESOURCE'),('664474854181357818','100',663275659070322517,'RESOURCE'),('664566169787312164','100',665954819953869204,'RESOURCE'),('662221707522498330','100',664045350445716108,'RESOURCE'),('667655321232989697','100',662153988581249729,'RESOURCE'),('665143907585921167','100',663860676592401658,'RESOURCE'),('666485979088757317','100',663452994200950662,'RESOURCE'),('669729572227809173','100',667943559264822277,'RESOURCE'),('661342011494297646','100',664885642316892535,'RESOURCE'),('664675655178942161','100',661970775881336151,'RESOURCE'),('660544230829139538','100',661025100428997328,'RESOURCE'),('669343644403616941','100',663548027249706152,'RESOURCE'),('668289062498190402','100',664505687202984697,'RESOURCE'),('666352745604743788','100',667756902961187556,'RESOURCE'),('661908844718620007','100',667028916706779128,'RESOURCE'),('668870635762913387','100',663158011926063815,'RESOURCE'),('663896493292512668','100',669892924364316647,'RESOURCE'),('667383528692338029','100',669080504408860061,'RESOURCE'),('660655671766432835','100',667354799974409963,'RESOURCE'),('668373199661393876','100',667806502084233844,'RESOURCE'),('666600377553138458','100',664507968539379002,'RESOURCE'),('668256498594585232','100',669190265682263191,'RESOURCE'),('663408585697470487','100',669459933738453239,'RESOURCE'),('666072686762386285','100',668549713025775808,'RESOURCE'),('669398463489920094','100',668068519160529583,'RESOURCE'),('663893487522874754','100',668715714853112479,'RESOURCE'),('660845488654256760','100',668406117203308173,'RESOURCE'),('667019094177662940','100',667259590589756876,'RESOURCE'),('662160764540425582','100',669450356346769783,'RESOURCE'),('665479798502679643','100',667323193295976529,'RESOURCE'),('664003167703831467','100',660838232192149193,'RESOURCE'),('666233228917770254','100',667456640799251446,'RESOURCE'),('666848548410733997','100',667043094765922093,'RESOURCE'),('666178370873271585','100',662176977463680776,'RESOURCE'),('662678714736075837','100',666770957976495250,'RESOURCE'),('666294080159028181','100',661906840156975649,'RESOURCE'),('660974294136714805','100',669416580931119127,'RESOURCE'),('660731682829695707','100',662196798736825642,'RESOURCE'),('665621657533157894','100',667210341023143727,'RESOURCE'),('662233070278532747','100',668467356328686453,'RESOURCE'),('669939470166762398','100',669619871371544552,'RESOURCE'),('667763620334784034','100',669030309787480008,'RESOURCE'),('660945456791459651','100',667281404781179830,'RESOURCE'),('664657425542152582','100',667093490717104372,'RESOURCE'),('661270248128017230','100',667466178372743427,'RESOURCE');
-- 授权批次 4：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (663810251917964428,666546201048295788,662194633177441357,665008635230925533,662164488750379231,666908711401421004,667500383259402428,665105689624721816,662147781546498476,665685408143933828,665650250835801369,663100707187446713,660390389393165364,660369073349852134,660210137449106525,669410206208968949,664792326636481995,660550341604161323,667252304658669364,660823413579138199,667232192385060005,662382691123691543,667178369682360134,661868564938480630,666406775263471284,662865970390130945,661366223083219148,668440315199092118,666828931980144792,669581269017315399,664181291958619225,667132991558699220,668146931940320399,669370314517955059,667045344432752504,669067604427580566,667210211495953045,664018156874135805,662632755969653703,664324207760586929,665788919637257540,666368990193743620,664324067810864784,660134059178084312,666446258178729087,662516162461457646,663564172939770681,660027875445979104,667681209975548584,669239550646691067);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('663492618566386436','100',663810251917964428,'RESOURCE'),('662512077639837622','100',666546201048295788,'RESOURCE'),('669029794450106970','100',662194633177441357,'RESOURCE'),('661865519786492766','100',665008635230925533,'RESOURCE'),('663459588802476383','100',662164488750379231,'RESOURCE'),('661931560310424239','100',666908711401421004,'RESOURCE'),('663030737642014425','100',667500383259402428,'RESOURCE'),('666048616844096652','100',665105689624721816,'RESOURCE'),('663250356025910058','100',662147781546498476,'RESOURCE'),('664229497148592802','100',665685408143933828,'RESOURCE'),('660897789599136588','100',665650250835801369,'RESOURCE'),('665036251432899383','100',663100707187446713,'RESOURCE'),('660103736670960966','100',660390389393165364,'RESOURCE'),('663115692701483732','100',660369073349852134,'RESOURCE'),('667935185967792845','100',660210137449106525,'RESOURCE'),('665787385278337681','100',669410206208968949,'RESOURCE'),('665215577085808012','100',664792326636481995,'RESOURCE'),('664112139122126161','100',660550341604161323,'RESOURCE'),('660269767786162219','100',667252304658669364,'RESOURCE'),('667475809436859518','100',660823413579138199,'RESOURCE'),('666650694768093974','100',667232192385060005,'RESOURCE'),('669052661705031078','100',662382691123691543,'RESOURCE'),('661273412515202628','100',667178369682360134,'RESOURCE'),('661995574043752579','100',661868564938480630,'RESOURCE'),('663562382924021989','100',666406775263471284,'RESOURCE'),('661511421264069493','100',662865970390130945,'RESOURCE'),('662256368380665136','100',661366223083219148,'RESOURCE'),('669089324030990012','100',668440315199092118,'RESOURCE'),('667668293481747755','100',666828931980144792,'RESOURCE'),('660028385563335700','100',669581269017315399,'RESOURCE'),('662342762720475967','100',664181291958619225,'RESOURCE'),('661602341145011521','100',667132991558699220,'RESOURCE'),('669308894462884179','100',668146931940320399,'RESOURCE'),('662625128239664755','100',669370314517955059,'RESOURCE'),('667319603183296986','100',667045344432752504,'RESOURCE'),('669626170475209964','100',669067604427580566,'RESOURCE'),('666985429341223571','100',667210211495953045,'RESOURCE'),('665771295442048499','100',664018156874135805,'RESOURCE'),('661727138454925395','100',662632755969653703,'RESOURCE'),('664028031272928389','100',664324207760586929,'RESOURCE'),('665646622367498831','100',665788919637257540,'RESOURCE'),('662542746258742355','100',666368990193743620,'RESOURCE'),('669429591015238419','100',664324067810864784,'RESOURCE'),('660032172777005994','100',660134059178084312,'RESOURCE'),('663035290029807276','100',666446258178729087,'RESOURCE'),('667966031561483791','100',662516162461457646,'RESOURCE'),('664313771186305205','100',663564172939770681,'RESOURCE'),('669147953586828057','100',660027875445979104,'RESOURCE'),('669097577875080363','100',667681209975548584,'RESOURCE'),('661340372237950876','100',669239550646691067,'RESOURCE');
-- 授权批次 5：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (664943560010817624,664634249734863424,660770040063063214,668405057434371693,668853511840330081,663687037582279077,661147939134692348,662111270330089217,669513654707128230,661428632525475637,663774576750848388,665880624313487133,660962139617314578,667088726076206963,666922772957864275,667096672631573868,660943436062838386,661684244932254175,664407793004187689,664022845136708785,660962325159423940,665342875798012683,666312788038537197,662048892823631658,660510103211818000,660020087328906642,660806100712154666,660104662034976379,663870339379720617,663629029511545883,663682835450977678,663205119442904996,664362582013709828,665480803265836565,664458818852243555,668967718099741790,662018679203607521,660412035796385654,667127329171760260,664066394502881340,669494700107737395,660834870756328650,666700814691331070,665933954801116213,665713887208372810,664077244463051370,665204353889117770,668423318403820131,666658655953697107,667009317596502398);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('663735846482916095','100',664943560010817624,'RESOURCE'),('668595486718492739','100',664634249734863424,'RESOURCE'),('666432783340800153','100',660770040063063214,'RESOURCE'),('667879767946070780','100',668405057434371693,'RESOURCE'),('661292393032152408','100',668853511840330081,'RESOURCE'),('664136683068945711','100',663687037582279077,'RESOURCE'),('666666080501594841','100',661147939134692348,'RESOURCE'),('667698140019193962','100',662111270330089217,'RESOURCE'),('668846242773369437','100',669513654707128230,'RESOURCE'),('660786895709285143','100',661428632525475637,'RESOURCE'),('667194108449883689','100',663774576750848388,'RESOURCE'),('667606764905957356','100',665880624313487133,'RESOURCE'),('668959955536464951','100',660962139617314578,'RESOURCE'),('661813043284404970','100',667088726076206963,'RESOURCE'),('668951579041526129','100',666922772957864275,'RESOURCE'),('663619275075236641','100',667096672631573868,'RESOURCE'),('662185650212506470','100',660943436062838386,'RESOURCE'),('668767427820991095','100',661684244932254175,'RESOURCE'),('662101872208114802','100',664407793004187689,'RESOURCE'),('669420598366199192','100',664022845136708785,'RESOURCE'),('661429774052765736','100',660962325159423940,'RESOURCE'),('666104540224961042','100',665342875798012683,'RESOURCE'),('665098796471716430','100',666312788038537197,'RESOURCE'),('660887984132125160','100',662048892823631658,'RESOURCE'),('661157083580406040','100',660510103211818000,'RESOURCE'),('668870571711378074','100',660020087328906642,'RESOURCE'),('669159744988082879','100',660806100712154666,'RESOURCE'),('667831835455361562','100',660104662034976379,'RESOURCE'),('660281288721316139','100',663870339379720617,'RESOURCE'),('667551713218285400','100',663629029511545883,'RESOURCE'),('661461565495750605','100',663682835450977678,'RESOURCE'),('661476501253854592','100',663205119442904996,'RESOURCE'),('661809535406452789','100',664362582013709828,'RESOURCE'),('666699261989064626','100',665480803265836565,'RESOURCE'),('666789953396334554','100',664458818852243555,'RESOURCE'),('661375968205231114','100',668967718099741790,'RESOURCE'),('664929122759259760','100',662018679203607521,'RESOURCE'),('665995269056261096','100',660412035796385654,'RESOURCE'),('661435939205989252','100',667127329171760260,'RESOURCE'),('667521160778201642','100',664066394502881340,'RESOURCE'),('668858699950652383','100',669494700107737395,'RESOURCE'),('669280124542864566','100',660834870756328650,'RESOURCE'),('663874324863894669','100',666700814691331070,'RESOURCE'),('667520766553817458','100',665933954801116213,'RESOURCE'),('661050376903282667','100',665713887208372810,'RESOURCE'),('669474900569698596','100',664077244463051370,'RESOURCE'),('664575657449701247','100',665204353889117770,'RESOURCE'),('669786154475318124','100',668423318403820131,'RESOURCE'),('669958932360131084','100',666658655953697107,'RESOURCE'),('668598851283467875','100',667009317596502398,'RESOURCE');
-- 授权批次 6：先清后插，保证脚本可重复执行
DELETE FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE'
  AND authority_id IN (665223103106643343,663729735864042528,666387513452209174,666692400296755126,665236271759596058,663880032653925278,661602334347452337,662009213541572815,666952267707619512,666574856521664478,661097332314965557,662131006218690651,667672198144709941,664377842721053906,663215245006545264,662167167075847717,668930980201663640,663878255651904174,667548259065781110,668220414172325762,666689242708134353,669158150780275739,666030440458333104,663731046652139397,665871747497635363,668800475224849703,660388603080135147,667626241676572773,660791279768268807,664676462461676233,667473874305976341,663236815697155123,667326462871196866,666864439796255664,662099626964240734,667741358960326435,663445890826610393,665126202131430504,669503640982921761,667226016720773611,665820130457948477,663207463204940768,663211182826550953);
INSERT INTO pd_auth_role_authority (id, role_id, authority_id, authority_type) VALUES
('661206736191528101','100',665223103106643343,'RESOURCE'),('666316224477419597','100',663729735864042528,'RESOURCE'),('665641415460973816','100',666387513452209174,'RESOURCE'),('663390801867716372','100',666692400296755126,'RESOURCE'),('666736871483021623','100',665236271759596058,'RESOURCE'),('661214674368507272','100',663880032653925278,'RESOURCE'),('667715759061636583','100',661602334347452337,'RESOURCE'),('660989751487186504','100',662009213541572815,'RESOURCE'),('663434430893946309','100',666952267707619512,'RESOURCE'),('669788496400122986','100',666574856521664478,'RESOURCE'),('665545951938540114','100',661097332314965557,'RESOURCE'),('669738848818174614','100',662131006218690651,'RESOURCE'),('669330751891691413','100',667672198144709941,'RESOURCE'),('660657022391305565','100',664377842721053906,'RESOURCE'),('669028138631250312','100',663215245006545264,'RESOURCE'),('664086591095978297','100',662167167075847717,'RESOURCE'),('667725309335814136','100',668930980201663640,'RESOURCE'),('665050661911353483','100',663878255651904174,'RESOURCE'),('665018992470717106','100',667548259065781110,'RESOURCE'),('669607838602815072','100',668220414172325762,'RESOURCE'),('664273287006215275','100',666689242708134353,'RESOURCE'),('665427581481178867','100',669158150780275739,'RESOURCE'),('669327479178356835','100',666030440458333104,'RESOURCE'),('666025240721179199','100',663731046652139397,'RESOURCE'),('661387251633136124','100',665871747497635363,'RESOURCE'),('663384035023116700','100',668800475224849703,'RESOURCE'),('665847275627079607','100',660388603080135147,'RESOURCE'),('660289375480807558','100',667626241676572773,'RESOURCE'),('664529439527302142','100',660791279768268807,'RESOURCE'),('663954101602921438','100',664676462461676233,'RESOURCE'),('663473093025350361','100',667473874305976341,'RESOURCE'),('661656807863218261','100',663236815697155123,'RESOURCE'),('661095144420275260','100',667326462871196866,'RESOURCE'),('663275812164737889','100',666864439796255664,'RESOURCE'),('662116777670921631','100',662099626964240734,'RESOURCE'),('660049011041470488','100',667741358960326435,'RESOURCE'),('669141320447997184','100',663445890826610393,'RESOURCE'),('661239024057134158','100',665126202131430504,'RESOURCE'),('662212532935074985','100',669503640982921761,'RESOURCE'),('660966843609373209','100',667226016720773611,'RESOURCE'),('669335142799122348','100',665820130457948477,'RESOURCE'),('668442024885510182','100',663207463204940768,'RESOURCE'),('668960429764162194','100',663211182826550953,'RESOURCE');

-- ---------- 步骤 3: 清理脏授权（指向已不存在资源的记录） ----------
DELETE ra FROM pd_auth_role_authority ra
LEFT JOIN pd_auth_resource r ON r.id = ra.authority_id
WHERE r.id IS NULL;

-- ---------- 验证 ----------
SELECT COUNT(*) AS 资源总数 FROM pd_auth_resource;
SELECT COUNT(*) AS role100_RESOURCE授权数
  FROM pd_auth_role_authority WHERE role_id='100' AND authority_type='RESOURCE';
SELECT method, url FROM pd_auth_resource
 WHERE CONCAT(method,url) IN ('GET/menu/router','GET/dashboard/visit','GET/loginLog/page')
