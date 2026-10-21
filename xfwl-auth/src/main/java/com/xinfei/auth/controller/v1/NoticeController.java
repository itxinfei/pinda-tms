package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.NoticeDetail;
import com.xinfei.auth.common.v1.NoticeForm;
import com.xinfei.auth.common.v1.NoticeItemVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.service.v1.NoticeService;
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
 * 通知公告（youlai 契约 /api/v1/notices）。
 */
@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    @SaCheckPermission("sys:notice:list")
    public ApiResult<PageResult<NoticeItemVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "publishStatus", required = false) Integer publishStatus) {
        return ApiResult.success(noticeService.page(pageNum, pageSize, title, publishStatus));
    }

    @GetMapping("/my")
    public ApiResult<PageResult<NoticeItemVO>> myPage(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "title", required = false) String title) {
        return ApiResult.success(noticeService.myPage(pageNum, pageSize, title, null));
    }

    @PutMapping("/read-all")
    public ApiResult<Void> readAll() {
        noticeService.readAll();
        return ApiResult.success();
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:notice:list")
    public ApiResult<NoticeForm> form(@PathVariable Long id) {
        return ApiResult.success(noticeService.form(id));
    }

    @GetMapping("/{id}/detail")
    public ApiResult<NoticeDetail> detail(@PathVariable Long id) {
        return ApiResult.success(noticeService.detail(id));
    }

    @PostMapping
    @SaCheckPermission("sys:notice:create")
    public ApiResult<Long> create(@RequestBody NoticeForm form) {
        return ApiResult.success(noticeService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:notice:update")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NoticeForm form) {
        noticeService.update(id, form);
        return ApiResult.success();
    }

    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:notice:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();
        noticeService.deleteByIds(idList);
        return ApiResult.success();
    }

    @PutMapping("/{id}/publish")
    @SaCheckPermission("sys:notice:publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return ApiResult.success();
    }

    @PutMapping("/{id}/revoke")
    @SaCheckPermission("sys:notice:revoke")
    public ApiResult<Void> revoke(@PathVariable Long id) {
        noticeService.revoke(id);
        return ApiResult.success();
    }
}