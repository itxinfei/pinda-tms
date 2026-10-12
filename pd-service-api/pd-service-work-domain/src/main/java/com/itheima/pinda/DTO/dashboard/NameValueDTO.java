package com.itheima.pinda.DTO.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 大屏/仪表盘通用「名称-数值」统计项。
 *
 * <p>name 为后端按枚举真源给出的中文名称（如运输任务状态中文名），前端不做状态码映射。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "名称-数值统计项")
public class NameValueDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "名称（后端给中文）")
    private String name;

    @Schema(description = "数值")
    private Long value;
}
