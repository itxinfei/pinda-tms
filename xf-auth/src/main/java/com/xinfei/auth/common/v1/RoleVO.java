package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色列表项（对齐前端 RoleItem）。
 * dataScope 本期固定 1（全部数据）——通用数据权限引擎 P2 才做（《62》§10.3）。
 */
@Data
public class RoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String code;
    private String name;
    private Integer sort;
    private Integer status;
    /** 数据范围：本期固定 1（全部），前端展示用 */
    private Integer dataScope = 1;
    private String dataScopeLabel = "全部数据";
    private String remark;
    private LocalDateTime updateTime;
}
