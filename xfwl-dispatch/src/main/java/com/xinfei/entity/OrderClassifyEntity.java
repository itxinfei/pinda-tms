package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 订单分类记录（disp_order_classify）
 *
 * <p>一次调度对订单按「起始机构#目的机构#当前机构」归组的结果。
 * 主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_order_classify")
public class OrderClassifyEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 定时任务ID
     */
    private Long jobId;

    /**
     * 任务日志ID
     */
    private Long jobLogId;

    /**
     * 起始机构ID
     */
    private Long startOrgId;

    /**
     * 目的机构ID
     */
    private Long endOrgId;

    /**
     * 订单类别:xxx#xxx#xxx
     */
    private String classify;

    /**
     * 组内订单数量
     */
    private Integer total;

    /**
     * 创建时间
     */
    private Date createTime;
}
