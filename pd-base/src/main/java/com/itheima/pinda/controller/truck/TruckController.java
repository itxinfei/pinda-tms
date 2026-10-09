package com.itheima.pinda.controller.truck;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.entity.agency.PdFleet;
import com.itheima.pinda.entity.truck.PdTruck;
import com.itheima.pinda.service.agency.IPdFleetService;
import com.itheima.pinda.service.truck.IPdTruckService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TruckController
 */
@RestController
@RequestMapping("base/truck")
public class TruckController {
    @Autowired
    private IPdTruckService truckService;

    @Autowired
    private IPdFleetService fleetService;

    /**
     * 添加车辆
     *
     * @param dto 车辆信息
     * @return 车辆信息
     */
    @PostMapping("")
    public TruckDto saveTruck(@RequestBody TruckDto dto) {
        PdTruck pdTruck = new PdTruck();
        BeanUtils.copyProperties(dto, pdTruck);
        pdTruck = truckService.saveTruck(pdTruck);
        BeanUtils.copyProperties(pdTruck, dto);
        return dto;
    }

    /**
     * 根据id获取车辆详情
     *
     * @param id 车辆id
     * @return 车辆信息
     */
    @GetMapping("/{id}")
    public TruckDto fineById(@PathVariable(name = "id") String id) {
        PdTruck pdTruck = truckService.getById(id);
        if (ObjectUtils.isEmpty(pdTruck)) {
            return null;
        }
        TruckDto dto = new TruckDto();
        BeanUtils.copyProperties(pdTruck, dto);
        return dto;
    }

    /**
     * 获取车辆分页数据
     *
     * @param page         页码
     * @param pageSize     页尺寸
     * @param truckTypeId  车辆类型id
     * @param licensePlate 车牌号码
     * @param fleetId      车队id
     * @param fleetName    车队名称（可选，与fleetId互斥，按名称模糊查询）
     * @return 车辆分页数据
     */
    @GetMapping("/page")
    public PageResponse<TruckDto> findByPage(@RequestParam(name = "page") Integer page,
                                             @RequestParam(name = "pageSize") Integer pageSize,
                                             @RequestParam(name = "truckTypeId", required = false) String truckTypeId,
                                             @RequestParam(name = "licensePlate", required = false) String licensePlate,
                                             @RequestParam(name = "fleetId", required = false) String fleetId,
                                             @RequestParam(name = "fleetName", required = false) String fleetName) {
        IPage<PdTruck> truckPage;
        if (StringUtils.isNotBlank(fleetName) && StringUtils.isBlank(fleetId)) {
            // 按车队名称模糊查询：先查匹配的车队，再按车队ID列表查车辆
            IPage<PdFleet> fleetPage = fleetService.findByPage(1, 1000, fleetName, null, null);
            List<String> fleetIds = fleetPage.getRecords().stream()
                    .map(PdFleet::getId)
                    .collect(Collectors.toList());
            truckPage = truckService.findByPageByFleetIds(page, pageSize, truckTypeId, licensePlate, fleetIds);
        } else {
            truckPage = truckService.findByPage(page, pageSize, truckTypeId, licensePlate, fleetId);
        }
        List<TruckDto> dtoList = new ArrayList<>();
        truckPage.getRecords().forEach(pdTruck -> {
            TruckDto dto = new TruckDto();
            BeanUtils.copyProperties(pdTruck, dto);
            dtoList.add(dto);
        });
        return PageResponse.<TruckDto>builder().items(dtoList).pagesize(pageSize).page(page).counts(truckPage.getTotal())
                .pages(truckPage.getPages()).build();
    }

    /**
     * 统计车辆数量
     *
     * @param fleetId 车队id
     * @return 车辆数量
     */
    @GetMapping("/count")
    public Integer count(@RequestParam(name = "fleetId", required = false) String fleetId) {
        return truckService.count(fleetId);
    }

    /**
     * 获取车辆列表
     *
     * @param ids 车辆id列表
     * @return 车辆列表
     */
    @GetMapping("")
    public List<TruckDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids, @RequestParam(name = "fleetId", required = false) String fleetId) {
        return truckService.findAll(ids, fleetId).stream().map(pdTruck -> {
            TruckDto dto = new TruckDto();
            BeanUtils.copyProperties(pdTruck, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    /**
     * 更新车辆信息
     *
     * @param id  车辆id
     * @param dto 车辆信息
     * @return 车辆信息
     */
    @PutMapping("/{id}")
    public TruckDto update(@PathVariable(name = "id") String id, @RequestBody TruckDto dto) {
        dto.setId(id);
        PdTruck pdTruck = new PdTruck();
        BeanUtils.copyProperties(dto, pdTruck);
        truckService.updateById(pdTruck);
        return dto;
    }

    /**
     * 删除车辆（逻辑删除：置为禁用状态）
     *
     * @param id 车辆id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        // 删除前检查车辆当前状态：已禁用/不存在时不允许重复操作
        PdTruck pdTruck = truckService.getById(id);
        if (ObjectUtils.isEmpty(pdTruck)) {
            return Result.error(400, "车辆不存在");
        }
        if (com.itheima.pinda.common.utils.Constant.DATA_DISABLE_STATUS.equals(pdTruck.getStatus())) {
            return Result.error(400, "车辆已处于禁用状态，请勿重复操作");
        }
        // 非空闲状态校验：在途运输任务校验已由管理端(web-manager deleteTruck)在删除前拦截，
        // 此处完成基础状态(存在性/重复禁用)校验
        truckService.disableById(id);
        return Result.ok();
    }

    /**
     * 更新车辆在线状态与心跳时间（P0-4 心跳键错配修复）
     *
     * <p>由 pd-netty GpsTraceConsumer 收到 GPS 上报后调用：
     * ① 收到 type=truck 的 GPS 上报 → online_status=1 + last_heartbeat_time=服务端now()
     * ② pd-netty 每 60s 扫描心跳超时（>5min 无上报）的车辆置 online_status=0</p>
     *
     * <p>注意：HTTP 查询参数名保留为 {@code deviceGpsId} 以兼容 pd-service-base-api 中
     * 既有的 TruckFeign 接口（本次不允许修改 pd-service-api），但其值的语义是
     * <b>车辆主键 id</b>，不是 GPS 设备号。</p>
     *
     * @param deviceGpsId 车辆主键 id（参数名仅为兼容保留）
     * @param heartbeatTime 心跳时间（ISO 格式，由 pd-netty 服务端生成）
     * @return 更新结果
     */
    @PutMapping("/heartbeat")
    public Result updateHeartbeat(@RequestParam(name = "deviceGpsId") String deviceGpsId,
                                  @RequestParam(name = "heartbeatTime")
                                  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                                  LocalDateTime heartbeatTime) {
        boolean ok = truckService.updateHeartbeat(deviceGpsId, heartbeatTime);
        return ok ? Result.ok() : Result.error(400, "车辆不存在或已禁用");
    }

    /**
     * 批量将心跳超时车辆置为离线
     *
     * <p>由 pd-netty @Scheduled 定时任务调用。</p>
     *
     * @param threshold 心跳超时阈值（ISO 格式）
     * @return 受影响行数
     */
    @PutMapping("/heartbeat/mark-offline")
    public Result markOffline(@RequestParam(name = "threshold")
                              @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                              LocalDateTime threshold) {
        int affected = truckService.markOfflineByHeartbeat(threshold);
        return Result.ok().put("data", affected);
    }
}