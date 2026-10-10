package com.itheima.pinda.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 快递员作业范围（电子围栏）
 */
@Data
@TableName("base_courier_scope")
public class PdCourierScope {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 快递员账号 sys_user.id */
    private Long courierId;

    /** 行政区划 dict_area.id */
    private Integer areaId;

    /** 围栏多边形点串（JSON） */
    private String polygonPoints;
}
