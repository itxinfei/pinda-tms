package com.xinfei.auth.license;

import lombok.Data;

import java.io.Serializable;

/**
 * 授权状态响应 VO（前端授权页/水印/提示消费）。授权真伪一律以后端本对象为准。
 */
@Data
public class LicenseInfo implements Serializable {
    private static final long serialVersionUID = 2L;

    /** 授权状态 */
    private LicenseStatus status;
    /** 是否处于已激活态（VALID/EMERGENCY/GRACE），前端据此决定水印与页面态 */
    private boolean activated;

    /** 授权对象（未授权为 null） */
    private String licensee;
    /** 许可类型（未授权为 null） */
    private String type;
    /** 实例数 */
    private Integer instances;
    /** 签发时间(ms) */
    private Long issuedAt;
    /** 到期时间(ms) */
    private Long expiresAt;

    /** 授权码绑定的机器指纹（应急码为 null） */
    private String boundMachineId;
    /** 本机机器指纹（64 位 hex），未激活也返回，供页面展示/复制给作者签发 */
    private String fingerprint;
    /** 本机机器码的分段展示形式 */
    private String fingerprintMasked;
    /** 当前授权来源：ENV（环境变量只读）/ FILE（激活落盘）/ NONE（无） */
    private String source;

    /** 试用剩余天数（仅 TRIAL 有值） */
    private Integer trialDaysLeft;
    /** 宽限剩余天数（仅 GRACE 有值） */
    private Integer graceDaysLeft;
    /** 应急码剩余天数（仅 EMERGENCY 有值） */
    private Integer emergencyDaysLeft;

    /** 前端展示文案 */
    private String message;
}
