package com.itheima.pinda.authority.controller.core;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.biz.service.core.OrgService;
import com.itheima.pinda.authority.biz.service.core.StationService;
import com.itheima.pinda.authority.dto.core.StationPageDTO;
import com.itheima.pinda.authority.dto.core.StationSaveDTO;
import com.itheima.pinda.authority.dto.core.StationUpdateDTO;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.authority.entity.core.Station;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.base.entity.SuperEntity;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.log.annotation.SysLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * 岗位
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/station")
@Api(value = "Station", tags = "岗位")
public class StationController extends BaseController {

    @Autowired
    private StationService stationService;
    @Autowired
    private OrgService orgService;
    @Autowired
    private DozerUtils dozer;

    /**
     * 分页查询岗位
     *
     * @param data 分页查询对象
     * @return 查询结果
     */
    @ApiOperation(value = "分页查询岗位", notes = "分页查询岗位")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "current", value = "当前页", dataType = "long", paramType = "query", defaultValue = "1"),
            @ApiImplicitParam(name = "size", value = "每页显示几条", dataType = "long", paramType = "query", defaultValue = "10"),
    })
    @GetMapping("/page")
    @SysLog("分页查询岗位")
    public R<IPage<Station>> page(StationPageDTO data) {

        Page<Station> page = getPage();
        stationService.findStationPage(page, data);
        List<Station> list = page.getRecords().stream().map(item -> {
            Org org = orgService.getById(item.getOrgId());
            if (org != null) {
                item.setOrgName(org.getName());
            }else{
                item.setOrgName("");
            }
            return item;
        }).collect(Collectors.toList());
        page.setRecords(list);
        return success(page);
    }
    @ApiOperation(value = "查询岗位", notes = "查询岗位")
    @GetMapping("/list")
    @SysLog("查询岗位")
    public R<List<Station>> list(StationPageDTO data) {
        List<Station> list = new ArrayList<>();
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(data.getOrgId() != null, Station::getOrgId, data.getOrgId());
        wrapper.orderByAsc(Station::getCreateTime);
        list.addAll(stationService.list(wrapper));
        log.info("岗位查询:{}", list);
        if (data.getOrgId() != null && data.getOrgId() != 100L) {
            wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Station::getOrgId, 100L);
            wrapper.orderByAsc(Station::getCreateTime);
            list.addAll(stationService.list(wrapper));
            log.info("岗位查询-默认:{}", list);
        }


        list = list.stream().map(item -> {
            Org org = orgService.getById(item.getOrgId());
            if (org != null) {
                item.setOrgName(org.getName());
            } else {
                item.setOrgName("");
            }
            return item;
        }).collect(Collectors.toList());
        return success(list);
    }

    /**
     * 查询岗位
     *
     * @param id 主键id
     * @return 查询结果
     */
    @ApiOperation(value = "查询岗位", notes = "查询岗位")
    @GetMapping("/{id}")
    @SysLog("查询岗位")
    public R<Station> get(@PathVariable Long id) {
        return success(stationService.getById(id));
    }

    /**
     * 新增岗位
     *
     * @param data 新增对象
     * @return 新增结果
     */
    @ApiOperation(value = "新增岗位", notes = "新增岗位不为空的字段")
    @PostMapping
    @SysLog("新增岗位")
    public R<Station> save(@RequestBody @Validated StationSaveDTO data) {
        Station station = dozer.map(data, Station.class);
        stationService.save(station);
        return success(station);
    }

    /**
     * 修改岗位
     *
     * @param data 修改对象
     * @return 修改结果
     */
    @ApiOperation(value = "修改岗位", notes = "修改岗位不为空的字段")
    @PutMapping
    @SysLog("修改岗位")
    public R<Station> update(@RequestBody @Validated(SuperEntity.Update.class) StationUpdateDTO data) {
        Station station = dozer.map(data, Station.class);
        if (station.getOrgId() == null) {
            station.setOrgId(0L);
        }
        stationService.updateById(station);
        return success(station);
    }

    /**
     * 删除岗位
     *
     * @param ids 主键id
     * @return 删除结果
     */
    @ApiOperation(value = "删除岗位", notes = "根据id物理删除岗位")
    @SysLog("删除岗位")
    @DeleteMapping
    public R<Boolean> delete(@RequestParam("ids[]") List<Long> ids) {
        stationService.removeByIds(ids);
        return success();
    }

}
