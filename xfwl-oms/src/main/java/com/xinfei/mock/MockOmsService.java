package com.xinfei.mock;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xinfei.common.context.TenantContextHolder;
import com.xinfei.entity.Order;
import com.xinfei.entity.OrderCargo;
import com.xinfei.entity.OrderLocation;
import com.xinfei.enums.OrderPaymentMethod;
import com.xinfei.enums.OrderPaymentStatus;
import com.xinfei.enums.OrderPickupType;
import com.xinfei.enums.OrderStatus;
import com.xinfei.enums.OrderType;
import com.xinfei.service.IOrderCargoService;
import com.xinfei.service.IOrderLocationService;
import com.xinfei.service.IOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * mock 订单数据生成（xfwl-oms 域：订单 + 货物 + 坐标 + 订单异常）。
 *
 * <p>设计口径（2026-10-19 拍板）：手动触发、绕过计价引擎直插（不依赖百度 AK）、
 * 全部数据 remark 前缀 {@code AUTO-MOCK} 可识别可清理；订单绑定演示会员
 * 9301~9303（customer01~03 登录客户端即可见）；create_time 按近 N 天分布
 * （当天占比一半），仪表盘"今日新增/近 7 日趋势"有数。</p>
 *
 * <p>跨域只读：goodsTypeId 经 JdbcTemplate 查 base_goods_type（只读不写，不违反模块边界）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Conditional(MockGuardCondition.class)
public class MockOmsService {

    /** 数据标记前缀（清理与下游 mock 关联的唯一依据，禁改） */
    public static final String MOCK_MARK = "AUTO-MOCK";

    /** 演示会员（sys_user.customer01~03 与 member_info id 同值种子对齐，客户端登录可见） */
    private static final long[] MEMBER_IDS = {9301L, 9302L, 9303L};

    /** 演示网点（武汉江岸/武昌，见 sys_org 种子） */
    private static final long[] ORG_IDS = {1001L, 1002L};

    /** 同城收发地：武汉（dict_area 码 420000/420100/420102，种子风格） */
    private static final int SENDER_PROVINCE = 420000, SENDER_CITY = 420100, SENDER_COUNTY = 420102;
    /** 城际目的地：成都（510000/510100/510107 武侯区） */
    private static final int RECEIVER_PROVINCE = 510000, RECEIVER_CITY = 510100, RECEIVER_COUNTY = 510107;

    private static final String[] SURNAMES = {"张", "王", "李", "刘", "陈", "杨", "赵", "黄", "周", "吴", "徐", "孙", "马", "朱", "胡", "郭"};
    private static final String[] GIVEN_NAMES = {"伟", "芳", "娜", "洋", "静", "帆", "磊", "敏", "杰", "倩", "强", "丽", "超", "婷", "军", "欣"};
    private static final String[] STREETS = {"解放大道", "建设大道", "中山路", "和平大道", "发展大道", "武珞路", "光谷大道", "关山大道"};
    private static final String[] CARGO_NAMES = {"服装", "电子产品", "食品", "日用品", "书籍", "小家电", "玩具", "化妆品"};

