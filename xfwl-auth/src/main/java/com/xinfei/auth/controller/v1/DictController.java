package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.DictItemForm;
import com.xinfei.auth.common.v1.DictItemOptionVO;
import com.xinfei.auth.common.v1.DictItemVO;
import com.xinfei.auth.common.v1.DictTypeForm;
import com.xinfei.auth.common.v1.DictTypeVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.service.v1.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * 数据字典（youlai 契约 /api/v1/dicts，《62》§6）。
 *
 * <p>字典项选项接口只要求登录、不加权限码：前端 DictSelect/DictTag 在业务页面全站消费，
 * 若挂 sys:dict:list 会让未授权角色看不到标签原文。管理端增删改仍按 sys:dict:* 校验。</p>
 */
@RestController
@RequestMapping("/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    @GetMapping
    @SaCheckPermission("sys:dict:list")
    public ApiResult<PageResult<DictTypeVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(dictService.page(pageNum, pageSize, keywords, status));
    }

    @GetMapping("/options")
    @SaCheckPermission("sys:dict:list")
    public ApiResult<List<OptionVO>> options() {
        return ApiResult.success(dictService.options());
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:dict:list")
    public ApiResult<DictTypeForm> typeForm(@PathVariable Long id) {
        return ApiResult.success(dictService.typeFormData(id));
    }

    @PostMapping
    @SaCheckPermission("sys:dict:create")
    public ApiResult<Long> createType(@RequestBody DictTypeForm form) {
        return ApiResult.success(dictService.createType(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:dict:update")
    public ApiResult<Void> updateType(@PathVariable Long id, @RequestBody DictTypeForm form) {
        dictService.updateType(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:dict:delete")
    public ApiResult<Void> deleteTypes(@PathVariable String ids) {
        dictService.deleteTypes(parseIds(ids));
        return ApiResult.success();
    }

    @GetMapping("/{dictCode}/items")
    @SaCheckPermission("sys:dict:list")
    public ApiResult<PageResult<DictItemVO>> itemPage(
            @PathVariable String dictCode,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords) {
        return ApiResult.success(dictService.itemPage(dictCode, pageNum, pageSize, keywords));
    }

    /** 字典项选项：登录即可（全站 DictSelect/DictTag 取数） */
    @GetMapping("/{dictCode}/items/options")
    public ApiResult<List<DictItemOptionVO>> itemOptions(@PathVariable String dictCode) {
        return ApiResult.success(dictService.itemOptions(dictCode));
    }

    @GetMapping("/{dictCode}/items/{id}/form")
    @SaCheckPermission("sys:dict:list")
    public ApiResult<DictItemForm> itemForm(@PathVariable String dictCode, @PathVariable Long id) {
        return ApiResult.success(dictService.itemFormData(dictCode, id));
    }

    @PostMapping("/{dictCode}/items")
    @SaCheckPermission("sys:dict:create")
    public ApiResult<Long> createItem(@PathVariable String dictCode, @RequestBody DictItemForm form) {
        return ApiResult.success(dictService.createItem(dictCode, form));
    }

    @PutMapping("/{dictCode}/items/{id}")
    @SaCheckPermission("sys:dict:update")
    public ApiResult<Void> updateItem(@PathVariable String dictCode, @PathVariable Long id,
                                      @RequestBody DictItemForm form) {
        dictService.updateItem(dictCode, id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{dictCode}/items/{ids}")
    @SaCheckPermission("sys:dict:delete")
    public ApiResult<Void> deleteItems(@PathVariable String dictCode, @PathVariable String ids) {
        dictService.deleteItems(dictCode, parseIds(ids));
        return ApiResult.success();
    }

    private List<Long> parseIds(String ids) {
        return Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
    }
}
