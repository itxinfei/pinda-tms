package com.xinfei.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class TransportOrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;


    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 运单状态(1.新建 2.已装车，发往x转运中心 3.到达 4.到达终端网点)
     */
    private Integer status;

    /**
     * 调度状态调度状态(1.待调度2.未匹配线路3.已调度)
     */
    private Integer schedulingStatus;

    /**
     * 父运单ID（合单时指向主运单，ORD-05）
     */
    private Long parentId;

    /**
     * 合并状态（ORD-05）：normal 普通 / merged 合并组主单 / split_child 合并组子单
     */
    private String mergeStatus;

    /**
     * 创建时间
     */
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;
}
