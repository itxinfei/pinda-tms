package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单管理树节点（对齐前端 MenuItem）。id/parentId 用字符串（雪花 ID 超 JS 安全范围）。
 */
@Data
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String type;
    private String name;
    private String routeName;
    /** 路由路径（前端表单同时给 path/routePath，后端取 path） */
    private String path;
    private String routePath;
    private String component;
    private String perm;
    private String icon;
    private Integer sort;
    private Integer visible;
    /** 1 平台 2 业务：本表全局，默认 1 平台 */
    private Integer scope = 1;
    private String redirect;
    private Integer keepAlive;
    private String externalUrl;
    private List<MenuVO> children = new ArrayList<>();
}
