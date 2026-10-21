package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.Invoice;

/**
 * 电子发票 Service（P0-6c 开票模块）
 *
 * <p>申请(apply)→开具(issue，自编发票号)→邮寄(mail)→作废(void)；不接真实税控。</p>
 */
public interface IInvoiceService extends IService<Invoice> {

    /**
     * 按账单申请开票（status=1 待申请）。金额与客户取自账单（禁篡改）。
     *
     * <p>幂等：同一账单已存在未作废发票则拒绝（返回 null）。</p>
     *
     * @param billId      账单id
     * @param type        票种 1普票 2专票
     * @param taxNo       客户税号
     * @param mailAddress 邮寄地址
     * @param operatorId  操作人id
     * @return 新建发票；账单不存在或同账单已有未作废发票返回 null
     */
    Invoice apply(Long billId, Integer type, String taxNo, String mailAddress, Long operatorId);

    /**
     * 开具（1/2→3）：自编发票号 TEST-+时间戳，置 invoice_time。
     *
     * @return 更新后的发票；状态不合法返回 null
     */
    Invoice issue(Long id, Long operatorId);

    /**
     * 邮寄（3→4）。
     *
     * @return 是否成功（含状态合法性校验）
     */
    boolean mail(Long id, Long operatorId);

    /**
     * 作废（→5）：作废原因必填。
     *
     * @param reason 作废原因（必填）
     * @return 是否成功
     */
    boolean voidInvoice(Long id, String reason, Long operatorId);

    /**
     * 分页查询（按客户/状态过滤）。
     */
    Page<Invoice> page(Integer page, Integer pageSize, String customerId, Integer status);
}
