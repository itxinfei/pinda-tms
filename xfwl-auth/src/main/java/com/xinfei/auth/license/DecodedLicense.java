package com.xinfei.auth.license;

import lombok.Data;

import java.io.Serializable;

/** 验签通过后的 license 明文载荷。 */
@Data
public class DecodedLicense implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 授权对象（企业/个人名称） */
    private String licensee;
    /** 许可类型：ENTERPRISE / PERSONAL / OEM */
    private String type;
    /** 授权到期时间戳(ms)；0 或 null 表示永久 */
    private Long expiresAt;
    /** 允许实例数 */
    private int instances = 1;
}
