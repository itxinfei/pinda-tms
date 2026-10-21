package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 运单拆合留痕（ORD-05）
 */
@Data
@TableName("work_transport_split_merge_log")
public class TransportSplitMergeLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 操作类型：1合单 2拆单
     */
    private Integer opType;

    /**
     * 主运单ID（组主单）
     */
    private Long mainOrderId;

    /**
     * 涉及运单ID（合单=子单；拆单=被还原运单）
     */
    private Long memberOrderId;

    /**
     * 操作前父运单ID（拆单留痕）
     */
    private Long oldParentId;

    /**
     * 备注（如 D-50 降级校验口径）
     */
    private String remark;

    /**
     * 操作人
     */
    private String operatorName;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删 1 已删
     */
    @TableLogic
    private Integer deleted;
}
