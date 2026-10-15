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
 * 字典项（sys_dict_item）。按 dict_type_id 归属字典类型，接口以字典编码寻址。
 */
@Data
@TableName("sys_dict_item")
public class DictItem {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 所属字典类型ID */
    private Long dictTypeId;

    /** 字典项标签 */
    private String label;

    /** 字典项值 */
    private String value;

    /** 标签样式语义全称：primary/success/warning/danger/info，空串=默认 */
    private String tagType;

    /** 排序（升序） */
    private Integer sortValue;

    /** 启用状态：1 启用 0 停用 */
    private Integer status;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
