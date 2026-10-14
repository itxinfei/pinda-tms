package com.xinfei.DTO;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 告警处置入参（PUT /alarm/{id}/handle）
 *
 * <p>处理人不入参，一律取自登录态（Sa-Token），防止冒充他人处置留痕。</p>
 */
@Data
@Schema(description = "告警处置入参")
public class AlarmHandleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "处置说明（可选，超 500 字符按列宽截断）")
    private String remark;
}
