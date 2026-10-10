package com.itheima.pinda.vo.work;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.pinda.vo.oms.OrderVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "运单信息")
public class TransportOrderVo implements Serializable {
    private static final long serialVersionUID = 208202200909848030L;
    @Schema(description = "id")
    private String id;

    @Schema(description = "订单信息")
    private OrderVo order;

    @Schema(description = "运单状态(1.新建 2.已装车，发往x转运中心 3.到达 4.到达终端网点)")
    private Integer status;

    @Schema(description = "调度状态调度状态(1.待调度2.未匹配线路3.已调度)")
    private Integer schedulingStatus;

    @Schema(description = "创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "页尺寸")
    private Integer pageSize;

    @Schema(description = "取件信息")
    private TaskPickupDispatchVo taskPickup;

    @Schema(description = "派件信息")
    private TaskPickupDispatchVo taskDispatch;

    @Schema(description = "运输信息")
    private List<TaskTransportVo> taskTransports;
}
