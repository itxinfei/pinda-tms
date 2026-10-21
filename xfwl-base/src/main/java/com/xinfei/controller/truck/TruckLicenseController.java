package com.xinfei.controller.truck;

import com.xinfei.entity.truck.XfTruckLicense;
import com.xinfei.service.truck.IPdTruckLicenseService;
import com.xinfei.DTO.truck.TruckLicenseDto;
import com.xinfei.controller.support.IdConverter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TruckLicenseController
 */
@RestController
@RequestMapping("base/truck/license")
public class TruckLicenseController {
    @Autowired
    private IPdTruckLicenseService truckLicenseService;

    /**
     * 保存车辆行驶证信息
     *
     * @param dto 车辆行驶证信息
     * @return 车辆行驶证信息
     */
    @PostMapping("")
    public TruckLicenseDto saveTruckLicense(@RequestBody TruckLicenseDto dto) {
        XfTruckLicense xfTruckLicense = new XfTruckLicense();
        BeanUtils.copyProperties(dto, xfTruckLicense);
        xfTruckLicense.setTruckId(IdConverter.toLong(dto.getTruckId()));
        xfTruckLicense = truckLicenseService.saveTruckLicense(xfTruckLicense);
        return toDto(xfTruckLicense);
    }

    /**
     * 根据id获取车辆行驶证详情
     *
     * @param id 车辆行驶证id
     * @return 车辆行驶证信息
     */
    @GetMapping("/{id}")
    public TruckLicenseDto findById(@PathVariable(name = "id") String id) {
        XfTruckLicense xfTruckLicense = truckLicenseService.getById(IdConverter.toLong(id));
        if (xfTruckLicense == null) {
            return new TruckLicenseDto();
        }
        return toDto(xfTruckLicense);
    }

    /**
     * 实体 → DTO：id/车辆id 转回 String
     */
    private TruckLicenseDto toDto(XfTruckLicense entity) {
        TruckLicenseDto dto = new TruckLicenseDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setId(IdConverter.toStr(entity.getId()));
        dto.setTruckId(IdConverter.toStr(entity.getTruckId()));
        return dto;
    }
}
