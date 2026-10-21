package com.xinfei.auth.common.v1;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;

/**
 * 审计日志导出行（D-30 可导出痕迹）。EasyExcel 注解列序即表头顺序。
 */
@Data
@ColumnWidth(20)
public class LogExportRow implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("类型")
    @ColumnWidth(8)
    private String type;

    @ExcelProperty("标题")
    @ColumnWidth(30)
    private String title;

    @ExcelProperty("操作人")
    private String operatorName;

    @ExcelProperty("IP")
    private String ip;

    @ExcelProperty("请求路径")
    @ColumnWidth(36)
    private String requestUri;

    @ExcelProperty("方法")
    @ColumnWidth(8)
    private String requestMethod;

    @ExcelProperty("状态")
    @ColumnWidth(8)
    private String status;

    @ExcelProperty("耗时(ms)")
    @ColumnWidth(10)
    private Integer executionTime;

    @ExcelProperty("错误信息")
    @ColumnWidth(40)
    private String errorMsg;

    @ExcelProperty("时间")
    private String createTime;
}
