package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 组织（机构）DTO：供微服务间 Feign 调用。
 * 字段按 pd-web / pd-dispatch 实际访问裁剪；manager 为负责人姓名，由服务端按 managerId 装配。
 */
@Data
@Schema(description = "组织信息")
public class OrgDTO {

    @Schema(description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织类型 1分公司 2一级转运中心 3二级转运中心 4网点")
    private Integer orgType;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "经度")
    private String longitude;

    @Schema(description = "纬度")
    private String latitude;

    @Schema(description = "联系电话")
    private String contractNumber;

    @Schema(description = "状态 true启用 false禁用")
    private Boolean status;

    @Schema(description = "负责人姓名")
    private String manager;

    @Schema(description = "负责人ID")
    private Long managerId;

    @Schema(description = "父级组织ID，根为0")
    private Long parentId;

    @Schema(description = "省行政区划ID")
    private Long provinceId;

    @Schema(description = "市行政区划ID")
    private Long cityId;

    @Schema(description = "区行政区划ID")
    private Long countyId;
}
