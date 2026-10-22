package com.xinfei.auth.license;

/**
 * 授权状态（机器绑定版）。
 *
 * <ul>
 *   <li>{@code VALID}：正式码验签通过、未到期、机器指纹匹配（已激活，灭购买提示水印）</li>
 *   <li>{@code EMERGENCY}：应急码有效（不绑机、短期，红色提示剩余天数；算激活）</li>
 *   <li>{@code GRACE}：正式码已到期但仍在宽限期内（算激活，强提示尽快续期）</li>
 *   <li>{@code TRIAL}：无有效授权、试用中（7 天，按机器指纹首见时间）</li>
 *   <li>{@code TRIAL_EXPIRED}：试用已到期</li>
 *   <li>{@code MISMATCH}：授权码有效但绑定的是别的机器（拒绝在本机使用）</li>
 *   <li>{@code EXPIRED}：正式码过期且超宽限，或应急码已过期</li>
 * </ul>
 */
public enum LicenseStatus {
    VALID,
    EMERGENCY,
    GRACE,
    TRIAL,
    TRIAL_EXPIRED,
    MISMATCH,
    EXPIRED;

    /** 该状态是否视为"已激活"（业务可用、不显示购买提示水印）。 */
    public boolean isActivated() {
        return this == VALID || this == EMERGENCY || this == GRACE;
    }
}
