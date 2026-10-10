package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

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

    @Schema(description = "作业范围")
    private List<AreaDTO> areas;
}
