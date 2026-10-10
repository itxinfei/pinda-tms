package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 组织（部门）。租户内树形结构，tree_path 以逗号包裹记录所有祖先，便于子孙查询。
 */
@Data
@TableName("sys_org")
public class CoreOrg {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（由租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 名称 */
    private String name;

    /** 简称 */
    private String abbreviation;

    /** 父组织ID，根节点为 0 */
    private Long parentId;

    /** 部门类型 1分公司 2一级转运中心 3二级转运中心 4网点 */
    private Integer orgType;

    /** 省行政区划ID（dict_area.id） */
    private Integer provinceId;

    /** 市行政区划ID（dict_area.id） */
    private Integer cityId;

    /** 区县行政区划ID（dict_area.id） */
    private Integer countyId;

    /** 详细地址 */
    private String address;

    /** 联系电话 */
    private String contractNumber;

    /** 负责人ID */
    private Long managerId;

    /** 树路径，形如 ,1,5, 以逗号包裹，根节点为 , */
    private String treePath;

    /** 排序值，升序 */
    private Integer sortValue;

    /** 启用状态：1 启用 0 禁用 */
    private Integer status;

    /** 描述 */
    private String description;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 营业时间 */
    private String businessHours;

    /** 子组织（仅树形查询时装配，非表字段） */
    @TableField(exist = false)
    private List<CoreOrg> children;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
