package com.xinfei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会员 DTO（跨服务传输模型，R2④ 替代直接暴露 Member 实体）。
 *
 * @author diesel
 * @since 2020-3-30
 */
@Data
@Schema(description = "会员")
public class MemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    /**
     * 身份证号
     */
    private String idCardNo;
    /**
     * 身份证号是否认证 1认证
     */
    private Integer idCardNoVerify;
    /**
     * 手机号
     */
    private String phone;
    /**
     * 头像
     */
    private String avatar;
    /**
     * 姓名（实体字段为 nickname）
     */
    private String name;
    /**
     * 状态：1 正常 0 禁用（禁用会员不可下单，见 USR-01）
     */
    private Integer status;
}
