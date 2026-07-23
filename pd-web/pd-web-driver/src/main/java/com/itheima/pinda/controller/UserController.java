package com.itheima.pinda.controller;


import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.DTO.TaskTransportDTO;
import com.itheima.pinda.DTO.UserProfileDTO;
import com.itheima.pinda.DTO.angency.FleetDto;
import com.itheima.pinda.DTO.truck.TruckDto;
import com.itheima.pinda.DTO.user.TruckDriverDto;
import com.itheima.pinda.authority.api.OrgApi;
import com.itheima.pinda.authority.api.UserApi;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.base.R;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.enums.driverjob.DriverJobStatus;
import com.itheima.pinda.feign.DriverJobFeign;
import com.itheima.pinda.feign.TransportTaskFeign;
import com.itheima.pinda.feign.agency.FleetFeign;
import com.itheima.pinda.feign.truck.TruckFeign;
import com.itheima.pinda.feign.user.DriverFeign;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-03-19
 */
@Slf4j
@Api(tags = "用户管理")
@Controller
@RequestMapping("user")
public class UserController {

    @Autowired
    private UserApi userApi;

    @Autowired
    private OrgApi orgApi;

    @Autowired
    private FleetFeign fleetFeign;

    @Autowired
    private DriverFeign driverFeign;

    @Autowired
    private DriverJobFeign driverJobFeign;

    @Autowired
    private TransportTaskFeign transportTaskFeign;

    @Autowired
    private TruckFeign truckFeign;


    @SneakyThrows
    @ApiOperation(value = "我的信息")
    @ResponseBody
    @GetMapping("profile")
    public Result profile() {

        //  获取司机id  并放入参数
        String driverId = RequestContext.getUserId();
        log.info("司机端-登录用户：{}", driverId);
        // 基本信息
        // 修改点：远程调用可能返回 null 包装，统一通过 Rx 安全取值，避免 NPE
        User user = Rx.data(userApi.get(Long.valueOf(driverId)));
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        log.info("司机端-登录用户：{}", user);
        // 司机信息
        TruckDriverDto truckDriverDto = driverFeign.findOneDriver(driverId);
        log.info("司机端-司机信息：{}", truckDriverDto);

        // 司机任务信息
        String truckId = null;
        String licensePlate = null;
        String transportTaskId = null;
        if (truckDriverDto != null) {
            DriverJobDTO driverJobDto = new DriverJobDTO();
            driverJobDto.setStatus(DriverJobStatus.PROCESSING.getCode());
            driverJobDto.setDriverId(driverId);
            List<DriverJobDTO> driverJobDtos = driverJobFeign.findAll(driverJobDto);
            log.info("司机端-在途任务：{}", driverJobDtos);
            if (!CollectionUtils.isEmpty(driverJobDtos)) {
                driverJobDto = driverJobDtos.get(0);
                String taskTransportId = driverJobDto.getTaskTransportId();
                TaskTransportDTO transportTaskDto = transportTaskFeign.findById(taskTransportId);
                log.info("司机端-在途任务详情：{}", transportTaskDto);
                if (transportTaskDto != null) {
                    transportTaskId = transportTaskDto.getId();
                    truckId = transportTaskDto.getTruckId();
                    TruckDto truckDto = truckFeign.fineById(truckId);
                    log.info("司机端-车辆信息：{}", truckDto);
                    if (truckDto != null) {
                        licensePlate = truckDto.getLicensePlate();
                    }
                }
            }
        }
        // 所属机构
        // 修改点：远程调用可能返回 null 包装，统一通过 Rx 安全取值，避免 NPE
        Org org = Rx.data(orgApi.get(user.getOrgId()));
        if (org == null) {
            return Result.error(404, "机构不存在");
        }
        FleetDto fleetDto = null;
        Org fleetOrg = null;
        if (truckDriverDto != null && StringUtils.isNotEmpty(truckDriverDto.getFleetId())) {
            // 车队信息
            fleetDto = fleetFeign.fineById(truckDriverDto.getFleetId());
            log.info("司机端-车队信息：{}", fleetDto);
            // 运转中心
            if (fleetDto != null && StringUtils.isNotEmpty(fleetDto.getAgencyId())) {
                // 修改点：远程调用可能返回 null 包装，统一通过 Rx 安全取值，避免 NPE
                fleetOrg = Rx.data(orgApi.get(Long.valueOf(fleetDto.getAgencyId())));
            }
        }

        return Result.ok().put("data", UserProfileDTO.builder()
                .id(user.getId().toString())
                .avatar(user.getAvatar())
                .name(user.getName())
                .phone(user.getMobile())
                .manager(org.getManager())
                .team(fleetDto != null ? fleetDto.getName() : "")
                .transport(org != null ? org.getName() : "")
                .truckId(truckId)
                .licensePlate(licensePlate)
                .transportTaskId(transportTaskId)
                .userNumber(driverId)
                .build());
    }
}
