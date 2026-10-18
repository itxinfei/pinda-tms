package com.xinfei.DTO;

import lombok.Data;

import java.util.List;

@Data
public class MailingQueryDTO {
    private String keyword;
    private Integer page;
    private Integer pagesize;
    private Integer mailType;
    /** 订单状态筛选（逗号绑定显式 code 列表，空=不筛；档位来自 /mailing/status-filters） */
    private List<Integer> statusList;
}
