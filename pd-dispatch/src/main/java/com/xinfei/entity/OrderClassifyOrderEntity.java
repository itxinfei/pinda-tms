package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 分类-订单关联表（disp_classify_order）
 *
 * <p>订单ID（orderId）落库为 BIGINT；pd-oms 侧以 String 协议交互，落库点完成转换。
 * 主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_classify_order")
public class OrderClassifyOrderEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类ID
     */
    private Long classifyId;

    /**
     * 订单ID（oms_order.id，落库 BIGINT）
     */
    private Long orderId;
}
