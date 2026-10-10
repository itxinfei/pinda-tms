package com.itheima.pinda.vo.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
    /**
     * 多边形经纬度坐标集合
     */
    @Schema(description = "多边形经纬度坐标集合")
    private List<List<Map>> mutiPoints;
}
