-- =====================================================================
-- 演示种子数据「只读验收」脚本  日期：2026-10-12
-- 纯 SELECT，无任何写操作，可重复执行。
-- 用法（本机 Git Bash）：
--   ssh pdwl "docker exec -i mysql8 sh -c 'mysql -uroot -p\$MYSQL_ROOT_PASSWORD --default-character-set=utf8mb4 pinda_tms'" < docs/sql/演示种子_验收核对_20261012.sql
-- =====================================================================
SET NAMES utf8mb4;

-- 【1】管理账号完好（应 2 行：1000 admin / 1001 pinda，deleted=0）
SELECT id, account, name, status, deleted FROM sys_user WHERE id IN (1000,1001) ORDER BY id;

-- 【2】种子关键计数（cnt 应与 expect 一致）
SELECT '组织(总1+枢纽7+中转场7+营业部19)' item, 34 expect, COUNT(*) cnt FROM sys_org WHERE tenant_id=1 AND (id=100 OR id BETWEEN 201 AND 207 OR id BETWEEN 1101 AND 1107 OR id BETWEEN 1201 AND 1219)
UNION ALL SELECT '岗位',9,COUNT(*) FROM sys_station WHERE id BETWEEN 1 AND 9
UNION ALL SELECT '角色',7,COUNT(*) FROM sys_role WHERE tenant_id=1 AND id BETWEEN 2 AND 8
UNION ALL SELECT 'demo账号(7司机+5快递员)',12,COUNT(*) FROM sys_user WHERE tenant_id=1 AND id IN (9111,9112,9113,9114,9115,9116,9117,9201,9202,9211,9212,9213)
UNION ALL SELECT '会员',10,COUNT(*) FROM member_info WHERE tenant_id=1 AND id BETWEEN 1501 AND 1510
UNION ALL SELECT '地址簿',20,COUNT(*) FROM member_address_book WHERE tenant_id=1 AND id BETWEEN 1601 AND 1620
UNION ALL SELECT '车队',7,COUNT(*) FROM base_fleet WHERE tenant_id=1 AND id BETWEEN 611 AND 617
UNION ALL SELECT '车辆',10,COUNT(*) FROM base_truck WHERE tenant_id=1 AND id BETWEEN 521 AND 530
UNION ALL SELECT '司机档案',7,COUNT(*) FROM base_truck_driver WHERE tenant_id=1 AND id BETWEEN 9111 AND 9117
UNION ALL SELECT '车次',15,COUNT(*) FROM base_transport_trips WHERE tenant_id=1 AND id BETWEEN 411 AND 425
UNION ALL SELECT '订单',20,COUNT(*) FROM oms_order WHERE tenant_id=1 AND id BETWEEN 910001 AND 910020 AND deleted=0
UNION ALL SELECT '货物',20,COUNT(*) FROM oms_order_cargo WHERE tenant_id=1 AND id BETWEEN 930001 AND 930020
UNION ALL SELECT '订单位置',20,COUNT(*) FROM oms_order_location WHERE tenant_id=1 AND id BETWEEN 912001 AND 912020 AND deleted=0
UNION ALL SELECT '运费明细',20,COUNT(*) FROM oms_freight_detail WHERE tenant_id=1 AND id BETWEEN 913001 AND 913020
UNION ALL SELECT '运单',20,COUNT(*) FROM work_transport_order WHERE tenant_id=1 AND id BETWEEN 920001 AND 920020 AND deleted=0
UNION ALL SELECT '运单-任务关联',7,COUNT(*) FROM work_transport_order_task WHERE tenant_id=1 AND id BETWEEN 940001 AND 940007
UNION ALL SELECT '运输任务',10,COUNT(*) FROM work_task_transport WHERE tenant_id=1 AND id BETWEEN 9701 AND 9710 AND deleted=0
UNION ALL SELECT '司机作业',11,COUNT(*) FROM work_driver_job WHERE tenant_id=1 AND id BETWEEN 9801 AND 9811 AND deleted=0
UNION ALL SELECT '取派任务',30,COUNT(*) FROM work_pickup_dispatch_task WHERE tenant_id=1 AND id BETWEEN 950001 AND 950030 AND deleted=0
UNION ALL SELECT '状态流转历史',20,COUNT(*) FROM work_status_transition_history WHERE tenant_id=1 AND id BETWEEN 990001 AND 990020
UNION ALL SELECT '调度作业',7,COUNT(*) FROM disp_schedule_job WHERE tenant_id=1 AND id BETWEEN 71 AND 77
UNION ALL SELECT '调度日志',12,COUNT(*) FROM disp_schedule_job_log WHERE tenant_id=1 AND id BETWEEN 801 AND 812
UNION ALL SELECT '调度分类',7,COUNT(*) FROM disp_order_classify WHERE tenant_id=1 AND id BETWEEN 701 AND 707
UNION ALL SELECT '缓存线路',8,COUNT(*) FROM disp_cache_line WHERE tenant_id=1 AND id BETWEEN 601 AND 608
UNION ALL SELECT '异常工单',5,COUNT(*) FROM disp_exception_order WHERE tenant_id=1 AND id BETWEEN 6301 AND 6305
UNION ALL SELECT 'GPS轨迹',60,COUNT(*) FROM iot_truck_location WHERE tenant_id=1 AND id BETWEEN 92101 AND 92230
UNION ALL SELECT '告警',12,COUNT(*) FROM iot_alarm_record WHERE tenant_id=1 AND id BETWEEN 93101 AND 93112
UNION ALL SELECT '支付单',20,COUNT(*) FROM pd_payment_order WHERE tenant_id=1 AND id LIKE 'PAY9100%'
UNION ALL SELECT '结算单',17,COUNT(*) FROM oms_settlement_order WHERE tenant_id=1 AND id BETWEEN 960001 AND 960017 AND deleted=0
UNION ALL SELECT '系统参数',5,COUNT(*) FROM sys_config WHERE tenant_id=0 AND id BETWEEN 2 AND 6;

