package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 合规决策留痕 —— P0-9 / FR-75（D-30）
 *
 * <p>改派(REASSIGN) / 运费调整(REPRICE) / 拒单取消(CANCEL) 三类决策的留痕：
 * 操作人 + 时间 + 枚举理由（白名单，不允许自由文本），留存 ≥3 年且可导出应对监管。
 * reason_detail 仅作补充备注，理由本体必须落 reason_enum。</p>
 */
@Data
@TableName("oms_decision_log")
public class DecisionLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 决策类型：运费调整 */
    public static final String TYPE_REPRICE = "REPRICE";
    /** 决策类型：拒单/取消 */
    public static final String TYPE_CANCEL = "CANCEL";
    /** 决策类型：改派（G2 落地时接入） */
    public static final String TYPE_REASSIGN = "REASSIGN";

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 决策类型: REPRICE/CANCEL/REASSIGN */
    private String businessType;

    /** 业务单据id */
    private Long businessId;

    /** 枚举理由（白名单校验） */
    private String reasonEnum;

    /** 理由补充说明（自由文本备注） */
    private String reasonDetail;

    /** 操作人 */
    private Long operator;

    /** 操作时间 */
    private LocalDateTime operateTime;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
