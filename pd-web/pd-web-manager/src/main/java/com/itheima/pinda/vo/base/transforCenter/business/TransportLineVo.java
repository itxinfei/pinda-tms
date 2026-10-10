package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.angency.AgencyVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Schema(description = "线路信息")
public class TransportLineVo implements Serializable {
    private static final long serialVersionUID = 5953547594480342286L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "线路名称")
    private String name;
    @Schema(description = "线路编号")
    private String lineNumber;
    @Schema(description = "所属机构")
    private AgencyVo agency;
    @Schema(description = "线路类型")
    private TransportLineTypeVo transportLineType;
    @Schema(description = "起始地机构")
    private AgencyVo startAgency;
    @Schema(description = "目的地机构")
    private AgencyVo endAgency;
    @Schema(description = "距离")
    private BigDecimal distance;
    @Schema(description = "成本")
    private BigDecimal cost;
    @Schema(description = "预计时间")
    private BigDecimal estimatedTime;
}
