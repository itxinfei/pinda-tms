package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.CustomIdGenerator;
import com.xinfei.entity.DecisionLog;
import com.xinfei.mapper.DecisionLogMapper;
import com.xinfei.service.IDecisionLogService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 合规决策留痕 Service 实现（P0-9 / FR-75）
 *
 * <p>理由本体必须落枚举（白名单校验，不允许自由文本）；detail 仅作补充备注。
 * 落库失败向上抛（合规留痕失败不静默——决策动作一并回滚由调用方决定）。</p>
 */
@Slf4j
@Service
public class DecisionLogServiceImpl extends ServiceImpl<DecisionLogMapper, DecisionLog>
        implements IDecisionLogService {

    /** 枚举理由白名单：按决策类型限定（新增理由须先扩此表并同步前端下拉） */
    private static final Map<String, Set<String>> REASON_ENUMS = Map.of(
            DecisionLog.TYPE_REPRICE, Set.of("COST_CORRECTION", "QUOTATION_ERROR", "CUSTOMER_DISCOUNT"),
            DecisionLog.TYPE_CANCEL, Set.of("CUSTOMER_CANCEL", "CARGO_DAMAGE", "ADDRESS_ERROR", "OTHER"),
            DecisionLog.TYPE_REASSIGN, Set.of("DRIVER_OFFLINE", "VEHICLE_FAULT", "CUSTOMER_REQUEST", "SCHEDULE_CONFLICT"));

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CustomIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DecisionLog apply(String businessType, Long businessId, String reasonEnum, String detail, Long operator) {
        if (StringUtils.isBlank(businessType) || businessId == null) {
            throw new IllegalArgumentException("决策类型与业务单据id必填");
        }
        Set<String> allowed = REASON_ENUMS.get(businessType);
        if (allowed == null) {
            throw new IllegalArgumentException("决策类型非法: " + businessType);
        }
        if (StringUtils.isBlank(reasonEnum) || !allowed.contains(reasonEnum)) {
            throw new IllegalArgumentException("枚举理由非法: " + reasonEnum
                    + "（允许 " + allowed + "）");
        }
        LocalDateTime now = LocalDateTime.now();
        DecisionLog entity = new DecisionLog();
        entity.setId(idGenerator.nextId(new DecisionLog()));
        entity.setBusinessType(businessType);
        entity.setBusinessId(businessId);
        entity.setReasonEnum(reasonEnum);
        entity.setReasonDetail(detail);
        entity.setOperator(operator);
        entity.setOperateTime(now);
        entity.setCreateTime(now);
        entity.setUpdateTime(now);
        baseMapper.insert(entity);
        log.warn("[决策留痕] {} businessId={} reason={} operator={}", businessType, businessId, reasonEnum, operator);
        return entity;
    }

    @Override
    public Page<DecisionLog> page(Integer page, Integer pageSize, String businessType, Long businessId) {
        Page<DecisionLog> p = new Page<>(page == null || page < 1 ? 1 : page,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<DecisionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(businessType), DecisionLog::getBusinessType, businessType)
                .eq(businessId != null, DecisionLog::getBusinessId, businessId)
                .orderByDesc(DecisionLog::getOperateTime);
        return baseMapper.selectPage(p, wrapper);
    }

    @Override
    public List<Map<String, Object>> exportRows(String businessType, Long businessId, String beginTime, String endTime) {
        LambdaQueryWrapper<DecisionLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(businessType), DecisionLog::getBusinessType, businessType)
                .eq(businessId != null, DecisionLog::getBusinessId, businessId)
                .ge(StringUtils.isNotBlank(beginTime), DecisionLog::getOperateTime, beginTime)
                .le(StringUtils.isNotBlank(endTime), DecisionLog::getOperateTime, endTime)
                .orderByAsc(DecisionLog::getOperateTime);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (DecisionLog d : baseMapper.selectList(wrapper)) {
            Map<String, Object> row = new HashMap<>();
            row.put("operateTime", d.getOperateTime() == null ? "" : d.getOperateTime().format(FMT));
            row.put("businessType", d.getBusinessType());
            row.put("businessId", d.getBusinessId());
            row.put("reasonEnum", d.getReasonEnum());
            row.put("reasonDetail", d.getReasonDetail());
            row.put("operator", d.getOperator());
            rows.add(row);
        }
        return rows;
    }
}
