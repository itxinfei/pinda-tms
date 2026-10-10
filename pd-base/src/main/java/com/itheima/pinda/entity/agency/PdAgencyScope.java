package com.itheima.pinda.entity.agency;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 机构作业范围（电子围栏）
 */
@Data
@TableName("base_org_scope")
public class PdAgencyScope {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 机构 sys_org.id */
    private Long orgId;

    /** 行政区划 dict_area.id */
    private Integer areaId;

    /** 围栏多边形点串（JSON） */
    private String polygonPoints;
}
