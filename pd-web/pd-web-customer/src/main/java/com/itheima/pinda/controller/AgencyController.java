package com.itheima.pinda.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import com.itheima.pinda.util.Rx;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 网点  前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-3-30
 */
@Log4j2
@Tag(name = "网点自寄")
@RestController
@RequestMapping("agency")
public class AgencyController {

    private final OrgFeign orgFeign;

    private final AreaFeign areaFeign;

    public AgencyController(OrgFeign orgFeign, AreaFeign areaFeign) {
        this.orgFeign = orgFeign;
        this.areaFeign = areaFeign;
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pagesize
     * @return
     */
    @SneakyThrows
    @Operation(summary = "网点自寄分页")
    @GetMapping("page")
    public Result page(Integer page, Integer pagesize, Long cityId, String keyword, String latitude, String longitude) {

        // 远程调用返回 PageResponse<OrgDTO>，可能为 null；统一通过 Rx 取 items，避免 NPE
        PageResponse<OrgDTO> pageResult = orgFeign.pageLike(pagesize, page, keyword, cityId, latitude, longitude);
        List<OrgDTO> records = Rx.items(pageResult);

        Set<Long> areaSet = new HashSet<>();
        records.stream().filter(item -> !ObjectUtils.isEmpty(item.getProvinceId())).forEach(item -> areaSet.add(item.getProvinceId()));
        records.stream().filter(item -> !ObjectUtils.isEmpty(item.getCityId())).forEach(item -> areaSet.add(item.getCityId()));
        records.stream().filter(item -> !ObjectUtils.isEmpty(item.getCountyId())).forEach(item -> areaSet.add(item.getCountyId()));
        CompletableFuture<Map<Long, AreaDTO>> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);
        Map<Long, AreaDTO> areaMap = areaMapFuture.get();

        List<Map> newRecords = records.stream().map(item -> {
            Map newItem = writeOrgToMap(item);
            newItem.put("province", (item.getProvinceId() != null && areaMap.get(item.getProvinceId()) != null) ? areaMap.get(item.getProvinceId()).getName() : "");
            newItem.put("city", (item.getCityId() != null && areaMap.get(item.getCityId()) != null) ? areaMap.get(item.getCityId()).getName() : "");
            newItem.put("county", (item.getCountyId() != null && areaMap.get(item.getCountyId()) != null) ? areaMap.get(item.getCountyId()).getName() : "");
            newItem.put("fullAddress", newItem.get("province") + "" + newItem.get("city") + newItem.get("county") + newItem.get("address"));
            return newItem;
        }).collect(Collectors.toList());

        return Result.ok().put("data", PageResponse.<Map>builder()
                .items(newRecords)
                .pagesize(pagesize)
                .page(page)
                .pages(pageResult != null ? pageResult.getPages() : 0L)
                .counts(pageResult != null ? pageResult.getCounts() : 0L)
                .build());
    }

    /**
     * 把 OrgDTO 转成与旧 ORM records 同构的 Map。
     * 开启 WriteMapNullValue 保证 null 字段 key 不丢，再由 writeMapNullToEmpty 统一落为 ""。
     */
    private Map writeOrgToMap(OrgDTO org) {
        Map<String, Object> params = JSON.parseObject(JSON.toJSONString(org, JSONWriter.Feature.WriteMapNullValue));
        return writeMapNullToEmpty(params);
    }

    private Map writeMapNullToEmpty(Map params) {
        Map map = new HashMap();
        params.keySet().forEach(key -> {
            Object value = params.get(key);
            if (value != null && !StringUtils.isEmpty(value.toString())) {
                map.put(key, value);
            } else {
                map.put(key, "");
            }
        });
        return map;
    }
}
