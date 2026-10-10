package com.itheima.pinda.DTO;

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
     * 认证id
     */
    private String authId;
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
     * 姓名
     */
    private String name;
}
