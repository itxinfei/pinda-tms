package com.xinfei.auth.license;

/**
 * 授权状态。
 *
 * <ul>
 *   <li>{@code VALID}：持有有效 license key（已购买授权）</li>
 *   <li>{@code TRIAL}：未授权但处于试用期内（默认 7 天）</li>
 *   <li>{@code TRIAL_EXPIRED}：未授权且试用已到期</li>
 * </ul>
 */
public enum LicenseStatus {
    VALID,
    TRIAL,
    TRIAL_EXPIRED
}
