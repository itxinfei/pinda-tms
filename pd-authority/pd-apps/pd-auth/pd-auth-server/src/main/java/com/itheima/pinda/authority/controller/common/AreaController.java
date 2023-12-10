package com.itheima.pinda.authority.controller.common;


import com.itheima.pinda.authority.biz.service.common.AreaService;
import com.itheima.pinda.authority.entity.common.Area;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 前端控制器
 * 系统日志
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/area")
@Api(value = "Area", tags = "行政区域")
public class AreaController extends BaseController {
    @Autowired
    private AreaService areaService;


    /**
     * 根据id获取行政区域详情
     *
     * @param id 行政区域id
     * @return 行政区域id
     */
    @GetMapping("/{id}")
    public R<Area> get(@PathVariable Long id) {
        return success(areaService.getById(id));
    }

    @GetMapping("/code/{code}")
    public R<Area> getByCode(@PathVariable String code) {
        return success(areaService.getByCode(code));
    }

    /**
     * 获取行政区域简要信息列表
     *
     * @param parentId 父级id
     * @return 简要信息列表
     */
    @GetMapping
    public R<List<Area>> findAll(@RequestParam(value = "parentId", required = false) Long parentId, @RequestParam(value = "ids", required = false) List<Long> ids) {
        return success(areaService.findAll(parentId, ids));
    }
}
