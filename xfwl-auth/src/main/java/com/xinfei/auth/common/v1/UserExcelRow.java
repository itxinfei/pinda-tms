package com.xinfei.auth.common.v1;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户导入/导出行模型（EasyExcel）。
 *
 * <p>导入时：账号/姓名必填；留空行跳过；机构/岗位/角色按名称匹配（不存在则该单元格置空并收集错误）。
 * 导出时：由系统用户数据填充。性别 1男 2女；状态 1启用 0禁用。</p>
 */
@Data
@ColumnWidth(18)
public class UserExcelRow implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("登录账号")
    private String username;

    @ExcelProperty("姓名")
    private String nickname;

    @ExcelProperty("性别(1男2女)")
    private Integer gender;

    @ExcelProperty("手机号")
    private String mobile;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("状态(1启用0禁用)")
    private Integer status;

    @ExcelProperty("所属机构名称")
    private String deptName;

    @ExcelProperty("岗位名称")
    private String stationName;

    @ExcelProperty("角色名称(多个用逗号分隔)")
    private String roleNames;
}
