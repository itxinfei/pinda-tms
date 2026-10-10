package com.itheima.pinda.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "机构简要信息")
public class AgencySimpleVo implements Serializable {
    private static final long serialVersionUID = -6300342950882936227L;
    @Schema(description = "id")
    private String id;

    @Schema(description = "机构名称")
    private String name;

    @Schema(description = "子部门简要信息列表")
    private List<AgencySimpleVo> subAgencies;
}
