package com.xinfei.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.FreightBill;
import com.xinfei.entity.SettlementOrder;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 客户运费账单 Service（P0-6b 应收对账闭环）
 *
 * <p>把已通的应收半环（签收→oms_settlement_order）拼成对账闭环：
 * generate 归集 → 对账视图 → confirm → write-off。详见《21》§1.5。</p>
 */
public interface IFreightBillService extends IService<FreightBill> {

    /**
     * 生成账单：归集该客户该账期内 应收(status∈{待对账,已对账}) 且未归账(bill_id 为空) 的结算单，
     * 落账单 + 回写结算单 bill_id。
     *
     * <p>幂等：同客户同账期已有未核销账单则拒绝（返回 null，由调用方提示单号），不重复归集。</p>
     *
     * @param customerId  客户id（settle_object_id）
     * @param periodStart 账期开始
     * @param periodEnd   账期结束
     * @param operatorId  操作人id（取自登录态）
     * @return 新建账单；若已存在未核销账单则返回 null
     */
    FreightBill generate(String customerId, LocalDate periodStart, LocalDate periodEnd, Long operatorId);

    /**
     * 对账视图：账单头 + 归集的结算单列表 + 差异标记（账单 total_amount vs Σ结算单 receivable_amount）。
     *
     * @param billId 账单id
     * @return { bill, settlements, sumReceivable, diff } ；账单不存在返回 null
     */
    Map<String, Object> reconcile(Long billId);

    /**
     * 确认账单：账单 0→1，联动其下结算单 0→1。
     *
     * @param billId      账单id
     * @param operatorId  操作人id
     * @return 是否成功（含状态合法性校验）
     */
    boolean confirm(Long billId, Long operatorId);

    /**
     * 核销账单：账单 1→2，联动其下结算单 1→2。
     *
     * @param billId      账单id
     * @param operatorId  操作人id
     * @return 是否成功（含状态合法性校验）
     */
    boolean writeOff(Long billId, Long operatorId);

    /**
     * 查询账单关联的结算单列表。
     */
    List<SettlementOrder> listSettlements(Long billId);

    /**
     * 账单分页列表（管理端列表页，2026-10-19 前端收口补齐）。
     *
     * @param page       页码（1 起）
     * @param pageSize   页大小
     * @param billNo     账单号（模糊，可空）
     * @param customerId 客户id（精确，可空）
     * @param status     状态（可空）
     * @return 分页结果，createTime 倒序
     */
    IPage<FreightBill> page(Integer page, Integer pageSize, String billNo, String customerId, Integer status);
}
