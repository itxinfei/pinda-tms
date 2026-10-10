package com.itheima.pinda.vo.base.angency;

import com.itheima.pinda.vo.base.AreaSimpleVo;
import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "机构信息")
public class AgencyVo implements Serializable {
    private static final long serialVersionUID = 3417821750914521294L;
    @Schema(description = "id")
    private String id;

    @Schema(description = "机构名称")
    private String name;

    @Schema(description = "机构类型 1为分公司，2为一级转运中心 3为二级转运中心 4为网点")
    private Integer agencyType;

    @Schema(description = "机构类型名称")
    private String agencyTypeName;

    @Schema(description = "所属省份")
    private AreaSimpleVo province;

    @Schema(description = "所属城市")
    private AreaSimpleVo city;

    @Schema(description = "所属区县")
    private AreaSimpleVo county;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "经度")
    private String longitude;

    @Schema(description = "纬度")
    private String latitude;

    @Schema(description = "父级机构")
    private AgencySimpleVo parent;

    @Schema(description = "负责人")
    private SysUserVo manager;

    @Schema(description = "联系电话")
    private String contractNumber;

    @Schema(description = "状态 0：禁用 1：正常")
    private Integer status;
}
