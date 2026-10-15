package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.v1.LogVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.common.v1.VisitOverviewDetail;
import com.xinfei.auth.common.v1.VisitTrendDetail;
import com.xinfei.auth.entity.SysOperationLog;
import com.xinfei.auth.mapper.SysOperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志（youlai 契约 /api/v1/logs）：分页查询 + 访问统计。
 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysOperationLogMapper logMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public PageResult<LogVO> page(long pageNum, long pageSize, String keywords,
                                  String startTime, String endTime) {
        IPage<SysOperationLog> res = logMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<SysOperationLog>lambdaQuery()
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(SysOperationLog::getIp, keywords)
                                .or().like(SysOperationLog::getOperatorName, keywords))
                        .ge(StringUtils.hasText(startTime), SysOperationLog::getCreateTime, parse(startTime))
                        .le(StringUtils.hasText(endTime), SysOperationLog::getCreateTime, parse(endTime))
                        .orderByDesc(SysOperationLog::getId));
        List<LogVO> list = res.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(list, res.getTotal());
    }

    /** 访问趋势：按天 PV/UV，日期区间内缺失的天补 0 */
    public VisitTrendDetail trend(String startDate, String endDate) {
        LocalDate start = parseDay(startDate, LocalDate.now().minusDays(6));
        LocalDate end = parseDay(endDate, LocalDate.now());
        Map<String, long[]> stat = new HashMap<>();
        for (Map<String, Object> row : logMapper.groupByDay(
                start.atStartOfDay().format(FMT), end.atTime(23, 59, 59).format(FMT))) {
            Object day = row.get("day");
            if (day == null) {
                continue;
            }
            String key = day instanceof java.sql.Date sqlDate
                    ? sqlDate.toLocalDate().format(DAY)
                    : String.valueOf(day);
            stat.put(key, new long[]{
                    toLong(row.get("pv")),
                    toLong(row.get("uv"))
            });
        }
        List<String> dates = new ArrayList<>();
        List<Long> pv = new ArrayList<>();
        List<Long> uv = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            String key = d.format(DAY);
            long[] v = stat.getOrDefault(key, new long[]{0L, 0L});
            dates.add(key);
            pv.add(v[0]);
            uv.add(v[1]);
        }
        VisitTrendDetail t = new VisitTrendDetail();
        t.setDates(dates);
        t.setPvList(pv);
        t.setUvList(uv);
        return t;
    }

    /** 访问概览：今日/累计 PV/UV 及较昨日增长率 */
    public VisitOverviewDetail overview() {
        String today = LocalDate.now().format(DAY);
        String yesterday = LocalDate.now().minusDays(1).format(DAY);
        long todayPv = nullToZero(logMapper.countByDay(today));
        long todayUv = nullToZero(logMapper.distinctOperatorByDay(today));
        long yPv = nullToZero(logMapper.countByDay(yesterday));
        long yUv = nullToZero(logMapper.distinctOperatorByDay(yesterday));

        VisitOverviewDetail o = new VisitOverviewDetail();
        o.setTodayPvCount(todayPv);
        o.setTodayUvCount(todayUv);
        o.setTotalPvCount(nullToZero(logMapper.totalCount()));
        o.setTotalUvCount(nullToZero(logMapper.totalDistinctOperator()));
        o.setPvGrowthRate(growth(todayPv, yPv));
        o.setUvGrowthRate(growth(todayUv, yUv));
        return o;
    }

    private double growth(long today, long yesterday) {
        if (yesterday <= 0) {
            return today <= 0 ? 0d : 100d;
        }
        return round2((today - yesterday) * 100d / yesterday);
    }

    private double round2(double v) {
        return Math.round(v * 100d) / 100d;
    }

    private long nullToZero(Long v) {
        return v == null ? 0L : v;
    }

    private long toLong(Object v) {
        return v == null ? 0L : Long.parseLong(String.valueOf(v));
    }

    private LocalDate parseDay(String s, LocalDate def) {
        if (!StringUtils.hasText(s)) {
            return def;
        }
        try {
            return LocalDate.parse(s.trim(), DAY);
        } catch (Exception e) {
            return def;
        }
    }

    private LocalDateTime parse(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String v = s.trim().replace('T', ' ');
        try {
            return LocalDateTime.parse(v.length() > 19 ? v.substring(0, 19) : v, FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private LogVO toVO(SysOperationLog l) {
        LogVO v = new LogVO();
        v.setId(l.getId());
        v.setModule(l.getModule());
        v.setActionType(l.getActionType());
        v.setTitle(l.getTitle());
        v.setContent(l.getContent());
        v.setOperatorId(l.getOperatorId());
        v.setOperatorName(l.getOperatorName());
        v.setRequestUri(l.getRequestUri());
        v.setRequestMethod(l.getRequestMethod());
        v.setIp(l.getIp());
        v.setRegion(l.getRegion());
        v.setDevice(l.getDevice());
        v.setBrowser(l.getBrowser());
        v.setOs(l.getOs());
        v.setStatus(l.getStatus());
        v.setExecutionTime(l.getExecutionTime());
        v.setErrorMsg(l.getErrorMsg());
        v.setCreateTime(l.getCreateTime() == null ? null : l.getCreateTime().format(FMT));
        return v;
    }
}