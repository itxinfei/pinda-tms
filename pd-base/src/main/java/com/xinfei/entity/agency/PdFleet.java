package com.xinfei.entity.agency;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车队
 */
@Data
@TableName("base_fleet")
public class PdFleet {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;


    /** 车队名称 */
    private String name;

    /** 车队编号（选填，编辑清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String fleetNumber;

    /** 所属组织ID（必填，编辑始终回传） */
    private Long orgId;

    /** 负责人（userId，选填，清空需落 null） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String manager;

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
