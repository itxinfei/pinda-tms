package com.xinfei.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.alibaba.excel.EasyExcel;
import com.xinfei.auth.common.v1.ApiResult;
import com.xinfei.auth.common.v1.LogExportRow;
import com.xinfei.auth.common.v1.LogVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.VisitOverviewDetail;
import com.xinfei.auth.common.v1.VisitTrendDetail;
import com.xinfei.auth.entity.SysOperationLog;
import com.xinfei.auth.service.v1.LogService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    /** 导出行时间格式（与前端 yyyy-MM-dd HH:mm:ss 全局契约一致） */
    private static final DateTimeFormatter EXPORT_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping
    @SaCheckPermission("sys:log:list")
    public ApiResult<PageResult<LogVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "createTime", required = false) List<String> createTime,
            @RequestParam(value = "logType", required = false) String logType) {
        String start = (createTime != null && !createTime.isEmpty()) ? createTime.get(0) : null;
        String end = (createTime != null && createTime.size() > 1) ? createTime.get(1) : null;
        return ApiResult.success(logService.page(pageNum, pageSize, keywords, start, end, logType));
    }

    /**
     * 审计日志导出（D-30：关键动作留可导出操作痕迹）。xlsx 同步写出，
     * 封顶 {@link LogService#EXPORT_MAX_ROWS} 行；导出动作本身由写请求拦截器记 OPER 日志。
     */
    @GetMapping("/export")
    @SaCheckPermission("sys:log:export")
    public void export(
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "createTime", required = false) List<String> createTime,
            @RequestParam(value = "logType", required = false) String logType,
            HttpServletResponse response) throws IOException {
        String start = (createTime != null && !createTime.isEmpty()) ? createTime.get(0) : null;
        String end = (createTime != null && createTime.size() > 1) ? createTime.get(1) : null;
        List<LogExportRow> rows = logService.listForExport(keywords, start, end, logType).stream()
                .map(l -> {
                    LogExportRow r = new LogExportRow();
                    r.setType(SysOperationLog.TYPE_LOGIN.equals(l.getLogType()) ? "登录" : "操作");
                    r.setTitle(l.getTitle());
                    r.setOperatorName(l.getOperatorName());
                    r.setIp(l.getIp());
                    r.setRequestUri(l.getRequestUri());
                    r.setRequestMethod(l.getRequestMethod());
                    r.setStatus(Integer.valueOf(1).equals(l.getStatus()) ? "成功" : "失败");
                    r.setExecutionTime(l.getExecutionTime());
                    r.setErrorMsg(l.getErrorMsg());
                    r.setCreateTime(l.getCreateTime() == null ? "" : l.getCreateTime().format(EXPORT_TIME_FMT));
                    return r;
                }).toList();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("审计日志_" + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx", StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName);
        EasyExcel.write(response.getOutputStream(), LogExportRow.class).sheet("审计日志").doWrite(rows);
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