    private final IOrderService orderService;
    private final IOrderCargoService orderCargoService;
    private final IOrderLocationService orderLocationService;
    private final DataSource dataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 生成 mock 订单（含货物/坐标/少量异常）
     *
     * @param count    本批条数（Controller 已限幅 1~200）
     * @param daysBack 历史回溯天数（含今天共 daysBack+1 天，当天占一半）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generate(int count, int daysBack) {
        // 系统态租户上下文：docker exec 直调无租户头，多租户插件会回落 tenant_id=0
        // 导致登录租户（1）查不到 mock 数据——必须显式置 1（惯例见 RabbitGpsTraceConsumer）
        TenantContextHolder.setTenantId(1L);
        try {
            return doGenerate(count, daysBack);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private Map<String, Object> doGenerate(int count, int daysBack) {
        List<Long> goodsTypeIds = jdbc().queryForList(
                "SELECT id FROM base_goods_type WHERE deleted = 0 AND status = 1", Long.class);
        if (goodsTypeIds.isEmpty()) {
            throw new IllegalStateException("base_goods_type 无可用种子，先执行 sql/base 种子脚本");
        }

        int todayCount = Math.max(1, count / 2);
        int historyCount = count - todayCount;
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        LocalDateTime now = LocalDateTime.now();

        int created = 0, interCity = 0, exceptions = 0;
        // 天序 0=今天，1=昨天……按 [今天一半 + 历史均分] 分配
        for (int i = 0; i < count; i++) {
            int dayOffset;
            int remain = i - todayCount;
            if (i < todayCount) {
                dayOffset = 0;
            } else if (daysBack > 0) {
                dayOffset = 1 + remain % daysBack;
            } else {
                dayOffset = 0;
            }
            // 历史单时间落在当天 9~19 点，今天单落在已过去的时段（8 点起）
            int hour = dayOffset == 0 ? 8 + rnd.nextInt(Math.max(1, now.getHour() - 8)) : 9 + rnd.nextInt(10);
            LocalDateTime createTime = now.toLocalDate().minusDays(dayOffset).atTime(Math.max(8, hour), rnd.nextInt(60));

            boolean isInterCity = rnd.nextInt(10) < 3; // 30% 城际单（供 xfwl-work 造干线任务）
            Order order = buildOrder(rnd, createTime, isInterCity);
            orderService.save(order);
            created++;
            if (isInterCity) {
                interCity++;
            }

            int cargoRows = 1 + rnd.nextInt(2);
            for (int c = 0; c < cargoRows; c++) {
                orderCargoService.save(buildCargo(rnd, order.getId(), goodsTypeIds, createTime));
            }
            orderLocationService.save(buildLocation(rnd, order.getId(), isInterCity, createTime));

            // 今天的单 ~20% 挂一条待处理异常（管理端"订单异常"页有活干）
            if (dayOffset == 0 && rnd.nextInt(5) == 0) {
                insertOrderException(rnd, order.getId(), createTime);
                exceptions++;
            }
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("created", created);
        stats.put("interCity", interCity);
        stats.put("exceptions", exceptions);
        stats.put("daysBack", daysBack);
        log.info("[mock] oms 生成完成: {}", stats);
        return stats;
    }

    private Order buildOrder(ThreadLocalRandom rnd, LocalDateTime createTime, boolean isInterCity) {
        Order order = new Order();
        // 单号与 id 同值（实体注释口径）；ASSIGN_ID 下显式 setId 不会被 MP 覆盖
        long id = IdWorker.getId();
        order.setId(id);
        order.setOrderNo(String.valueOf(id));
        order.setOrderType(isInterCity ? OrderType.OUTCITY.getCode() : OrderType.INCITY.getCode());
        order.setPickupType(rnd.nextBoolean()
                ? OrderPickupType.NO_PICKUP.getCode()
                : OrderPickupType.PICKUP.getCode());
        // 当天单取活跃态，历史单允许走到签收（趋势/状态分布有层次）
        boolean isToday = createTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
        int[] pool = isToday
                ? activeStatusPool(rnd)
                : new int[]{OrderStatus.IN_TRANSIT.getCode(), OrderStatus.DISPATCHING.getCode(), OrderStatus.RECEIVED.getCode()};
        order.setStatus(pool[rnd.nextInt(pool.length)]);
        order.setMemberId(MEMBER_IDS[rnd.nextInt(MEMBER_IDS.length)]);
        order.setSenderName(randomName(rnd));
        order.setSenderPhone(randomPhone(rnd));
        order.setSenderProvinceId(SENDER_PROVINCE);
        order.setSenderCityId(SENDER_CITY);
        order.setSenderCountyId(SENDER_COUNTY);
        order.setSenderAddress(randomStreet(rnd, "武汉"));
        order.setReceiverName(randomName(rnd));
        order.setReceiverPhone(randomPhone(rnd));
        order.setReceiverProvinceId(RECEIVER_PROVINCE);
        order.setReceiverCityId(RECEIVER_CITY);
        order.setReceiverCountyId(RECEIVER_COUNTY);
        order.setReceiverAddress(randomStreet(rnd, "成都"));
        order.setCurrentOrgId(ORG_IDS[rnd.nextInt(ORG_IDS.length)]);
        order.setPaymentMethod(rnd.nextBoolean()
                ? OrderPaymentMethod.PRE_PAY.getCode()
                : OrderPaymentMethod.END_PAY.getCode());
        order.setPaymentStatus(rnd.nextBoolean()
                ? OrderPaymentStatus.UNPAID.getStatus()
                : OrderPaymentStatus.PAID.getStatus());
        BigDecimal distance = isInterCity
                ? BigDecimal.valueOf(1000 + rnd.nextInt(500))
                : BigDecimal.valueOf(5 + rnd.nextInt(25));
        order.setDistance(distance);
        order.setAmount(BigDecimal.valueOf(15 + rnd.nextInt(150)));
        order.setEstimatedArrivalTime(createTime.plusDays(1 + rnd.nextInt(3)));
        order.setRemark(MOCK_MARK);
        order.setCreateTime(createTime);
        order.setUpdateTime(createTime);
        return order;
    }

    /** 当天单状态池：待取件/已取件/入库/待装车/运输中/待派送/派送中（1~9 活跃态） */
    private int[] activeStatusPool(ThreadLocalRandom rnd) {
        return new int[]{
                OrderStatus.PENDING.getCode(), OrderStatus.PICKED_UP.getCode(),
                OrderStatus.OUTLETS_WAREHOUSE.getCode(), OrderStatus.FOR_LOADING.getCode(),
                OrderStatus.IN_TRANSIT.getCode(), OrderStatus.TO_BE_DISPATCHED.getCode(),
                OrderStatus.DISPATCHING.getCode()};
    }

    private OrderCargo buildCargo(ThreadLocalRandom rnd, Long orderId, List<Long> goodsTypeIds, LocalDateTime createTime) {
        OrderCargo cargo = new OrderCargo();
        cargo.setOrderId(orderId);
        cargo.setGoodsTypeId(goodsTypeIds.get(rnd.nextInt(goodsTypeIds.size())));
        cargo.setName(CARGO_NAMES[rnd.nextInt(CARGO_NAMES.length)]);
        cargo.setUnit("件");
        int quantity = 1 + rnd.nextInt(5);
        cargo.setQuantity(quantity);
        BigDecimal weight = BigDecimal.valueOf(1 + rnd.nextInt(20));
        BigDecimal volume = BigDecimal.valueOf(rnd.nextDouble(0.5) + 0.1).setScale(2, RoundingMode.HALF_UP);
        cargo.setWeight(weight);
        cargo.setVolume(volume);
        cargo.setTotalWeight(weight.multiply(BigDecimal.valueOf(quantity)));
        cargo.setTotalVolume(volume.multiply(BigDecimal.valueOf(quantity)));
        cargo.setCargoValue(BigDecimal.valueOf(50 + rnd.nextInt(500)));
        cargo.setRemark(MOCK_MARK);
        cargo.setCreateTime(createTime);
        return cargo;
    }

    private OrderLocation buildLocation(ThreadLocalRandom rnd, Long orderId, boolean isInterCity, LocalDateTime createTime) {
        OrderLocation location = new OrderLocation();
        location.setOrderId(orderId);
        // 武汉 114.30,30.59 / 成都 104.06,30.57 附近抖动
        location.setSendLongitude(jitter(rnd, 114.30));
        location.setSendLatitude(jitter(rnd, 30.59));
        location.setReceiveLongitude(jitter(rnd, isInterCity ? 104.06 : 114.32));
        location.setReceiveLatitude(jitter(rnd, isInterCity ? 30.57 : 30.61));
        location.setSendOrgId(ORG_IDS[rnd.nextInt(ORG_IDS.length)]);
        location.setReceiveOrgId(isInterCity ? 204L : ORG_IDS[rnd.nextInt(ORG_IDS.length)]); // 204 成都转运中心
        location.setStatus(1); // 1有效
        location.setCreateTime(createTime);
        return location;
    }

    /** 订单异常直插（实体服务在 web 层，本域用 JdbcTemplate 最小侵入；绕过多租户插件须显式补 tenant_id） */
    private void insertOrderException(ThreadLocalRandom rnd, Long orderId, LocalDateTime createTime) {
        // 异常类型码表：1滞留 2破损（本域无枚举，口径见 sql/oms/V20261017_0930_order_exception.sql 表注释）
        int type = 1 + rnd.nextInt(2);
        String description = type == 1 ? "包裹滞留网点超 24 小时" : "外包装破损，待核验内件";
        jdbc().update(
                "INSERT INTO oms_order_exception (id, tenant_id, order_id, exception_type, description, handle_status, create_time, update_time) "
                        + "VALUES (?, 1, ?, ?, ?, 1, ?, ?)",
                IdWorker.getId(), orderId, type, description, createTime, createTime);
    }

    /**
     * 清理全部 AUTO-MOCK 订单域数据（物理删，先子后主；调用前应已清理 work/netty 引用）。
     * 顺序守护：work 域清理锚点依赖本表 remark，先删 oms 会让 work 域 mock 数据永久变孤儿。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cleanup() {
        JdbcTemplate jdbc = jdbc();
        Integer workRefs = jdbc.queryForObject(
                "SELECT COUNT(*) FROM work_transport_order WHERE order_id IN "
                        + "(SELECT id FROM oms_order WHERE remark LIKE '" + MOCK_MARK + "%')", Integer.class);
        if (workRefs != null && workRefs > 0) {
            throw new IllegalStateException("work 域仍有 AUTO-MOCK 关联数据，请先调 xfwl-work DELETE /mock/all 再清 oms");
        }
        String orderIds = "SELECT id FROM oms_order WHERE remark LIKE '" + MOCK_MARK + "%'";
        int cargo = jdbc.update("DELETE FROM oms_order_cargo WHERE order_id IN (" + orderIds + ")");
        int location = jdbc.update("DELETE FROM oms_order_location WHERE order_id IN (" + orderIds + ")");
        int exception = jdbc.update("DELETE FROM oms_order_exception WHERE order_id IN (" + orderIds + ")");
        int order = jdbc.update("DELETE FROM oms_order WHERE remark LIKE '" + MOCK_MARK + "%'");
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("order", order);
        stats.put("cargo", cargo);
        stats.put("location", location);
        stats.put("exception", exception);
        log.info("[mock] oms 清理完成: {}", stats);
        return stats;
    }

    private String randomName(ThreadLocalRandom rnd) {
        return SURNAMES[rnd.nextInt(SURNAMES.length)] + GIVEN_NAMES[rnd.nextInt(GIVEN_NAMES.length)];
    }

    private String randomPhone(ThreadLocalRandom rnd) {
        return "139" + String.format("%08d", rnd.nextInt(100000000));
    }

    private String randomStreet(ThreadLocalRandom rnd, String city) {
        return city + "市" + STREETS[rnd.nextInt(STREETS.length)] + (10 + rnd.nextInt(200)) + "号";
    }

    private BigDecimal jitter(ThreadLocalRandom rnd, double base) {
        return BigDecimal.valueOf(base + (rnd.nextDouble() - 0.5) * 0.06).setScale(6, RoundingMode.HALF_UP);
    }
}
