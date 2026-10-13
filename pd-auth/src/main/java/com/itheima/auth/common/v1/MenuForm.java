package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单新建/编辑入参（对齐前端 MenuForm）。params/CRUD 按钮生成等前端辅助字段忽略。
 */
@Data
public class MenuForm implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String parentId;
    private String type;
    private String name;
    private String path;
    private String routeName;
    private String routePath;
    private String component;
    private String perm;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer scope;
    private String redirect;
    private Object keepAlive;
    private String externalUrl;
}
