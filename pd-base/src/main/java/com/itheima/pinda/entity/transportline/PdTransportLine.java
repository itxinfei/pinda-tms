package com.itheima.pinda.entity.transportline;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 业务线路
 */
@Data
@TableName("base_transport_line")
public class PdTransportLine {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 线路名称 */
    private String name;

    /** 线路编号（选填，编辑清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String lineNumber;

    /** 所属组织ID（必填，编辑始终回传） */
    private Long orgId;

    /** 线路类型ID（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long transportLineTypeId;

    /** 起始机构ID（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long startOrgId;

    /** 目的机构ID（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long endOrgId;

    /** 距离(km)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal distance;

    /** 成本(元)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal cost;

    /** 预计时间(分钟)（选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer estimatedTime;

    /** 1正常 0禁用 */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
