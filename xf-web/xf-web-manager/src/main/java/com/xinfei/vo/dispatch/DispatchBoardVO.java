package com.xinfei.vo.dispatch;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 调度一张图聚合数据（FR-04 / DSP-03，《15》§三 后端聚合契约）
 *
 * <p>一次请求返回地图所需的全部静态要素，避免前端串行拉多次：
 * 待调度订单点（按所属网点坐标落点）、可用线路、汇总统计。
 * 车辆实时位置由前端直连 {@code GET /api/netty-service/trace/latestAll} 获取
 * （xf-netty 公开端点，职责单一，不由本聚合转发）。</p>
 */
@Data
public class DispatchBoardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待调度订单（网点入库状态） */
    private List<OrderPoint> orders;

    /** 可用线路（base_transport_line 启用状态） */
    private List<Line> lines;

    /** 汇总统计 */
    private Summary summary;

    /** 待调度订单地图点 */
    @Data
    public static class OrderPoint implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 订单 id */
        private String id;
        /** 订单号 */
        private String orderNo;
        /** 发件人 */
        private String senderName;
        /** 收件人 */
        private String receiverName;
        /** 当前入库网点 id */
        private String currentOrgId;
        /** 当前入库网点名称 */
        private String currentOrgName;
        /** 网点经度（WGS84 落库，前端按地图供应商转换渲染） */
        private String longitude;
        /** 网点纬度 */
        private String latitude;
        /** 下单时间 */
        private String createTime;
        /** 货物件数（OrderCargo 汇总，多条脏数据已求和；右栏载重匹配用） */
        private Integer quantity;
        /** 货物总重 kg（同上） */
        private BigDecimal totalWeight;
        /** 货物总体积 m³（同上） */
        private BigDecimal totalVolume;
    }

    /** 线路 */
    @Data
    public static class Line implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 线路 id */
        private String id;
        /** 线路名称 */
        private String name;
        /** 起始机构 id */
        private String startOrgId;
        /** 目的机构 id */
        private String endOrgId;
        /** 起始机构名称 */
        private String startOrgName;
        /** 目的机构名称 */
        private String endOrgName;
        /** 起始机构经度（WGS84 落库，前端按地图供应商转换渲染） */
        private String startOrgLng;
        /** 起始机构纬度 */
        private String startOrgLat;
        /** 目的机构经度（WGS84） */
        private String endOrgLng;
        /** 目的机构纬度 */
        private String endOrgLat;
        /** 里程（公里） */
        private BigDecimal distance;
        /** 成本 */
        private BigDecimal cost;
        /** 预计时长（与线路档案口径一致，后端以 BigDecimal 承载） */
        private BigDecimal estimatedTime;
    }

    /** 汇总统计 */
    @Data
    public static class Summary implements Serializable {
        private static final long serialVersionUID = 1L;
        /** 待调度订单数 */
        private int pendingOrderCount;
        /** 线路数 */
        private int lineCount;
        /** 覆盖网点数 */
        private int orgCount;
    }
}