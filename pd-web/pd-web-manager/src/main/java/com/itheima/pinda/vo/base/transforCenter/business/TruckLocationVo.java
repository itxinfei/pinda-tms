package com.itheima.pinda.vo.base.transforCenter.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "车辆位置信息")
public class TruckLocationVo implements Serializable {
    private static final long serialVersionUID = 8540398115062991784L;
    @Schema(description = "姓名")
    private String name;
    @Schema(description = "电话")
    private String mobile;
    @Schema(description = "头像")
    private String avatar;
    @Schema(description = "车辆类型名称")
    private String truckTypeName;
    @Schema(description = "车牌号")
    private String licensePlate;
}
