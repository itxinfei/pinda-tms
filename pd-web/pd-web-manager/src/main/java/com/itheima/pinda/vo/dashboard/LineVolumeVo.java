package com.itheima.pinda.vo.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 大屏「各线路运量」柱状图单项：线路名 + 近 N 日运输任务量。
 */
@Data
@Schema(description = "线路运量统计项")
public class LineVolumeVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "线路名称（补不到线路时用车次名）")
    private String name;

    @Schema(description = "近 N 日运输任务量")
    private Long taskCount;

    public LineVolumeVo() {
    }

    public LineVolumeVo(String name, Long taskCount) {
        this.name = name;
        this.taskCount = taskCount;
    }
}
