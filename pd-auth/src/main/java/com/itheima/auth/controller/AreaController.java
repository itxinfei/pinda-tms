package com.itheima.auth.controller;

import com.itheima.auth.common.R;
import com.itheima.auth.entity.Area;
import com.itheima.auth.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区划查询。经网关访问路径 /api/auth/area/**。
 */
@RestController
@RequestMapping("/area")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;

    /** 懒加载下级：parentId 为空返回省级 */
    @GetMapping("/children")
    public R<List<Area>> children(@RequestParam(required = false) Long parentId) {
        return R.success(areaService.children(parentId));
    }

    /** 按名称搜索 */
    @GetMapping("/search")
    public R<List<Area>> search(@RequestParam String name) {
        return R.success(areaService.search(name));
    }

    /** 行政区划详情 */
    @GetMapping("/detail")
    public R<Area> detail(@RequestParam Long id) {
        return R.success(areaService.detail(id));
    }
}
