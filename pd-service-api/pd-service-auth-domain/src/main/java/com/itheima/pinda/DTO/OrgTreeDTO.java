package com.itheima.pinda.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 组织树 DTO：继承组织信息，附加下级子节点。
 * 保持与旧契约一致的 extends Org + children 形态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "组织树节点")
public class OrgTreeDTO extends OrgDTO {

    @Schema(description = "下级组织节点")
    private List<OrgTreeDTO> children;
}
