package com.itheima.pinda.entity.transportline;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 车次（运输班次）
 */
@Data
@TableName("base_transport_trips")
public class PdTransportTrips {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 车次名称 */
    private String name;

    /** 发车时间 HH:mm */
    private String departureTime;

    /** 所属线路ID */
    private Long transportLineId;

    /** 周期 1天 2周 3月 */
    private Integer period;

    /** 1正常 0禁用 */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
