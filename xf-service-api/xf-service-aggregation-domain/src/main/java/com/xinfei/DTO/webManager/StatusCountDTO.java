package com.xinfei.DTO.webManager;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 状态-计数统计项（含状态码，供看板图表点击下钻；中文名由后端按枚举真源给出）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "状态-计数统计项")
public class StatusCountDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "状态码（枚举真源值）")
    private Integer code;

    @Schema(description = "状态中文名（后端按枚举给出）")
    private String name;

    @Schema(description = "计数")
    private Long count;
}
