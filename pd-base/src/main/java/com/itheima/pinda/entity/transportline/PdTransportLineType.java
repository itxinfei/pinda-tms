package com.itheima.pinda.entity.transportline;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线路类型
 */
@Data
@TableName("base_transport_line_type")
public class PdTransportLineType {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 线路类型名称 */
    private String name;

    /** 编号 */
    private String typeNumber;

    /** 起始机构类型（对齐 sys_org.org_type） */
    private Integer startAgencyType;

    /** 目的机构类型 */
    private Integer endAgencyType;

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
