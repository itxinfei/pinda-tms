package com.itheima.pinda.controller.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.DTO.user.TruckDriverDto;
import com.itheima.pinda.DTO.user.TruckDriverLicenseDto;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.controller.support.IdConverter;
import com.itheima.pinda.entity.user.PdTruckDriver;
import com.itheima.pinda.entity.user.PdTruckDriverLicense;
import com.itheima.pinda.service.user.IPdTruckDriverLicenseService;
import com.itheima.pinda.service.user.IPdTruckDriverService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 司机相关
 */
@RestController
@RequestMapping("sys/driver")
public class DriverController {
    @Autowired
    private IPdTruckDriverService truckDriverService;
    @Autowired
    private IPdTruckDriverLicenseService truckDriverLicenseService;

    /**
     * 保存司机基本信息
     *
     * @param dto 司机基本信息
     * @return 返回信息
     */
    @PostMapping("")
    public TruckDriverDto saveDriver(@RequestBody TruckDriverDto dto) {
        PdTruckDriver driver = new PdTruckDriver();
        BeanUtils.copyProperties(dto, driver);
        driver.setUserId(IdConverter.toLong(dto.getUserId()));
        driver.setFleetId(IdConverter.toLong(dto.getFleetId()));
        truckDriverService.saveTruckDriver(driver);
        return toDriverDto(driver);
    }

    /**
     * 获取司机基本信息列表
     *
     * @param userIds 司机id列表
     * @return 司机基本信息列表
     */
    @GetMapping("")
    public List<TruckDriverDto> findAllDriver(@RequestParam(name = "userIds", required = false) List<String> userIds,
                                              @RequestParam(name = "fleetId", required = false) String fleetId) {
        return truckDriverService.findAll(IdConverter.toLongList(userIds), IdConverter.toLong(fleetId)).stream()
                .map(this::toDriverDto)
                .toList();
    }

    /**
     * 获取司机基本信息
     *
     * @param id 司机id
     * @return 司机基本信息
     */
    @GetMapping("/{id}")
    public TruckDriverDto findOneDriver(@PathVariable(name = "id") String id) {
        PdTruckDriver pdTruckDriver = truckDriverService.findOne(IdConverter.toLong(id));
        if (pdTruckDriver == null) {
            return new TruckDriverDto();
        }
        return toDriverDto(pdTruckDriver);
    }

    /**
     * 保存司机驾驶证信息
     *
     * @param dto 司机驾驶证信息
     * @return 返回信息
     */
    @PostMapping("/driverLicense")
    public TruckDriverLicenseDto saveDriverLicense(@RequestBody TruckDriverLicenseDto dto) {
        PdTruckDriverLicense driverLicense = new PdTruckDriverLicense();
        BeanUtils.copyProperties(dto, driverLicense);
        driverLicense.setDriverId(IdConverter.toLong(dto.getUserId()));
        truckDriverLicenseService.saveTruckDriverLicense(driverLicense);
        return toLicenseDto(driverLicense);
    }

    /**
     * 获取司机驾驶证信息
     *
     * @param id 司机id
     * @return 司机驾驶证信息
     */
    @GetMapping("/{id}/driverLicense")
    public TruckDriverLicenseDto findOneDriverLicense(@PathVariable(name = "id") String id) {
        PdTruckDriverLicense driverLicense = truckDriverLicenseService.findOne(IdConverter.toLong(id));
        if (driverLicense == null) {
            return new TruckDriverLicenseDto();
        }
        return toLicenseDto(driverLicense);
    }

    /**
     * 统计司机数量
     *
     * @param fleetId 车队id
     * @return 司机数量
     */
    @GetMapping("/count")
    public Integer count(@RequestParam(name = "fleetId", required = false) String fleetId) {
        return truckDriverService.count(IdConverter.toLong(fleetId));
    }

    /**
     * 获取司机分页数据
     *
     * @param page     页码
     * @param pageSize 页尺寸
     * @param fleetId  车队id
     * @return 司机分页数据
     */
    @GetMapping("/page")
    public PageResponse<TruckDriverDto> findByPage(@RequestParam(name = "page") Integer page,
                                                   @RequestParam(name = "pageSize") Integer pageSize,
                                                   @RequestParam(name = "fleetId", required = false) String fleetId) {
        IPage<PdTruckDriver> truckPage = truckDriverService.findByPage(page, pageSize, IdConverter.toLong(fleetId));
        List<TruckDriverDto> dtoList = new ArrayList<>();
        truckPage.getRecords().forEach(pdTruckDriver -> dtoList.add(toDriverDto(pdTruckDriver)));
        return PageResponse.<TruckDriverDto>builder().items(dtoList).pagesize(pageSize).page(page).counts(truckPage.getTotal())
                .pages(truckPage.getPages()).build();
    }


    @GetMapping("/findAll")
    public List<TruckDriverDto> findAll(@RequestParam(name = "ids", required = false) List<String> ids) {
        LambdaQueryWrapper<PdTruckDriver> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(PdTruckDriver::getId, IdConverter.toLongList(ids));
        return truckDriverService.list(wrapper).stream()
                .map(this::toDriverDto)
                .toList();
    }

    /**
     * 司机实体 → DTO：id/userId/fleetId 转回 String
     */
    private TruckDriverDto toDriverDto(PdTruckDriver entity) {
        TruckDriverDto dto = new TruckDriverDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setId(IdConverter.toStr(entity.getId()));
        dto.setUserId(IdConverter.toStr(entity.getUserId()));
        dto.setFleetId(IdConverter.toStr(entity.getFleetId()));
        return dto;
    }

    /**
     * 驾驶证实体 → DTO：id/driverId 转回 String（driverId 对应 DTO 的 userId）
     */
    private TruckDriverLicenseDto toLicenseDto(PdTruckDriverLicense entity) {
        TruckDriverLicenseDto dto = new TruckDriverLicenseDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setId(IdConverter.toStr(entity.getId()));
        dto.setUserId(IdConverter.toStr(entity.getDriverId()));
        return dto;
    }
}
