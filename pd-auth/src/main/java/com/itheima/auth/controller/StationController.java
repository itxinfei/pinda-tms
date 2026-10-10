package com.itheima.auth.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.auth.common.R;
import com.itheima.auth.entity.CoreStation;
import com.itheima.auth.service.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 岗位管理。经网关访问路径 /api/auth/station/**。
 */
@RestController
@RequestMapping("/station")
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    /** 分页查询 */
    @GetMapping("/page")
    public R<IPage<CoreStation>> page(@RequestParam(defaultValue = "1") long pageNo,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String name,
                                      @RequestParam(required = false) Long orgId) {
        return R.success(stationService.page(pageNo, size, name, orgId));
    }

    /** 岗位详情 */
    @GetMapping("/detail")
    public R<CoreStation> detail(@RequestParam Long id) {
        return R.success(stationService.detail(id));
    }

    /** 新增岗位 */
    @PostMapping
    public R<Long> save(@RequestBody CoreStation station) {
        return R.success(stationService.save(station));
    }

    /** 修改岗位 */
    @PutMapping
    public R<Void> update(@RequestBody CoreStation station) {
        stationService.update(station);
        return R.success();
    }

    /** 删除岗位 */
    @DeleteMapping
    public R<Void> remove(@RequestParam Long id) {
        stationService.remove(id);
        return R.success();
    }
}
