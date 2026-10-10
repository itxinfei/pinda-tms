package com.itheima.pinda.vo.base.userCenter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(description = "密码重置")
@Data
public class PasswordResetVo implements Serializable {
    private static final long serialVersionUID = 3260392307623280638L;
    @Schema(description = "原始密码")
    private String sourcePassword;
    @Schema(description = "新密码")
    private String newPassword;
}
