package com.xinfei.auth.license;

import lombok.Data;

import java.io.Serializable;

/**
 * 验签通过后的 license 明文载荷（机器绑定版协议）。
 *
 * <p>授权码 = {@code base64url(payloadJson) + "." + base64url(RSA-SHA256 签名)}，
 * 由独立签发库 xinfei-license 用作者私钥签发，本服务只用内置公钥验签。</p>
 *
 * <ul>
 *   <li>正式码 {@link #TYPE_PERSONAL}/{@link #TYPE_ENTERPRISE}/{@link #TYPE_OEM}：
 *       {@code machineId} 必填且须与本机指纹一致；</li>
 *   <li>应急码 {@link #TYPE_EMERGENCY}：不绑机（machineId 为空），寿命受后端上限约束。</li>
 * </ul>
 */
@Data
public class DecodedLicense implements Serializable {
    private static final long serialVersionUID = 2L;

    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String TYPE_ENTERPRISE = "ENTERPRISE";
    public static final String TYPE_OEM = "OEM";
    public static final String TYPE_EMERGENCY = "EMERGENCY";
    /** 历史体验码类型：新协议不再签发，命中按未绑机的非正式码拒绝激活 */
    public static final String TYPE_TRIAL = "TRIAL";

    /** 授权对象（企业/个人名称） */
    private String licensee;
    /** 许可类型：PERSONAL / ENTERPRISE / OEM / EMERGENCY / TRIAL */
    private String type;
    /** 允许实例数（当前仅记录，商业化阶段再强制） */
    private int instances = 1;
    /** 绑定的机器指纹（64 位 hex）；应急码为 null */
    private String machineId;
    /** 签发时间(ms) */
    private Long issuedAt;
    /** 到期时间(ms) */
    private Long expiresAt;
    /** 受众，固定 xinfei-tms */
    private String aud;

    public boolean isEmergency() {
        return TYPE_EMERGENCY.equalsIgnoreCase(type);
    }
}
