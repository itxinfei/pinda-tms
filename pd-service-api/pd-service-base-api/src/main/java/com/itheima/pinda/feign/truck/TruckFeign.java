package com.itheima.pinda.feign.truck;

import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.feign.truck.hystrix.TruckFeignFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.time.LocalDateTime;
import java.util.List;

@FeignClient(value = "pd-base", fallback = TruckFeignFallback.class, path = "/base/truck")
@ApiIgnore
public interface TruckFeign {
    /**
     * 添加车辆
     *
     * @param dto 车辆信息
     * @return 车辆信息
     */
    @PostMapping("")
    TruckDto saveTruck(@RequestBody TruckDto dto);

    /**
     * 根据id获取车辆详情
     *
     * @param id 车辆id
     * @return 车辆信息
     */
    @GetMapping("/{id}")
    TruckDto fineById(@PathVariable(name = "id") String id);

    /**
     * 获取车辆分页数据
     *
     * @param page         页码
     * @param pageSize     页尺寸
     * @param truckTypeId  车辆类型id
     * @param licensePlate 车牌好吗
     * @return 车辆分页数据
     */
    @GetMapping("/page")
    PageResponse<TruckDto> findByPage(@RequestParam(name = "page") Integer page,
                                      @RequestParam(name = "pageSize") Integer pageSize,
                                      @RequestParam(name = "truckTypeId", required = false) String truckTypeId,
                                      @RequestParam(name = "licensePlate", required = false) String licensePlate,
                                      @RequestParam(name = "fleetId", required = false) String fleetId);

    /**
     * 获取车辆列表
     *
     * @param ids     车辆id列表
     * @param fleetId 车队id
     * @return 车辆列表
     */
    @GetMapping("")
    List<TruckDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids, @RequestParam(name = "fleetId", required = false) String fleetId);

    /**
     * 更新车辆信息
     *
     * @param id  车辆id
     * @param dto 车辆信息
     * @return 车辆信息
     */
    @PutMapping("/{id}")
    TruckDto update(@PathVariable(name = "id") String id, @RequestBody TruckDto dto);

    /**
     * 统计车辆数量
     *
     * @param fleetId 车队id
     * @return 车辆数量
     */
    @GetMapping("/count")
    Integer count(@RequestParam(name = "fleetId", required = false) String fleetId);

    /**
     * 删除车辆
     *
     * @param id 车辆id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    Result disable(@PathVariable(name = "id") String id);

    /**
     * 更新车辆在线状态与心跳时间（P0-4 北斗字段最小改造 · D-13）
     *
     * <p>由 pd-netty GpsTraceConsumer 收到 GPS 上报后调用：
     * online_status=1 并刷新 last_heartbeat_time。</p>
     *
     * @param deviceGpsId GPS 设备 id
     * @param heartbeatTime 心跳时间
     * @return 更新结果
     */
    @PutMapping("/heartbeat")
    Result updateHeartbeat(@RequestParam(name = "deviceGpsId") String deviceGpsId,
                           @RequestParam(name = "heartbeatTime") LocalDateTime heartbeatTime);

    /**
     * 批量将心跳超时车辆置为离线（P0-4 北斗字段最小改造 · D-13）
     *
     * @param threshold 心跳超时阈值
     * @return 更新结果（data 字段返回受影响行数）
     */
    @PutMapping("/heartbeat/mark-offline")
    Result markOffline(@RequestParam(name = "threshold") LocalDateTime threshold);
}
