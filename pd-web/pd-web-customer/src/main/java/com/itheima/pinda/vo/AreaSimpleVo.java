package com.itheima.pinda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "行政区域简要信息")
public class AreaSimpleVo implements Serializable {
    private static final long serialVersionUID = 5473514348905248093L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "行政名称")
    private String name;
    @Schema(description = "经度")
    private String lng;
    @Schema(description = "纬度")
    private String lat;
}
