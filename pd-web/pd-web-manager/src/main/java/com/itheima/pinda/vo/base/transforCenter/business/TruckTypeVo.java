package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.businessHall.GoodsTypeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "车辆类型")
public class TruckTypeVo implements Serializable {
    private static final long serialVersionUID = 3017388977673057982L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "车辆类型名称")
    private String name;
    @Schema(description = "准载重量")
    private BigDecimal allowableLoad;
    @Schema(description = "准载体积")
    private BigDecimal allowableVolume;
    @Schema(description = "长")
    private BigDecimal measureLong;
    @Schema(description = "宽")
    private BigDecimal measureWidth;
    @Schema(description = "高")
    private BigDecimal measureHigh;
    @Schema(description = "货物类型列表")
    private List<GoodsTypeVo> goodsTypes;
}
