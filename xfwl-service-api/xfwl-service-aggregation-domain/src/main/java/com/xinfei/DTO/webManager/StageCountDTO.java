package com.xinfei.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 看板阶段统计项（DSP-01 六阶段流程条）。
 *
 * <p>下钻参数（drillType/drillField/drillStatuses）由后端下发，
 * 前端仅透传给既有分页接口，禁止在前端硬编码状态数字。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "看板阶段统计项")
public class StageCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "阶段标识：DISPATCH/ASSIGN/DEPART/TRANSIT/ARRIVE/DELIVER")
    private String key;

    @Schema(description = "阶段中文名")
    private String name;

    @Schema(description = "该阶段当前停留量")
    private Long count;

    @Schema(description = "下钻数据源：task=运输任务分页 transportOrder=运单分页")
    private String drillType;

    @Schema(description = "下钻过滤字段：status | schedulingStatus")
    private String drillField;

    @Schema(description = "下钻过滤状态值列表（到达阶段含到达/到达终端网点两个状态）")
    private List<Integer> drillStatuses;
}
