package com.itheima.pinda.vo.base.businessHall;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

import com.itheima.pinda.vo.base.transforCenter.business.TruckTypeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * GoodsTypeVo
 */
@Data
@Schema(description = "货物类型信息")
public class GoodsTypeVo implements Serializable {
    private static final long serialVersionUID = -6733081065775505503L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "货物类型名称")
    private String name;
    @Schema(description = "默认重量，单位：千克")
    private BigDecimal defaultWeight;
    @Schema(description = "默认体积，单位：方")
    private BigDecimal defaultVolume;
    @Schema(description = "说明")
    private String remark;
    @Schema(description = "车辆类型")
    private List<TruckTypeVo> truckTypes;
}