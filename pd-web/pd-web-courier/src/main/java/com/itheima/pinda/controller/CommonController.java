package com.itheima.pinda.controller;

import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.AreaSimpleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("common")
@Tag(name = "公共信息")
@Slf4j
public class CommonController {
    @Autowired
    private AreaFeign areaFeign;

    @Operation(summary = "获取行政区域简要信息列表")
    @Parameters({
            @Parameter(name = "parentId", description = "父级id，无父级为0", required = true, example = "0")
    })
    @GetMapping(value = "area/simple")
    public Result areaSimple(@RequestParam(value = "parentId") String parentId) {
        // 修改点：远程调用结果可能为 null，统一通过 Rx 安全取值，避免 NPE
        return Result.ok().put("data", Rx.dataList(areaFeign.findAll(StringUtils.isEmpty(parentId) ? null : Long.valueOf(parentId), null)).stream().map(area -> {
            AreaSimpleVo vo = new AreaSimpleVo();
            if (area != null && area.getId() != null) {
                BeanUtils.copyProperties(area, vo);
                vo.setId(String.valueOf(area.getId()));
            }
            return vo;
        }).collect(Collectors.toList()));
    }

}
