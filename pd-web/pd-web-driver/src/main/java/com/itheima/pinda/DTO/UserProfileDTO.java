package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class UserProfileDTO implements Serializable {
    /**
     * id
     */
    @Schema(description = "id")
    private String id;

    /**
     * 用户头
     */
    @Schema(description = "用户头像")
    private String avatar;

    /**
     * 负责人
     */
    @Schema(description = "负责人")
    private String manager;

    /**
     * 用户姓名
     */
    @Schema(description = "用户姓名")
    private String name;

    /**
     * 手机号码
     */
    @Schema(description = "手机号码")
    private String phone;

    /**
     * 所属车队
     */
    @Schema(description = "所属车队")
    private String team;

    /**
     * 所属转运中心
     */
    @Schema(description = "所属转运中心")
    private String transport;

    /**
     * 司机编号
     */
    @Schema(description = "司机编号")
    private String userNumber;

    /**
     * 车辆id
     */
    @Schema(description = "车辆id")
    private String truckId;

    @Schema(description = "车牌号")
    private String licensePlate;

    @Schema(description = "运输任务id")
    private String transportTaskId;

}
