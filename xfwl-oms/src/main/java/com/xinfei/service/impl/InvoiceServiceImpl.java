package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.common.CustomIdGenerator;
import com.xinfei.entity.FreightBill;
import com.xinfei.entity.Invoice;
import com.xinfei.mapper.InvoiceMapper;
import com.xinfei.service.IFreightBillService;
import com.xinfei.service.IInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 电子发票 Service 实现（P0-6c 开票模块，据《21》FIN-1.2）
 *
 * <p>金额与客户一律取自关联账单（禁篡改）；不接真实税控，invoice_no 测试环境自编 TEST-+ts。</p>
 */
@Slf4j
@Service
public class InvoiceServiceImpl extends ServiceImpl<InvoiceMapper, Invoice>
        implements IInvoiceService {

    @Autowired
    private IFreightBillService freightBillService;

    @Autowired
    private CustomIdGenerator idGenerator;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice apply(Long billId, Integer type, String taxNo, String mailAddress, Long operatorId) {
        FreightBill bill = freightBillService.getById(billId);
        if (bill == null) {
            log.warn("[发票] 申请失败：账单不存在 billId={}", billId);
            return null;
        }
        // 幂等：同一账单已存在未作废发票则拒绝
        Long exist = baseMapper.selectCount(new LambdaQueryWrapper<Invoice>()
                .eq(Invoice::getBillId, billId)
                .ne(Invoice::getStatus, Invoice.STATUS_VOIDED));
        if (exist != null && exist > 0) {
            log.warn("[发票] 同账单已存在未作废发票，幂等拒绝: billId={}", billId);
            return null;
        }
        Long id = idGenerator.nextId(new Invoice());
        LocalDateTime now = LocalDateTime.now();
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setBillId(billId);
        inv.setCustomerId(bill.getCustomerId());
        inv.setAmount(bill.getTotalAmount());
        inv.setType(type == null ? Invoice.TYPE_GENERAL : type);
        inv.setStatus(Invoice.STATUS_APPLIED);
        inv.setTaxNo(taxNo);
        inv.setMailAddress(mailAddress);
        inv.setCreateBy(operatorId);
        inv.setCreateTime(now);
        inv.setUpdateBy(operatorId);
        inv.setUpdateTime(now);
        baseMapper.insert(inv);
        log.info("[发票] 申请成功: id={}, billId={}, amount={}", id, billId, inv.getAmount());
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Invoice issue(Long id, Long operatorId) {
        Invoice inv = baseMapper.selectById(id);
        if (inv == null) {
            return null;
        }
        // 合法前置：1待申请 或 2已申请 → 3已开具
        if (inv.getStatus() == null
                || (inv.getStatus() != Invoice.STATUS_APPLIED && inv.getStatus() != Invoice.STATUS_REQUESTED)) {
            log.warn("[发票] 开具失败：状态非待申请/已申请 id={}, status={}", id, inv.getStatus());
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        Invoice upd = new Invoice();
        upd.setId(id);
        upd.setInvoiceNo("TEST-" + now.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")));
        upd.setStatus(Invoice.STATUS_ISSUED);
        upd.setInvoiceTime(now);
        upd.setUpdateBy(operatorId);
        upd.setUpdateTime(now);
        baseMapper.updateById(upd);
        inv.setInvoiceNo(upd.getInvoiceNo());
        inv.setStatus(Invoice.STATUS_ISSUED);
        inv.setInvoiceTime(now);
        log.info("[发票] 开具成功: id={}, invoiceNo={}", id, upd.getInvoiceNo());
        return inv;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean mail(Long id, Long operatorId) {
        Invoice inv = baseMapper.selectById(id);
        if (inv == null || inv.getStatus() == null || inv.getStatus() != Invoice.STATUS_ISSUED) {
            log.warn("[发票] 邮寄失败：发票不存在或状态非已开具 id={}", id);
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Invoice upd = new Invoice();
        upd.setId(id);
        upd.setStatus(Invoice.STATUS_MAILED);
        upd.setUpdateBy(operatorId);
        upd.setUpdateTime(now);
        baseMapper.updateById(upd);
        log.info("[发票] 邮寄成功: id={} (3→4)", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean voidInvoice(Long id, String reason, Long operatorId) {
        if (StringUtils.isBlank(reason)) {
            throw new IllegalArgumentException("作废原因必填");
        }
        Invoice inv = baseMapper.selectById(id);
        if (inv == null || inv.getStatus() == null || inv.getStatus() == Invoice.STATUS_VOIDED) {
            log.warn("[发票] 作废失败：发票不存在或已作废 id={}", id);
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        Invoice upd = new Invoice();
        upd.setId(id);
        upd.setStatus(Invoice.STATUS_VOIDED);
        upd.setVoidReason(reason.trim());
        upd.setUpdateBy(operatorId);
        upd.setUpdateTime(now);
        baseMapper.updateById(upd);
        log.info("[发票] 作废成功: id={} →5, reason={}", id, reason);
        return true;
    }

    @Override
    public Page<Invoice> page(Integer page, Integer pageSize, String customerId, Integer status) {
        Page<Invoice> p = new Page<>(page == null || page < 1 ? 1 : page,
                pageSize == null || pageSize < 1 ? 10 : pageSize);
        LambdaQueryWrapper<Invoice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.isNotBlank(customerId), Invoice::getCustomerId, customerId)
                .eq(status != null, Invoice::getStatus, status)
                .orderByDesc(Invoice::getCreateTime);
        return baseMapper.selectPage(p, wrapper);
    }
}
