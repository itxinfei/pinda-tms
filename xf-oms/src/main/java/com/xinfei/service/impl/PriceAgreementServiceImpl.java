package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xinfei.entity.PriceAgreement;
import com.xinfei.mapper.PriceAgreementMapper;
import com.xinfei.service.IPriceAgreementService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户价格协议服务实现（FIN-1.1 / POD-06 费率卡）
 */
@Slf4j
@Service
public class PriceAgreementServiceImpl extends ServiceImpl<PriceAgreementMapper, PriceAgreement>
        implements IPriceAgreementService {

    @Override
    public PriceAgreement saveAgreement(PriceAgreement agreement, Long operator) {
        if (StringUtils.isBlank(agreement.getMemberId())) {
            throw new IllegalArgumentException("客户id不能为空（散客 GUEST 不适用协议价）");
        }
        if (agreement.getPrice() == null || agreement.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单价必须大于 0");
        }
        if (agreement.getPriceType() == null) {
            agreement.setPriceType(PriceAgreement.PRICE_TYPE_PER_WAYBILL);
        }
        if (agreement.getDiscount() == null) {
            agreement.setDiscount(BigDecimal.ONE);
        }
        if (agreement.getDiscount().compareTo(BigDecimal.ZERO) <= 0
                || agreement.getDiscount().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("折扣须在 (0,1] 区间（0.90=9折，1.00=无折扣）");
        }
        if (agreement.getEffectiveFrom() == null) {
            throw new IllegalArgumentException("生效日期不能为空");
        }
        if (agreement.getEffectiveTo() != null && agreement.getEffectiveTo().isBefore(agreement.getEffectiveFrom())) {
            throw new IllegalArgumentException("失效日期不能早于生效日期");
        }
        if (agreement.getWeightMin() != null && agreement.getWeightMax() != null
                && agreement.getWeightMax().compareTo(agreement.getWeightMin()) < 0) {
            throw new IllegalArgumentException("重量段上限不能小于下限");
        }
        LocalDateTime now = LocalDateTime.now();
        if (agreement.getId() == null) {
            agreement.setStatus(PriceAgreement.STATUS_ENABLED);
            agreement.setCreateBy(operator);
            agreement.setCreateTime(now);
        }
        agreement.setUpdateBy(operator);
        agreement.setUpdateTime(now);
        saveOrUpdate(agreement);
        log.info("[价格协议] 落库 id={} memberId={} 价格={}×折扣{} 有效期 {}~{} 操作人={}",
                agreement.getId(), agreement.getMemberId(), agreement.getPrice(), agreement.getDiscount(),
                agreement.getEffectiveFrom(), agreement.getEffectiveTo(), operator);
        return agreement;
    }

    @Override
    public boolean disable(Long id, Long operator) {
        PriceAgreement db = getById(id);
        if (db == null || db.getStatus() != null && db.getStatus() == PriceAgreement.STATUS_DISABLED) {
            return false;
        }
        db.setStatus(PriceAgreement.STATUS_DISABLED);
        db.setUpdateBy(operator);
        db.setUpdateTime(LocalDateTime.now());
        updateById(db);
        return true;
    }

    @Override
    public Page<PriceAgreement> page(Integer page, Integer pageSize, String memberId, Integer status) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 500);
        LambdaQueryWrapper<PriceAgreement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(memberId)) {
            wrapper.eq(PriceAgreement::getMemberId, memberId);
        }
        if (status != null) {
            wrapper.eq(PriceAgreement::getStatus, status);
        }
        wrapper.orderByDesc(PriceAgreement::getId);
        return this.page(new Page<>(p, ps), wrapper);
    }

    @Override
    public PriceAgreement matchAgreement(Long memberId, BigDecimal totalWeight, LocalDate businessDate) {
        if (memberId == null) {
            return null;
        }
        LocalDate date = businessDate != null ? businessDate : LocalDate.now();
        LambdaQueryWrapper<PriceAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PriceAgreement::getMemberId, String.valueOf(memberId))
                .eq(PriceAgreement::getStatus, PriceAgreement.STATUS_ENABLED)
                // 第一期口径：line/goodsType 限空=不限（订单 DTO 暂无线路/货物类型维度）
                .isNull(PriceAgreement::getLineId)
                .isNull(PriceAgreement::getGoodsTypeId)
                .le(PriceAgreement::getEffectiveFrom, date)
                .and(w -> w.isNull(PriceAgreement::getEffectiveTo).or().ge(PriceAgreement::getEffectiveTo, date));
        if (totalWeight != null) {
            wrapper.le(PriceAgreement::getWeightMin, totalWeight)
                    .and(w -> w.isNull(PriceAgreement::getWeightMax).or().ge(PriceAgreement::getWeightMax, totalWeight));
        } else {
            wrapper.isNull(PriceAgreement::getWeightMin);
        }
        wrapper.orderByDesc(PriceAgreement::getWeightMin).orderByDesc(PriceAgreement::getId);
        List<PriceAgreement> hit = this.list(wrapper.last("LIMIT 1"));
        if (hit.isEmpty()) {
            return null;
        }
        log.info("[价格协议] 计费命中 agreementId={} memberId={} 业务日期={}",
                hit.get(0).getId(), memberId, date);
        return hit.get(0);
    }

    @Override
    public BigDecimal calcAgreementAmount(PriceAgreement agreement, BigDecimal totalWeight) {
        BigDecimal amount;
        if (agreement.getPriceType() != null && agreement.getPriceType() == PriceAgreement.PRICE_TYPE_PER_TON) {
            BigDecimal weight = totalWeight != null ? totalWeight : BigDecimal.ZERO;
            amount = agreement.getPrice().multiply(weight);
        } else {
            // 按票/按方（预留）：一口价
            amount = agreement.getPrice();
        }
        amount = amount.multiply(agreement.getDiscount()).setScale(2, RoundingMode.HALF_UP);
        log.info("[价格协议] 协议价计算 agreementId={} priceType={} price={} discount={} totalWeight={} → {}",
                agreement.getId(), agreement.getPriceType(), agreement.getPrice(),
                agreement.getDiscount(), totalWeight, amount);
        return amount;
    }
}
