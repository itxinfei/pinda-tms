package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.DecisionLog;

import java.util.List;
import java.util.Map;

/**
 * 合规决策留痕 Service（P0-9 / FR-75：操作人+时间+枚举理由，留存 ≥3 年可导出）
 */
public interface IDecisionLogService extends IService<DecisionLog> {

    /**
     * 落留痕（枚举理由白名单校验，非法理由抛 IllegalArgumentException）。
     *
     * @param businessType 决策类型 REPRICE/CANCEL/REASSIGN
     * @param businessId   业务单据id
     * @param reasonEnum   枚举理由（必须在该类型白名单内）
     * @param detail       补充说明（可空）
     * @param operator     操作人
     */
    DecisionLog apply(String businessType, Long businessId, String reasonEnum, String detail, Long operator);

    /**
     * 分页查询（按类型/单据过滤）。
     */
    Page<DecisionLog> page(Integer page, Integer pageSize, String businessType, Long businessId);

    /**
     * 导出行（监管导出：按类型/单据/时间范围）。
     *
     * @return 每行 {operateTime, businessType, businessId, reasonEnum, reasonDetail, operator}
     */
    List<Map<String, Object>> exportRows(String businessType, Long businessId,
                                         String beginTime, String endTime);
}
