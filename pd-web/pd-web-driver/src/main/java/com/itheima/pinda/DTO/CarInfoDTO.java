package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class CarInfoDTO implements Serializable {
    /**
     * 车辆id
     */
    @Schema(description = "车辆id")
    private String id;

    /**
     * 品牌
     */
    @Schema(description = "品牌")
    private String brand;

    /**
     * 车型
     */
    @Schema(description = "车型")
    private String carModel;

    /**
     * 尺寸
     */
    @Schema(description = "尺寸")
    private String carSize;

    /**
     * 车辆牌照
     */
    @Schema(description = "车辆牌照")
    private String licensePlate;

    /**
     * 载重
     */
    @Schema(description = "载重")
    private String load;

    /**
     * 图片
     */
    @Schema(description = "图片")
    private String picture;

}
