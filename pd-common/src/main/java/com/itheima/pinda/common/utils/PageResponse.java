package com.itheima.pinda.common.utils;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

/**
 * 分页结果包装
 *
 * @author itcast
 */
@Data
@Schema(description = "分页数据统一对象")
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class PageResponse<T> {

    @Schema(description = "总条目数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long counts;

    @Schema(description = "页尺寸", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer pagesize;

    @Schema(description = "总页数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long pages;

    @Schema(description = "页码", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer page;

    @Schema(description = "数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<T> items;
}
