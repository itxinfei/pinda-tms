package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 访问概览（对齐前端 VisitOverviewDetail）。
 */
@Data
public class VisitOverviewDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private long todayUvCount;
    private long totalUvCount;
    private double uvGrowthRate;
    private long todayPvCount;
    private long totalPvCount;
    private double pvGrowthRate;
}