-- 【3】断链检测（bad 必须全部为 0）
SELECT '01 cargo->order' chk, COUNT(*) bad FROM oms_order_cargo c WHERE c.tenant_id=1 AND c.id BETWEEN 930001 AND 930020 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=c.order_id)
UNION ALL SELECT '02 transportOrder->order', COUNT(*) FROM work_transport_order t WHERE t.tenant_id=1 AND t.id BETWEEN 920001 AND 920020 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=t.order_id)
UNION ALL SELECT '03 totLink->transportOrder', COUNT(*) FROM work_transport_order_task x WHERE x.tenant_id=1 AND x.id BETWEEN 940001 AND 940007 AND NOT EXISTS(SELECT 1 FROM work_transport_order t WHERE t.id=x.transport_order_id)
UNION ALL SELECT '04 totLink->task', COUNT(*) FROM work_transport_order_task x WHERE x.tenant_id=1 AND x.id BETWEEN 940001 AND 940007 AND NOT EXISTS(SELECT 1 FROM work_task_transport k WHERE k.id=x.task_id)
UNION ALL SELECT '05 driverJob->task', COUNT(*) FROM work_driver_job j WHERE j.tenant_id=1 AND j.id BETWEEN 9801 AND 9811 AND NOT EXISTS(SELECT 1 FROM work_task_transport k WHERE k.id=j.task_transport_id)
UNION ALL SELECT '06 driverJob->driver', COUNT(*) FROM work_driver_job j WHERE j.tenant_id=1 AND j.id BETWEEN 9801 AND 9811 AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=j.driver_id)
UNION ALL SELECT '07 pickup->order', COUNT(*) FROM work_pickup_dispatch_task p WHERE p.tenant_id=1 AND p.id BETWEEN 950001 AND 950030 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=p.order_id)
UNION ALL SELECT '08 pickup->courier', COUNT(*) FROM work_pickup_dispatch_task p WHERE p.tenant_id=1 AND p.id BETWEEN 950001 AND 950030 AND p.courier_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=p.courier_id)
UNION ALL SELECT '09 task.truck->truck', COUNT(*) FROM work_task_transport k WHERE k.tenant_id=1 AND k.id BETWEEN 9701 AND 9710 AND k.truck_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM base_truck b WHERE b.id=k.truck_id)
UNION ALL SELECT '10 task.trips->trips', COUNT(*) FROM work_task_transport k WHERE k.tenant_id=1 AND k.id BETWEEN 9701 AND 9710 AND k.trips_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM base_transport_trips s WHERE s.id=k.trips_id)
UNION ALL SELECT '11 classifyOrder->classify', COUNT(*) FROM disp_classify_order co WHERE co.tenant_id=1 AND co.id BETWEEN 7101 AND 7107 AND NOT EXISTS(SELECT 1 FROM disp_order_classify c WHERE c.id=co.classify_id)
UNION ALL SELECT '12 classifyOrder->order', COUNT(*) FROM disp_classify_order co WHERE co.tenant_id=1 AND co.id BETWEEN 7101 AND 7107 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=co.order_id)
UNION ALL SELECT '13 attach->classify', COUNT(*) FROM disp_classify_attach a WHERE a.tenant_id=1 AND a.id BETWEEN 7201 AND 7207 AND NOT EXISTS(SELECT 1 FROM disp_order_classify c WHERE c.id=a.classify_id)
UNION ALL SELECT '14 attach.trips->trips', COUNT(*) FROM disp_classify_attach a WHERE a.tenant_id=1 AND a.id BETWEEN 7201 AND 7207 AND a.trips_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM base_transport_trips s WHERE s.id=a.trips_id)
UNION ALL SELECT '15 attach.truck->truck', COUNT(*) FROM disp_classify_attach a WHERE a.tenant_id=1 AND a.id BETWEEN 7201 AND 7207 AND a.truck_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM base_truck b WHERE b.id=a.truck_id)
UNION ALL SELECT '16 attach.driver->driver', COUNT(*) FROM disp_classify_attach a WHERE a.tenant_id=1 AND a.id BETWEEN 7201 AND 7207 AND a.driver_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=a.driver_id)
UNION ALL SELECT '17 cacheDetail->cacheLine', COUNT(*) FROM disp_cache_line_detail d WHERE d.tenant_id=1 AND d.id BETWEEN 6101 AND 6109 AND NOT EXISTS(SELECT 1 FROM disp_cache_line l WHERE l.id=d.cache_line_id)
UNION ALL SELECT '18 cacheDetail->line', COUNT(*) FROM disp_cache_line_detail d WHERE d.tenant_id=1 AND d.id BETWEEN 6101 AND 6109 AND NOT EXISTS(SELECT 1 FROM base_transport_line l WHERE l.id=d.transport_line_id)
UNION ALL SELECT '19 cacheUse->cacheLine', COUNT(*) FROM disp_cache_line_use u WHERE u.tenant_id=1 AND u.id BETWEEN 6201 AND 6207 AND NOT EXISTS(SELECT 1 FROM disp_cache_line l WHERE l.id=u.cache_line_id)
UNION ALL SELECT '20 cacheUse->classify', COUNT(*) FROM disp_cache_line_use u WHERE u.tenant_id=1 AND u.id BETWEEN 6201 AND 6207 AND NOT EXISTS(SELECT 1 FROM disp_order_classify c WHERE c.id=u.classify_id)
UNION ALL SELECT '21 iotTruck->truck', COUNT(*) FROM iot_truck_location g WHERE g.tenant_id=1 AND g.id BETWEEN 92101 AND 92230 AND g.business_type=1 AND NOT EXISTS(SELECT 1 FROM base_truck b WHERE b.id=g.business_id)
UNION ALL SELECT '22 iotCourier->user', COUNT(*) FROM iot_truck_location g WHERE g.tenant_id=1 AND g.id BETWEEN 92101 AND 92230 AND g.business_type=2 AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=g.business_id)
UNION ALL SELECT '23 alarm.truck->truck', COUNT(*) FROM iot_alarm_record a WHERE a.tenant_id=1 AND a.id BETWEEN 93101 AND 93112 AND a.truck_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM base_truck b WHERE b.id=a.truck_id)
UNION ALL SELECT '24 alarm.driver->user', COUNT(*) FROM iot_alarm_record a WHERE a.tenant_id=1 AND a.id BETWEEN 93101 AND 93112 AND a.driver_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=a.driver_id)
UNION ALL SELECT '25 order.currentOrg->org', COUNT(*) FROM oms_order o WHERE o.tenant_id=1 AND o.id BETWEEN 910001 AND 910020 AND o.current_org_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=o.current_org_id)
UNION ALL SELECT '26 order.member->member', COUNT(*) FROM oms_order o WHERE o.tenant_id=1 AND o.id BETWEEN 910001 AND 910020 AND NOT EXISTS(SELECT 1 FROM member_info m WHERE m.id=o.member_id)
UNION ALL SELECT '27 exception->order', COUNT(*) FROM disp_exception_order e WHERE e.tenant_id=1 AND e.id BETWEEN 6301 AND 6305 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=e.order_id)
UNION ALL SELECT '28 exception.org->org', COUNT(*) FROM disp_exception_order e WHERE e.tenant_id=1 AND e.id BETWEEN 6301 AND 6305 AND e.org_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=e.org_id)
UNION ALL SELECT '29 classify.startEndOrg->org', COUNT(*) FROM disp_order_classify c WHERE c.tenant_id=1 AND c.id BETWEEN 701 AND 707 AND (NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=c.start_org_id) OR NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=c.end_org_id))
UNION ALL SELECT '30 payment->order', COUNT(*) FROM pd_payment_order p WHERE p.tenant_id=1 AND p.id LIKE 'PAY9100%' AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=p.order_id)
UNION ALL SELECT '31 settlement->order', COUNT(*) FROM oms_settlement_order s WHERE s.tenant_id=1 AND s.id BETWEEN 960001 AND 960017 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=s.order_id)
UNION ALL SELECT '32 location->order', COUNT(*) FROM oms_order_location l WHERE l.tenant_id=1 AND l.id BETWEEN 912001 AND 912020 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=l.order_id)
UNION ALL SELECT '33 location.sendOrg->org', COUNT(*) FROM oms_order_location l WHERE l.tenant_id=1 AND l.id BETWEEN 912001 AND 912020 AND l.send_org_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=l.send_org_id)
UNION ALL SELECT '34 location.recvOrg->org', COUNT(*) FROM oms_order_location l WHERE l.tenant_id=1 AND l.id BETWEEN 912001 AND 912020 AND l.receive_org_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_org g WHERE g.id=l.receive_org_id)
UNION ALL SELECT '35 freight->order', COUNT(*) FROM oms_freight_detail f WHERE f.tenant_id=1 AND f.id BETWEEN 913001 AND 913020 AND NOT EXISTS(SELECT 1 FROM oms_order o WHERE o.id=f.order_id)
UNION ALL SELECT '36 freight->transportOrder', COUNT(*) FROM oms_freight_detail f WHERE f.tenant_id=1 AND f.id BETWEEN 913001 AND 913020 AND f.transport_order_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM work_transport_order t WHERE t.id=f.transport_order_id)
UNION ALL SELECT '37 history->taskTransport', COUNT(*) FROM work_status_transition_history h WHERE h.tenant_id=1 AND h.id BETWEEN 990001 AND 990020 AND NOT EXISTS(SELECT 1 FROM work_task_transport k WHERE k.id=h.business_id)
UNION ALL SELECT '38 ttd->trips', COUNT(*) FROM base_trips_truck_driver d WHERE d.tenant_id=1 AND d.id BETWEEN 8001 AND 8016 AND NOT EXISTS(SELECT 1 FROM base_transport_trips s WHERE s.id=d.trips_id)
UNION ALL SELECT '39 ttd->truck', COUNT(*) FROM base_trips_truck_driver d WHERE d.tenant_id=1 AND d.id BETWEEN 8001 AND 8016 AND NOT EXISTS(SELECT 1 FROM base_truck b WHERE b.id=d.truck_id)
UNION ALL SELECT '40 ttd->driver', COUNT(*) FROM base_trips_truck_driver d WHERE d.tenant_id=1 AND d.id BETWEEN 8001 AND 8016 AND NOT EXISTS(SELECT 1 FROM sys_user u WHERE u.id=d.driver_id);

-- 【4】订单 12 状态分布（status 1..12 应各有数据；合计 20）
SELECT status, COUNT(*) cnt FROM oms_order WHERE tenant_id=1 AND id BETWEEN 910001 AND 910020 AND deleted=0 GROUP BY status ORDER BY status;

-- 【5】未处理告警（应 6 条：超速1/离线2/货损1/地址错1/延误1）
SELECT alarm_type, COUNT(*) cnt FROM iot_alarm_record WHERE tenant_id=1 AND id BETWEEN 93101 AND 93112 AND status=0 GROUP BY alarm_type ORDER BY alarm_type;
