package com.xinfei.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 机器指纹试用台账（平台级，无 tenant_id）。
 *
 * <p>按机器指纹记录"首次出现时间"，用于未授权实例的 7 天试用计时。落库而非写容器/卷内文件，
 * 因此重建容器、删除 license 卷都不会刷新试用期。授权对象是机器而非浏览器，故不带用户/租户维度。</p>
 */
@Data
@TableName("sys_license_usage")
public class SysLicenseUsage {

    /** 机器指纹（64 位 SHA-256 hex），即主键 */
    @TableId(type = IdType.INPUT)
    private String fingerprint;

    /** 该机器首次出现（开始试用）时间 */
    private LocalDateTime firstSeenAt;

    /** 最近一次上报状态时间 */
    private LocalDateTime lastSeenAt;
}
