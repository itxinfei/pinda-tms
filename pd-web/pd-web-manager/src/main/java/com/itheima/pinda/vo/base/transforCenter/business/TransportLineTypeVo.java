package com.itheima.pinda.vo.base.transforCenter.business;

import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "线路类型")
public class TransportLineTypeVo implements Serializable {
    private static final long serialVersionUID = -6081155291135396513L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "线路类型名称")
    private String name;
    @Schema(description = "线路类型编码")
    private String typeNumber;
    @Schema(description = "起始地机构类型")
    private Integer startAgencyType;
    @Schema(description = "起始地机构类型名称")
    private String startAgencyTypeName;
    @Schema(description = "目的地机构类型")
    private Integer endAgencyType;
    @Schema(description = "目的地机构类型名称")
    private String endAgencyTypeName;
    @Schema(description = "最后更新时间")
    private String lastUpdateTime;
    @Schema(description = "更新人")
    private SysUserVo updater;
    @Schema(description = "状态 0：禁用 1：正常")
    private Integer status;
}
