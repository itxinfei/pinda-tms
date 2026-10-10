package com.itheima.pinda.vo.base.angency;

import com.itheima.pinda.vo.base.AreaSimpleVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@Schema(description = "机构业务范围信息")
public class AgencyScopeVo implements Serializable {
    private static final long serialVersionUID = -7364866310440069186L;
    @Schema(description = "机构信息")
    private AgencyVo agency;
    @Schema(description = "业务范围")
    private List<AreaSimpleVo> areas;
}
