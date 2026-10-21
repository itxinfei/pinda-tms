package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 访问趋势（对齐前端 VisitTrendDetail）。
 */
@Data
public class VisitTrendDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> dates;
    private List<Long> pvList;
    private List<Long> uvList;
}
