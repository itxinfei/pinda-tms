package com.itheima.pinda.vo.base.userCenter;

import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.angency.RoleVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "用户信息")
public class SysUserVo implements Serializable {
    private static final long serialVersionUID = -3424962804442674755L;
    @Schema(description = "用户id")
    private String userId;
    @Schema(description = "员工账号")
    private String username;
    @Schema(description = "员工姓名")
    private String name;
    @Schema(description = "密码")
    private String password;
    @Schema(description = "工号")
    private String workNumber;
    @Schema(description = "邮箱")
    private String email;
    @Schema(description = "所属机构信息")
    private AgencySimpleVo agency;
    @Schema(description = "手机号")
    private String mobile;
    @Schema(description = "岗位 1为员工 2为快递员 3为司机")
    private Integer station;
    @Schema(description = "岗位名称")
    private String stationName;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "账号状态 0：禁用   1：正常")
    private Integer status;
    @Schema(description = "创建者信息")
    private SysUserVo creator;
    @Schema(description = "创建时间,格式: yyyy-MM-dd HH:mm:ss")
    private String createTime;
    @Schema(description = "角色信息")
    private List<RoleVo> roles;
}
