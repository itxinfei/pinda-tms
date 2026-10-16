package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.DTO.OrderDTO;
import com.xinfei.entity.TransportOrder;
import com.xinfei.entity.TransportSplitMergeLog;
import com.xinfei.enums.transportorder.TransportOrderSchedulingStatus;
import com.xinfei.feign.OrderFeign;
import com.xinfei.mapper.TransportSplitMergeLogMapper;
import com.xinfei.service.IWaybillSplitMergeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 运单拆合单服务实现（ORD-05）
 *
 * <p>口径：
 * ① 不动运单 6 状态机，仅维护 parent_id/merge_status；
 * ② 仅调度前（scheduling_status=1 待调度）运单可合拆；
 * ③ 合单=多个待调度运单归入一个主单；拆单=解除合并组，全部还原 normal；
 * ④ D-50 同线路合单三要素（线路/时效/目的网点）因订单表字段不全，
 *    降级为「同当前网点 + 同收件区县」校验（决策点，待 D-50 字段落地后升级）；
 * ⑤ 全程留痕 work_transport_split_merge_log。</p>
 */
@Slf4j
@Service
public class WaybillSplitMergeServiceImpl extends ServiceImpl<TransportSplitMergeLogMapper, TransportSplitMergeLog>
        implements IWaybillSplitMergeService {

    private static final String MERGE_STATUS_NORMAL = "normal";
    private static final String MERGE_STATUS_MAIN = "merged";
    private static final String MERGE_STATUS_CHILD = "split_child";

    private static final int OP_TYPE_MERGE = 1;
    private static final int OP_TYPE_SPLIT = 2;

    private static final String D50_REMARK =
            "D-50 降级校验：同当前网点+同收件区县（订单表线路/时效/目的网点字段不全，待字段落地后升级）";

    @Autowired
    @Lazy
    private com.xinfei.service.ITransportOrderService transportOrderService;

    @Autowired
    private OrderFeign orderFeign;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void merge(Long mainId, List<Long> childIds, String operatorName) {
        if (mainId == null) {
            throw new IllegalArgumentException("主运单ID不能为空");
        }
        if (childIds == null || childIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要合并的子运单");
        }
        List<Long> distinctChildIds = childIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctChildIds.contains(mainId)) {
            throw new IllegalArgumentException("主运单不能同时作为子运单");
        }

        // 全量读取（主单+子单），一次校验存在性/调度状态/合并状态
        List<Long> allIds = new ArrayList<>(distinctChildIds);
        allIds.add(mainId);
        Map<Long, TransportOrder> orderMap = transportOrderService.listByIds(allIds).stream()
                .collect(Collectors.toMap(TransportOrder::getId, Function.identity()));
        if (orderMap.size() < allIds.size()) {
            throw new IllegalArgumentException("存在无效运单（不存在或已删除）");
        }
        for (TransportOrder to : orderMap.values()) {
            if (!TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode().equals(to.getSchedulingStatus())) {
                throw new IllegalArgumentException("运单[" + to.getId() + "]已调度，仅待调度运单可合单");
            }
            if (!MERGE_STATUS_NORMAL.equals(to.getMergeStatus())) {
                throw new IllegalArgumentException("运单[" + to.getId() + "]已存在于其他合并组，不可重复合单");
            }
        }

        // D-50 降级校验：同当前网点 + 同收件区县（经订单 Feign）
        checkSameDestination(orderMap);

        // 主单置 merged，子单挂父单
        TransportOrder main = orderMap.get(mainId);
        TransportOrder mainUpdate = new TransportOrder();
        mainUpdate.setId(mainId);
        mainUpdate.setMergeStatus(MERGE_STATUS_MAIN);
        if (!transportOrderService.updateById(mainUpdate)) {
            throw new IllegalArgumentException("主运单更新失败");
        }
        for (Long childId : distinctChildIds) {
            TransportOrder childUpdate = new TransportOrder();
            childUpdate.setId(childId);
            childUpdate.setParentId(mainId);
            childUpdate.setMergeStatus(MERGE_STATUS_CHILD);
            if (!transportOrderService.updateById(childUpdate)) {
                throw new IllegalArgumentException("子运单[" + childId + "]更新失败");
            }
            logOp(OP_TYPE_MERGE, mainId, childId, null, operatorName);
        }
        log.info("[运单拆合] 合单完成: main={}, children={}, operator={}", mainId, distinctChildIds, operatorName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void split(Long mainId, String operatorName) {
        if (mainId == null) {
            throw new IllegalArgumentException("主运单ID不能为空");
        }
        TransportOrder main = transportOrderService.getById(mainId);
        if (main == null) {
            throw new IllegalArgumentException("运单不存在或已删除");
        }
        if (!MERGE_STATUS_MAIN.equals(main.getMergeStatus())) {
            throw new IllegalArgumentException("该运单不是合并组主单，不可拆单");
        }
        if (!TransportOrderSchedulingStatus.TO_BE_SCHEDULED.getCode().equals(main.getSchedulingStatus())) {
            throw new IllegalArgumentException("合并组已进入调度，不可拆单");
        }
        List<TransportOrder> children = transportOrderService.list(new LambdaQueryWrapper<TransportOrder>()
                .eq(TransportOrder::getParentId, mainId)
                .eq(TransportOrder::getMergeStatus, MERGE_STATUS_CHILD));

        // 主单还原（parent_id 置 null 须走 UpdateWrapper 显式 set，updateById 默认忽略 null 字段）
        transportOrderService.lambdaUpdate()
                .eq(TransportOrder::getId, mainId)
                .set(TransportOrder::getParentId, null)
                .set(TransportOrder::getMergeStatus, MERGE_STATUS_NORMAL)
                .update();
        // 子单还原
        for (TransportOrder child : children) {
            transportOrderService.lambdaUpdate()
                    .eq(TransportOrder::getId, child.getId())
                    .set(TransportOrder::getParentId, null)
                    .set(TransportOrder::getMergeStatus, MERGE_STATUS_NORMAL)
                    .update();
            logOp(OP_TYPE_SPLIT, mainId, child.getId(), child.getParentId(), operatorName);
        }
        log.info("[运单拆合] 拆单完成: main={}, children={}, operator={}", mainId,
                children.stream().map(TransportOrder::getId).collect(Collectors.toList()), operatorName);
    }

    /**
     * D-50 降级校验：组内订单「同当前网点 + 同收件区县」。
     * 订单缺失（Feign 降级/查无）视为校验不通过，阻断合单避免错并异路货件。
     */
    private void checkSameDestination(Map<Long, TransportOrder> orderMap) {
        List<String> orderIds = orderMap.values().stream()
                .map(TransportOrder::getOrderId)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
        if (orderIds.isEmpty()) {
            throw new IllegalArgumentException("运单缺少关联订单，无法执行合单校验");
        }
        List<OrderDTO> orders = orderFeign.findByIds(orderIds);
        if (orders == null || orders.size() < orderIds.size()) {
            throw new IllegalArgumentException("存在无法核对的关联订单，合单校验不通过");
        }
        Map<String, OrderDTO> dtoMap = orders.stream()
                .collect(Collectors.toMap(OrderDTO::getId, Function.identity(), (a, b) -> a));
        Long destOrgId = null;
        String destCountyId = null;
        for (TransportOrder to : orderMap.values()) {
            OrderDTO dto = to.getOrderId() == null ? null : dtoMap.get(String.valueOf(to.getOrderId()));
            if (dto == null) {
                throw new IllegalArgumentException("运单[" + to.getId() + "]的关联订单无法获取，合单校验不通过");
            }
            Long orgId = parseLong(dto.getCurrentAgencyId());
            String countyId = StringUtils.defaultString(dto.getReceiverCountyId());
            if (destOrgId == null && destCountyId == null) {
                destOrgId = orgId;
                destCountyId = countyId;
                continue;
            }
            if (!Objects.equals(destOrgId, orgId) || !Objects.equals(destCountyId, countyId)) {
                throw new IllegalArgumentException("合单校验不通过（D-50 降级口径）：组内运单目的网点或收件区县不一致");
            }
        }
    }

    private Long parseLong(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 拆合留痕（失败不阻断业务——留痕表写入异常仅告警，保持与主流程解耦）
     */
    private void logOp(int opType, Long mainId, Long memberOrderId, Long oldParentId, String operatorName) {
        try {
            TransportSplitMergeLog logEntity = new TransportSplitMergeLog();
            logEntity.setOpType(opType);
            logEntity.setMainOrderId(mainId);
            logEntity.setMemberOrderId(memberOrderId);
            logEntity.setOldParentId(oldParentId);
            logEntity.setRemark(D50_REMARK);
            logEntity.setOperatorName(StringUtils.defaultIfBlank(operatorName, "系统"));
            logEntity.setCreateTime(LocalDateTime.now());
            save(logEntity);
        } catch (Exception e) {
            log.error("[运单拆合] 留痕写入失败（不阻断业务）: main={}, member={}", mainId, memberOrderId, e);
        }
    }
}
