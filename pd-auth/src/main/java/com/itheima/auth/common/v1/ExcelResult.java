package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入结果（对齐前端 ExcelResult）。
 * code 用字符串成功码（前端拦截器按响应壳判断；messageList 给逐条错误）。
 */
@Data
public class ExcelResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String code;
    private int validCount;
    private int invalidCount;
    private List<String> messageList = new ArrayList<>();

    public static ExcelResult of(int valid, int invalid, List<String> messages) {
        ExcelResult r = new ExcelResult();
        r.setCode(invalid == 0 ? ApiResult.SUCCESS : ApiResult.FAIL);
        r.setValidCount(valid);
        r.setInvalidCount(invalid);
        r.setMessageList(messages == null ? new ArrayList<>() : messages);
        return r;
    }
}
