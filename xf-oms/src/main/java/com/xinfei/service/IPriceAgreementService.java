package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xinfei.entity.PriceAgreement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 客户价格协议服务（FIN-1.1 / POD-06 客户侧费率卡）
 */
public interface IPriceAgreementService extends IService<PriceAgreement> {

    /**
     * 新增/修改协议（id 空=新增；status 默认生效）
     *
     * @return 落库后的协议；参数非法抛 IllegalArgumentException
     */
    PriceAgreement saveAgreement(PriceAgreement agreement, Long operator);

    /** 停用协议（status→0）；返回 false=不存在或已停用 */
    boolean disable(Long id, Long operator);

    /** 分页查询（memberId/status 过滤） */
    Page<PriceAgreement> page(Integer page, Integer pageSize, String memberId, Integer status);

    /**
     * 计费衔接：按 客户+重量段+业务日期 匹配生效协议（status=1，line/goodsType 限空=不限的第一期口径）。
     * 多条命中时取 weight_min 更精确（更大）者，再取 id 最新。
     *
     * @param memberId     客户id（对齐 order.member_id）
     * @param totalWeight  货物总重 kg（可空=按票/不限段）
     * @param businessDate 业务日期（取"业务日期命中版本"而非最新版本）
     * @return 命中的协议；未命中返回 null
     */
    PriceAgreement matchAgreement(Long memberId, BigDecimal totalWeight, LocalDate businessDate);

    /**
     * 协议价计算：按票 = price×discount；按吨 = price×totalWeight×discount；按方暂同按票。
     * 结果保留 2 位小数（HALF_UP）。
     */
    BigDecimal calcAgreementAmount(PriceAgreement agreement, BigDecimal totalWeight);
}
