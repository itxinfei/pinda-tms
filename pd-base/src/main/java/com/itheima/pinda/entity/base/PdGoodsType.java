package com.itheima.pinda.entity.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 货物类型
 */
@Data
@TableName("base_goods_type")
public class PdGoodsType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 货物类型名称 */
    private String name;

    /** 默认重量(kg) */
    private BigDecimal defaultWeight;

    /** 默认体积(m³) */
    private BigDecimal defaultVolume;

    /** 说明 */
    private String remark;

    /** 1启用 0禁用 */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
