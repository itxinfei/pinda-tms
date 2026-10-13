package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户分页列表项（对齐前端 UserItem）。
 * 字段名沿用 youlai：username/nickname/gender/deptId/deptName，
 * 后端实体为 account/name/sex/orgId——在 service 做映射。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String nickname;
    private String avatar;
    /** 性别：1男 2女 */
    private Integer gender;
    private String mobile;
    private String email;
    /** 状态：1启用 0禁用 */
    private Integer status;
    /** 归属机构 ID（前端沿用 deptId 命名） */
    private String deptId;
    private String deptName;
    /** 岗位 ID */
    private String stationId;
    private String stationName;
    /** 角色名称（逗号分隔） */
    private String roleNames;
    private List<Long> roleIds;
    private LocalDateTime createTime;
}
