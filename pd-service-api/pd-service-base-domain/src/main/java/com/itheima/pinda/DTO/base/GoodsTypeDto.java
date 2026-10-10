package com.itheima.pinda.DTO.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * GoodsTypeDto
 */
@Data
public class GoodsTypeDto implements Serializable {
    private static final long serialVersionUID = -6890776123674482761L;
    /**
     * id
     */
    @Schema(description = "主键")
    private String id;
    /**
     * 货物类型名称
     */
    @Schema(description = "物品类型名称")
    @NotNull
    private String name;
    /**
     * 默认重量，单位：千克
     */
    @Schema(description = "默认重量")
    private BigDecimal defaultWeight;
    /**
     * 默认体积，单位：方
     */
    @Schema(description = "默认体积")
    private BigDecimal defaultVolume;
    /**
     * 说明
     */
    @Schema(description = "备注")
    private String remark;
    /**
     * 车辆类型id列表
     */
    private List<String> truckTypeIds;
    /**
     * 状态 0：禁用 1：正常
     */
    @Schema(description = "状态 0：禁用 1：正常")
    @NotNull
    @Max(value = 1)
    @Min(value = 0)
    private Integer status;
}