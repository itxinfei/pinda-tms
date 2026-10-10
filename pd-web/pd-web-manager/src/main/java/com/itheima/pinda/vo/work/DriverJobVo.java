package com.itheima.pinda.vo.work;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.itheima.pinda.vo.base.angency.AgencySimpleVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "司机作业单")
public class DriverJobVo implements Serializable {
    private static final long serialVersionUID = -4002290301421836423L;
    @Schema(description = "id")
    private String id;

    @Schema(description = "起始机构")
    private AgencySimpleVo startAgency;

    @Schema(description = "目的机构")
    private AgencySimpleVo endAgency;

    @Schema(description = "作业状态，1为待执行（对应 待提货）、2为进行中（对应在途）、3为改派（对应 已交付）、4为已完成（对应 已交付）、5为已作废")
    private Integer status;

    @Schema(description = "司机")
    private SysUserVo driver;

    @Schema(description = "运输任务")
    private TaskTransportVo taskTransport;

    @Schema(description = "提货对接人")
    private String startHandover;

    @Schema(description = "交付对接人")
    private String finishHandover;

    @Schema(description = "计划发车时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planDepartureTime;

    @Schema(description = "实际发车时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualDepartureTime;

    @Schema(description = "计划到达时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime planArrivalTime;

    @Schema(description = "实际到达时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime actualArrivalTime;

    @Schema(description = "创建时间")
    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss"
    )
    private LocalDateTime createTime;

    @Schema(description = "页码")
    private Integer page;

    @Schema(description = "页尺寸")
    private Integer pageSize;
}
