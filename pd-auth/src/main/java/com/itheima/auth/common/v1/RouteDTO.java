package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态路由节点：对齐前端 vue3-element-admin 的 RouteItem（/menus/routes 输出）。
 * 字段名与前端一致（path/name/component/redirect/meta/children）。
 */
@Data
public class RouteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String path;
    private String name;
    private String component;
    private String redirect;
    private Meta meta;
    private List<RouteDTO> children = new ArrayList<>();

    @Data
    public static class Meta implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private String icon;
        private Boolean hidden;
        private Boolean keepAlive;
        private String externalUrl;
        /** 可访问角色（为空不限制；超管恒放行） */
        private List<String> roles;
    }
}
