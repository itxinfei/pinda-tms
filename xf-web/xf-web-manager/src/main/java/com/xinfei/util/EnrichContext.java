package com.xinfei.util;

import com.xinfei.DTO.AreaDTO;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.DTO.OrgDTO;
import com.xinfei.DTO.TransportOrderDTO;
import com.xinfei.DTO.UserDTO;
import com.xinfei.DTO.transportline.TransportTripsDto;
import com.xinfei.DTO.truck.TruckDto;
import com.xinfei.feign.AreaFeign;
import com.xinfei.feign.DriverJobFeign;
import com.xinfei.feign.OrderFeign;
import com.xinfei.feign.OrgFeign;
import com.xinfei.feign.TransportOrderFeign;
import com.xinfei.feign.UserFeign;
import com.xinfei.feign.transportline.TransportTripsFeign;
import com.xinfei.feign.truck.TruckFeign;
import com.xinfei.vo.base.userCenter.SysUserVo;

import java.util.List;
import java.util.Map;

/**
 * 列表富化上下文（巡检报告 P0-1）。
 *
 * <p>统一承载各 Feign 客户端与调用方「批量预取」得到的 Map 缓存，
 * 把原本逐行远程查询的 {@code parse*} 方法改为「一次批量预取 → 行内查 Map」，
 * 消除分页接口的三重嵌套 N+1。</p>
 *
 * <p>约定：Map 字段为 {@code null} 表示该项调用方未预取，方法回退到逐行远程查
 * （与旧签名行为等价），因此尚未迁移的调用点（TransportTaskController /
 * TransportOrderController）可继续走旧签名，互不干扰。Map 非 null 即视为已预取，
 * 直接 {@code get}（缺失即返回 null，不再回源）。</p>
 */
public final class EnrichContext {

    final TransportTripsFeign transportTripsFeign;
    final OrgFeign orgFeign;
    final UserFeign userFeign;
    final TruckFeign truckFeign;
    final TransportOrderFeign transportOrderFeign;
    final OrderFeign orderFeign;
    final AreaFeign areaFeign;
    final DriverJobFeign driverJobFeign;

    final Map<Long, OrgDTO> orgMap;
    final Map<Long, UserDTO> userMap;
    // 车次/车辆/订单的 Feign 主键为 String（fineById/findById 入参即 String），故用 String 作 key
    final Map<String, TransportTripsDto> tripsMap;
    final Map<String, TruckDto> truckMap;
    final Map<Long, TransportOrderDTO> transportOrderMap;
    final Map<String, OrderDTO> orderMap;
    final Map<Long, AreaDTO> areaMap;
    final Map<Long, List<SysUserVo>> driversByTaskId;

    private EnrichContext(Builder b) {
        this.transportTripsFeign = b.transportTripsFeign;
        this.orgFeign = b.orgFeign;
        this.userFeign = b.userFeign;
        this.truckFeign = b.truckFeign;
        this.transportOrderFeign = b.transportOrderFeign;
        this.orderFeign = b.orderFeign;
        this.areaFeign = b.areaFeign;
        this.driverJobFeign = b.driverJobFeign;
        this.orgMap = b.orgMap;
        this.userMap = b.userMap;
        this.tripsMap = b.tripsMap;
        this.truckMap = b.truckMap;
        this.transportOrderMap = b.transportOrderMap;
        this.orderMap = b.orderMap;
        this.areaMap = b.areaMap;
        this.driversByTaskId = b.driversByTaskId;
    }

    public static Builder builder(TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign,
                                  TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign,
                                  AreaFeign areaFeign, DriverJobFeign driverJobFeign) {
        return new Builder(transportTripsFeign, orgFeign, userFeign, truckFeign, transportOrderFeign, orderFeign, areaFeign, driverJobFeign);
    }

    public static final class Builder {
        private final TransportTripsFeign transportTripsFeign;
        private final OrgFeign orgFeign;
        private final UserFeign userFeign;
        private final TruckFeign truckFeign;
        private final TransportOrderFeign transportOrderFeign;
        private final OrderFeign orderFeign;
        private final AreaFeign areaFeign;
        private final DriverJobFeign driverJobFeign;

        private Map<Long, OrgDTO> orgMap;
        private Map<Long, UserDTO> userMap;
        private Map<String, TransportTripsDto> tripsMap;
        private Map<String, TruckDto> truckMap;
        private Map<Long, TransportOrderDTO> transportOrderMap;
        private Map<String, OrderDTO> orderMap;
        private Map<Long, AreaDTO> areaMap;
        private Map<Long, List<SysUserVo>> driversByTaskId;

        private Builder(TransportTripsFeign transportTripsFeign, OrgFeign orgFeign, UserFeign userFeign,
                        TruckFeign truckFeign, TransportOrderFeign transportOrderFeign, OrderFeign orderFeign,
                        AreaFeign areaFeign, DriverJobFeign driverJobFeign) {
            this.transportTripsFeign = transportTripsFeign;
            this.orgFeign = orgFeign;
            this.userFeign = userFeign;
            this.truckFeign = truckFeign;
            this.transportOrderFeign = transportOrderFeign;
            this.orderFeign = orderFeign;
            this.areaFeign = areaFeign;
            this.driverJobFeign = driverJobFeign;
        }

        public Builder orgMap(Map<Long, OrgDTO> v) { this.orgMap = v; return this; }
        public Builder userMap(Map<Long, UserDTO> v) { this.userMap = v; return this; }
        public Builder tripsMap(Map<String, TransportTripsDto> v) { this.tripsMap = v; return this; }
        public Builder truckMap(Map<String, TruckDto> v) { this.truckMap = v; return this; }
        public Builder transportOrderMap(Map<Long, TransportOrderDTO> v) { this.transportOrderMap = v; return this; }
        public Builder orderMap(Map<String, OrderDTO> v) { this.orderMap = v; return this; }
        public Builder areaMap(Map<Long, AreaDTO> v) { this.areaMap = v; return this; }
        public Builder driversByTaskId(Map<Long, List<SysUserVo>> v) { this.driversByTaskId = v; return this; }

        public EnrichContext build() {
            return new EnrichContext(this);
        }
    }
}
