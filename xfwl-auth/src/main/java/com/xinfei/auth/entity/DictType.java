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
 * 字典类型（sys_dict_type）。租户私有，仅承载"运营可配分类"。
 *
 * <p>业务状态机枚举不入此表（《62》§10.5：走《07》+ 前端 src/enums，防止状态被后台改坏）。
 * code 租户内唯一，因逻辑删除不设 DB 唯一索引，唯一性由 DictService 校验。</p>
 */
@Data
@TableName("sys_dict_type")
public class DictType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 所属租户ID（租户拦截器自动写入与过滤） */
    private Long tenantId;

    /** 字典名称 */
    private String name;

    /** 字典编码（前端 dictCode，租户内唯一） */
    private String code;

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
