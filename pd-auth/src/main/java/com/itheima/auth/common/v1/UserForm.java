package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户新建/编辑入参（对齐前端 UserForm）。
 * 新建时 username 必填；编辑不修改密码（重置密码走专用端点）。
 */
@Data
public class UserForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    /** 登录账号 */
    private String username;
    /** 姓名/昵称 */
    private String nickname;
    private String avatar;
    private Integer gender;
    private String mobile;
    private String email;
    private Integer status;
    /** 归属机构（前端 deptId） */
    private String deptId;
    /** 岗位 */
    private String stationId;
    /** 角色 ID */
    private List<Long> roleIds;
}
