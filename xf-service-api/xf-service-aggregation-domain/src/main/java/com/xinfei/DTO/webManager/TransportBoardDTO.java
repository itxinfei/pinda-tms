package com.xinfei.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 运输闭环看板聚合数据（DSP-01）。
 *
 * <p>由 xf-aggregation 按运输任务/运单状态实时聚合；阶段与状态的中文口径
 * 均由后端按《数据字典与枚举口径》枚举真源给出，前端不做状态码映射与硬编码。</p>
 */
@Data
@Schema(description = "运输闭环看板聚合数据")
public class TransportBoardDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "六阶段流程计数（调度→分配→发车→在途→到达→交付）")
    private List<StageCountDTO> stages = new ArrayList<>();

    @Schema(description = "运输任务状态分布（含状态码，供下钻）")
    private List<StatusCountDTO> taskStatusCounts = new ArrayList<>();

    @Schema(description = "运单状态分布（含状态码，供下钻）")
    private List<StatusCountDTO> transportOrderStatusCounts = new ArrayList<>();
}
