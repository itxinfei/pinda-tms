package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.vo.AreaSimpleVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("common")
@Tag(name = "公共信息")
@Slf4j
public class CommonController {

    private final AreaFeign areaFeign;

    public CommonController(AreaFeign areaFeign) {
        this.areaFeign = areaFeign;
    }

    @Operation(summary = "获取行政区域简要信息列表")
    @GetMapping(value = "/area/simple")
    public Result areaSimple(@Parameter(description = "父级id，无父级为0", required = true, example = "0")
                             @RequestParam(value = "parentId") String parentId) {
        // Feign 直接返回裸 List，远程失败可能为 null，统一通过 Rx 安全取值，避免 NPE
        List<AreaDTO> areas = Rx.list(areaFeign.findAll(StringUtils.isEmpty(parentId) ? null : Long.valueOf(parentId), null));
        return Result.ok().put("data", areas.stream().map(area -> {
            AreaSimpleVo vo = new AreaSimpleVo();
            if (area != null && area.getId() != null) {
                BeanUtils.copyProperties(area, vo);
                vo.setId(String.valueOf(area.getId()));
            }
            return vo;
        }).collect(Collectors.toList()));
    }
}
