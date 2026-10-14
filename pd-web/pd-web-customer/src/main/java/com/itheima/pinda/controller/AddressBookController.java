package com.itheima.pinda.controller;


import com.itheima.pinda.DTO.AddressBookDTO;
import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.common.utils.PageQuery;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.component.MemberIdentityResolver;
import com.itheima.pinda.util.Rx;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.AddressBookFeign;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.future.PdCompletableFuture;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 地址簿  前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-3-30
 */
@Log4j2
@Tag(name = "地址簿")
@RestController
@RequestMapping("address")
public class AddressBookController {

    private final AddressBookFeign addressBookFeign;

    private final AreaFeign areaFeign;

    private final MemberIdentityResolver memberIdentityResolver;

    public AddressBookController(AddressBookFeign addressBookFeign, AreaFeign areaFeign,
                                 MemberIdentityResolver memberIdentityResolver) {
        this.addressBookFeign = addressBookFeign;
        this.areaFeign = areaFeign;
        this.memberIdentityResolver = memberIdentityResolver;
    }

    /**
     * 分页查询
     *
     * @param page
     * @param pageSize
     * @return
     */
    @SneakyThrows
    @Operation(summary = "地址簿分页查询")
    @GetMapping("page")
    public Result page(Integer page, Integer pageSize, String keyword) {
        // D-05 分页规范化（《12》§8.4）：page>=1、pageSize<=500，非法入参回退默认，不再原样透传
        int[] pp = PageQuery.normalize(page, pageSize);
        page = pp[0];
        pageSize = pp[1];
        // 认证账号 ID 解析为会员雪花 ID（未登录/未绑手机号抛异常由全局 advice 处理）
        String memberId = memberIdentityResolver.requireCurrentMemberId();
        // 远程调用返回 PageResponse 可能为 null，统一通过 Rx 安全取值，避免 NPE
        PageResponse<AddressBookDTO> result = addressBookFeign.page(page, pageSize, memberId, keyword);
        List<AddressBookDTO> items = Rx.items(result);
        Set<Long> areaSet = new HashSet<>();
        areaSet.addAll(items.stream().map(item -> item.getProvinceId()).collect(Collectors.toSet()));
        areaSet.addAll(items.stream().map(item -> item.getCityId()).collect(Collectors.toSet()));
        areaSet.addAll(items.stream().map(item -> item.getCountyId()).collect(Collectors.toSet()));
        CompletableFuture<Map<Long, AreaDTO>> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);
        Map<Long, AreaDTO> areaMap = areaMapFuture.get();

        // R2④：Feign 已直接返回 DTO，只需就地补齐省市区展示字段，不再反射拷贝
        List<AddressBookDTO> newItems = items.stream().map(addressBookDTO -> {
            addressBookDTO.setProvince(areaMap.containsKey(addressBookDTO.getProvinceId()) ? areaMap.get(addressBookDTO.getProvinceId()).getName() : "");
            addressBookDTO.setCity(areaMap.containsKey(addressBookDTO.getCityId()) ? areaMap.get(addressBookDTO.getCityId()).getName() : "");
            addressBookDTO.setCounty(areaMap.containsKey(addressBookDTO.getCountyId()) ? areaMap.get(addressBookDTO.getCountyId()).getName() : "");
            addressBookDTO.setFullAddress(addressBookDTO.getProvince() + addressBookDTO.getCity() + addressBookDTO.getCounty() + addressBookDTO.getAddress());
            return addressBookDTO;
        }).collect(Collectors.toList());

        return Result.ok().put("data", PageResponse.<AddressBookDTO>builder()
                .counts(result != null ? result.getCounts() : 0L)
                .pages(result != null ? result.getPages() : 0L)
                .page(result != null ? result.getPage() : page)
                .pagesize(result != null ? result.getPagesize() : pageSize)
                .items(newItems)
                .build());
    }

    /**
     * 新增
     *
     * @param entity
     * @return
     */
    @PostMapping("")
    @Operation(summary = "新增")
    public Result save(@RequestBody AddressBookDTO entity) {
        String memberId = memberIdentityResolver.requireCurrentMemberId();
        // 归属会员由服务端解析后显式传参，请求体里的 userId 下游会强制丢弃
        return addressBookFeign.save(memberId, entity);
    }

    /**
     * 修改
     *
     * @param id
     * @param entity
     * @return
     */
    @PutMapping("/{id}")
    @Operation(summary = "修改")
    public Result update(@PathVariable(name = "id") String id, @RequestBody AddressBookDTO entity) {
        String memberId = memberIdentityResolver.requireCurrentMemberId();
        return addressBookFeign.update(id, memberId, entity);
    }

    /**
     * 删除
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除")
    public Result del(@PathVariable(name = "id") String id) {
        String memberId = memberIdentityResolver.requireCurrentMemberId();
        return addressBookFeign.del(id, memberId);
    }

    /**
     * 详情
     *
     * @param id
     * @return
     */
    @SneakyThrows
    @Operation(summary = "明细")
    @GetMapping("detail/{id}")
    public Result detail(@PathVariable(name = "id") String id) {
        String memberId = memberIdentityResolver.requireCurrentMemberId();
        AddressBookDTO addressBook = addressBookFeign.detail(id, memberId);
        if (addressBook == null) {
            return Result.error();
        }
        Set<Long> areaSet = new HashSet<>();
        areaSet.add(addressBook.getProvinceId());
        areaSet.add(addressBook.getCityId());
        areaSet.add(addressBook.getCountyId());
        CompletableFuture<Map<Long, AreaDTO>> areaMapFuture = PdCompletableFuture.areaMapFuture(areaFeign, null, areaSet);
        Map<Long, AreaDTO> areaMap = areaMapFuture.get();

        // R2④：Feign 已直接返回 DTO，就地补齐省市区展示字段，不再反射拷贝
        addressBook.setProvince(areaMap.containsKey(addressBook.getProvinceId()) ? areaMap.get(addressBook.getProvinceId()).getName() : "");
        addressBook.setCity(areaMap.containsKey(addressBook.getCityId()) ? areaMap.get(addressBook.getCityId()).getName() : "");
        addressBook.setCounty(areaMap.containsKey(addressBook.getCountyId()) ? areaMap.get(addressBook.getCountyId()).getName() : "");
        addressBook.setFullAddress(addressBook.getProvince() + addressBook.getCity() + addressBook.getCounty() + addressBook.getAddress());

        return Result.ok().put("data",addressBook);


    }
}
