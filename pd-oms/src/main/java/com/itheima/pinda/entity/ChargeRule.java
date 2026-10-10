package com.itheima.pinda.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 计费规则（Drools 脚本）
 *
 * <p>tenant_id=0 为系统默认规则；租户可维护各自版本。
 * 由 ReloadDroolsRulesService 加载构建 KieContainer。</p>
 */
@Data
@TableName("oms_charge_rule")
@Schema
public class ChargeRule implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 租户ID，0为系统默认
     */
    private Long tenantId;

    /**
     * 规则标识
     */
    private String ruleKey;

    /**
     * 版本
     */
    private String version;

    /**
     * 规则脚本
     */
    private String content;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删 1 已删
     */
    @TableLogic
    private Integer deleted;
}
