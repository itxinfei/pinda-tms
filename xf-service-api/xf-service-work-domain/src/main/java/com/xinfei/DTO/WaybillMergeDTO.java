package com.xinfei.DTO;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 运单合单入参（ORD-05，xf-work / BFF / Feign 共用）
 */
@Data
public class WaybillMergeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主运单ID
     */
    private Long mainId;

    /**
     * 子运单ID集合（不含主单）
     */
    private List<Long> childIds;
}
