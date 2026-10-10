package com.itheima.pinda.controller.agency;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.controller.support.IdConverter;
import com.itheima.pinda.entity.agency.PdFleet;
import com.itheima.pinda.service.agency.IPdFleetService;
import com.itheima.pinda.DTO.angency.FleetDto;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * FleetController
 */
@RestController
@RequestMapping("sys/agency/fleet")
public class FleetController {

    @Autowired
    private IPdFleetService fleetService;

    /**
     * 添加车队
     *
     * @param dto 车队信息
     * @return 车队信息
     */
    @PostMapping("")
    public FleetDto saveFleet(@RequestBody FleetDto dto) {
        PdFleet pdFleet = new PdFleet();
        BeanUtils.copyProperties(dto, pdFleet);
        pdFleet.setOrgId(IdConverter.toLong(dto.getAgencyId()));
        pdFleet = fleetService.saveFleet(pdFleet);
        return toDto(pdFleet);
    }

    /**
     * 根据id获取车队详情
     *
     * @param id 车队id
     * @return 车队信息
     */
    @GetMapping("/{id}")
    public FleetDto findById(@PathVariable(name = "id") String id) {
        PdFleet pdFleet = fleetService.getById(IdConverter.toLong(id));
        if (pdFleet == null) {
            return new FleetDto();
        }
        return toDto(pdFleet);
    }

    /**
     * 获取车队分页数据
     *
     * @param page        页码
     * @param pageSize    页尺寸
     * @param name        车队名称
     * @param fleetNumber 车队编号
     * @param manager     负责人id
     * @return 车队分页数据
     */
    @GetMapping("/page")
    public PageResponse<FleetDto> findByPage(@RequestParam(name = "page") Integer page,
                                             @RequestParam(name = "pageSize") Integer pageSize,
                                             @RequestParam(name = "name", required = false) String name,
                                             @RequestParam(name = "fleetNumber", required = false) String fleetNumber,
                                             @RequestParam(name = "manager", required = false) String manager) {
        IPage<PdFleet> fleetPage = fleetService.findByPage(page, pageSize, name, fleetNumber, manager);
        List<FleetDto> dtoList = new ArrayList<>();
        fleetPage.getRecords().forEach(pdFleet -> dtoList.add(toDto(pdFleet)));
        return PageResponse.<FleetDto>builder().items(dtoList).pagesize(pageSize).page(page).counts(fleetPage.getTotal())
                .pages(fleetPage.getPages()).build();
    }

    /**
     * 获取车队列表
     *
     * @param ids 车队Id列表
     * @return 车队列表
     */
    @GetMapping("")
    public List<FleetDto> findAll(@RequestParam(value = "ids", required = false) List<String> ids,
                                  @RequestParam(value = "agencyId", required = false) String agencyId) {
        return fleetService.findAll(IdConverter.toLongList(ids), IdConverter.toLong(agencyId)).stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * 更新车队信息
     *
     * @param dto 车队信息
     * @return 车队信息
     */
    @PutMapping("/{id}")
    public FleetDto update(@PathVariable(name = "id") String id, @RequestBody FleetDto dto) {
        PdFleet pdFleet = new PdFleet();
        BeanUtils.copyProperties(dto, pdFleet);
        pdFleet.setId(IdConverter.toLong(id));
        pdFleet.setOrgId(IdConverter.toLong(dto.getAgencyId()));
        fleetService.updateById(pdFleet);
        dto.setId(id);
        return dto;
    }

    /**
     * 删除车队
     *
     * @param id 车队id
     * @return 返回信息
     */
    @PutMapping("/{id}/disable")
    public Result disable(@PathVariable(name = "id") String id) {
        fleetService.disableById(IdConverter.toLong(id));
        return Result.ok();
    }

    /**
     * 实体 → DTO：id/orgId 转回 String
     */
    private FleetDto toDto(PdFleet pdFleet) {
        FleetDto dto = new FleetDto();
        BeanUtils.copyProperties(pdFleet, dto);
        dto.setId(IdConverter.toStr(pdFleet.getId()));
        dto.setAgencyId(IdConverter.toStr(pdFleet.getOrgId()));
        return dto;
    }
}
