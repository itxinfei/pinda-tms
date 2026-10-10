package com.itheima.pinda.vo.base.angency;

import com.itheima.pinda.vo.base.userCenter.SysUserVo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "角色信息")
public class RoleVo implements Serializable {
    private static final long serialVersionUID = 1597893585759987194L;
    @Schema(description = "id")
    private String id;
    @Schema(description = "角色名称")
    private String name;
    @Schema(description = "机构类型")
    private Integer agencyType;
    @Schema(description = "机构类型名称")
    private String agencyTypeName;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "最近更新时间")
    private String lastUpdateTime;
    @Schema(description = "最近更新人")
    private SysUserVo updater;
}
