package com.itheima.pinda.vo.oms;

import com.itheima.pinda.vo.base.businessHall.GoodsTypeVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 货品总重量
 * </p>
 *
 * @author jpf
 * @since 2019-12-26
 */
@Data
@Schema(description = "货物信息")
public class OrderCargoVo implements Serializable {
    private static final long serialVersionUID = -2953242040093337789L;

    @Schema(description = "id")
    private String id;

    @Schema(description = "订单信息")
    private OrderVo order;

    @Schema(description = "运单id")
    private String tranOrderId;

    @Schema(description = "货物类型信息")
    private GoodsTypeVo goodsType;

    @Schema(description = "货物名称")
    private String name;

    @Schema(description = "货物单位")
    private String unit;

    @Schema(description = "货品货值")
    private BigDecimal cargoValue;

    @Schema(description = "货品条码")
    private String cargoBarcode;

    @Schema(description = "货品数量")
    private Integer quantity;

    @Schema(description = "货品体积")
    private BigDecimal volume;

    @Schema(description = "货品重量")
    private BigDecimal weight;

    @Schema(description = "货品备注")
    private String remark;

    @Schema(description = "货品总体积")
    private BigDecimal totalVolume;

    @Schema(description = "货品总重量")
    private BigDecimal totalWeight;
}
