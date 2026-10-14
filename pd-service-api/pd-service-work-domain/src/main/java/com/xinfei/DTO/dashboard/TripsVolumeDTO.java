package com.xinfei.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 按车次统计的运输任务量（大屏「各线路运量」柱状图的中间数据）。
 *
 * <p>pd-work 只掌握任务与车次(base_transport_trips)的关联，线路名称由 BFF
 * 经 TransportTripsFeign + TransportLineFeign 补全，本类不带线路名。</p>
 */
@Data
@Schema(description = "车次任务量统计项")
public class TripsVolumeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "车次 id（String 边界，与 base Feign 对齐）")
    private String tripsId;

    @Schema(description = "近 N 日该车次关联的运输任务数")
    private Long taskCount;

    public TripsVolumeDTO() {
    }

    public TripsVolumeDTO(String tripsId, Long taskCount) {
        this.tripsId = tripsId;
        this.taskCount = taskCount;
    }
}
