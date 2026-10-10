package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class AddressInfoDTO implements Serializable {
    /**
     * 姓名
     */
    @Schema(description = "姓名")
    private String name;

    /**
     * 地址
     */
    @Schema(description = "地址")
    private String address;

    /**
     * 公司
     */
    @Schema(description = "公司")
    private String company;

    /**
     * 电话
     */
    @Schema(description = "电话")
    private String phoneNumber;

}
