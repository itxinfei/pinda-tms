package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.LogVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.VisitOverviewDetail;
import com.xinfei.auth.common.v1.VisitTrendDetail;
import com.xinfei.auth.service.v1.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志（youlai 契约 /api/v1/logs）。
 *
 * <p>createTime 为前端时间区间数组，序列化为重复查询参数，这里以 List 接两个元素。</p>
 */
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogService logService;

    @GetMapping
    @SaCheckPermission("sys:log:list")
    public ApiResult<PageResult<LogVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "createTime", required = false) List<String> createTime) {
        String start = (createTime != null && !createTime.isEmpty()) ? createTime.get(0) : null;
        String end = (createTime != null && createTime.size() > 1) ? createTime.get(1) : null;
        return ApiResult.success(logService.page(pageNum, pageSize, keywords, start, end));
    }

    @GetMapping("/analytics/trend")
    @SaCheckPermission("sys:log:list")
    public ApiResult<VisitTrendDetail> trend(
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate) {
        return ApiResult.success(logService.trend(startDate, endDate));
    }

    @GetMapping("/analytics/overview")
    @SaCheckPermission("sys:log:list")
    public ApiResult<VisitOverviewDetail> overview() {
        return ApiResult.success(logService.overview());
    }
}