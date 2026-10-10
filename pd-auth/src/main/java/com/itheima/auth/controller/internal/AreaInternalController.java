package com.itheima.auth.controller.internal;

import com.itheima.auth.service.internal.AreaQueryService;
import com.itheima.pinda.DTO.AreaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 行政区划内部端点：路径与 AreaFeign 对齐。
 */
@RestController
@RequestMapping("/internal/area")
@RequiredArgsConstructor
public class AreaInternalController {

    private final AreaQueryService areaQueryService;

    @GetMapping("/{id}")
    public AreaDTO get(@PathVariable("id") Long id) {
        return areaQueryService.get(id);
    }

    @GetMapping("/list")
    public List<AreaDTO> findAll(@RequestParam(value = "parentId", required = false) Long parentId,
                                 @RequestParam(value = "ids", required = false) List<Long> ids) {
        return areaQueryService.findAll(parentId, ids);
    }

    @GetMapping("/code/{code}")
    public AreaDTO getByCode(@PathVariable("code") String code) {
        return areaQueryService.getByCode(code);
    }
}
