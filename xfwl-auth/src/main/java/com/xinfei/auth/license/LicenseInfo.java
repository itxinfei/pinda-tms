package com.xinfei.auth.license;

import lombok.Data;

import java.io.Serializable;

/** 授权状态响应 VO（前端 banner 消费）。 */
@Data
public class LicenseInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 授权状态 */
    private LicenseStatus status;
    /** 授权对象（未授权为 null） */
    private String licensee;
    /** 许可类型（未授权为 null） */
    private String type;
    /** 授权到期时间戳(ms)，未授权为 null */
    private Long expiresAt;
    /** 试用剩余天数（已授权为 null） */
    private Integer trialDaysLeft;
    /** 前端展示文案 */
    private String message;
}
