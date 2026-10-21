package com.xinfei.service;

import java.util.List;

/**
 * 运单拆合单服务（ORD-05）
 */
public interface IWaybillSplitMergeService {

    /**
     * 合单：将多个待调度运单归入一个主单
     *
     * <p>D-50 同线路合单三要素（线路/时效/目的网点）因订单表字段不全降级为
     * 「同当前网点 + 同收件区县」校验（决策点已注明，待 D-50 字段落地后升级）。</p>
     *
     * @param mainId       主运单ID
     * @param childIds     子运单ID集合（不含主单）
     * @param operatorName 操作人
     * @throws IllegalArgumentException 校验不通过
     */
    void merge(Long mainId, List<Long> childIds, String operatorName);

    /**
     * 拆单：解除合并组，组内全部运单还原 normal
     *
     * @param mainId       主运单ID
     * @param operatorName 操作人
     * @throws IllegalArgumentException 校验不通过
     */
    void split(Long mainId, String operatorName);
}
