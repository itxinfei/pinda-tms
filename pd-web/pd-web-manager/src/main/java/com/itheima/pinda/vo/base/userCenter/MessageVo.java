package com.itheima.pinda.vo.base.userCenter;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "消息信息")
public class MessageVo implements Serializable {
    private static final long serialVersionUID = 6933377966443529574L;
    @Schema(description = "消息id")
    private String id;
    @Schema(description = "消息标题")
    private String title;
    @Schema(description = "消息正文")
    private String content;
    @Schema(description = "消息创建时间 格式：yyyy-MM-dd HH:mm:ss")
    private String createTime;
    @Schema(description = "消息类型：notice为通知,bulletin为公告")
    private String messageType;
    @Schema(description = "消息状态：0是已读，1是未读")
    private Integer status;
}
