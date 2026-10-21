package com.xinfei.feign;

import com.xinfei.common.utils.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * GPS 轨迹内部 Feign（FR-36 离线禁排班），对接 xfwl-netty /trace。
 *
 * <p>复用既有 {@code /trace/latestAll}（各车辆/快递员最新位置，含 reportTime），
 * dispatch 据此判定车辆 GPS 是否在线（gps.offline.timeout.minutes），离线车辆禁入调度。</p>
 */
@FeignClient(name = "xfwl-netty", path = "/trace")
public interface NettyTraceFeign {

    @GetMapping("/latestAll")
    Result latestAll(@RequestParam(value = "type", required = false) String type);
}
