package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 菜单/按钮（全局平台表，不带租户隔离）。
 *
 * <p>type：C 目录、M 菜单、E 外链、B 按钮。按钮(type=B)的 {@link #perm} 同时供
 * 前端 v-hasPerm 与后端 @SaCheckPermission 使用；按钮不参与路由生成。</p>
 */
@Data
@TableName("sys_menu")
public class SysMenu {

    /** 固定种子ID用短ID；新增走雪花 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 父菜单ID，根为0 */
    private Long parentId;

    /** C目录 M菜单 E外链 B按钮 */
    private String type;

    /** 菜单/按钮名称 */
    private String name;

    /** 路由 name（需唯一，keep-alive 依赖） */
    private String routeName;

    /** 路由 path：目录相对段；菜单 / 开头为绝对路径（不拼父级） */
    private String path;

    /** 前端组件（views 下相对路径，如 order/index；目录为空，按钮为 null） */
    private String component;

    /** 权限标识，如 sys:user:add；按钮必填 */
    private String perm;

    private String icon;

    private Integer sortValue;

    /** 是否在菜单显示：1显示 0隐藏 */
    private Integer visible;

    /** 1启用 0禁用 */
    private Integer status;

    /** 目录默认跳转 */
    private String redirect;

    /** 是否缓存：1是 0否 */
    private Integer keepAlive;

    /** 外链地址（type=E） */
    private String externalUrl;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
