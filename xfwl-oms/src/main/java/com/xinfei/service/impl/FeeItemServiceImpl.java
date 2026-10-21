package com.xinfei.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.FeeItem;
import com.xinfei.mapper.FeeItemMapper;
import com.xinfei.service.IFeeItemService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 附加费费用项配置服务实现（POD-03 第一期 / FA-10）
 */
@Slf4j
@Service
public class FeeItemServiceImpl extends ServiceImpl<FeeItemMapper, FeeItem> implements IFeeItemService {

    @Override
    public FeeItem saveItem(FeeItem item, Long operator) {
        if (StringUtils.isBlank(item.getFeeCode())) {
            throw new IllegalArgumentException("费用项编码不能为空");
        }
        if (StringUtils.isBlank(item.getFeeName())) {
            throw new IllegalArgumentException("费用项名称不能为空");
        }
        if (item.getCalcType() == null) {
            item.setCalcType(FeeItem.CALC_TYPE_FIXED);
        }
        if (item.getCalcType() == FeeItem.CALC_TYPE_FIXED) {
            if (item.getFixedAmount() == null || item.getFixedAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("固定金额计费须填不小于 0 的 fixed_amount");
            }
            item.setRatio(null);
        } else if (item.getCalcType() == FeeItem.CALC_TYPE_RATIO) {
            if (item.getRatio() == null || item.getRatio().compareTo(BigDecimal.ZERO) <= 0
                    || item.getRatio().compareTo(BigDecimal.ONE) > 0) {
                throw new IllegalArgumentException("按运费比例计费须填 (0,1] 区间的 ratio（0.0500=5%）");
            }
            item.setFixedAmount(null);
        } else {
            throw new IllegalArgumentException("计费方式非法（仅支持 1固定 2按运费比例）");
        }
        LocalDateTime now = LocalDateTime.now();
        if (item.getId() == null) {
            // fee_code 全租户唯一（uk_tenant_fee_code），重复即拒绝
            long dup = count(new LambdaQueryWrapper<FeeItem>().eq(FeeItem::getFeeCode, item.getFeeCode()));
            if (dup > 0) {
                throw new IllegalArgumentException("费用项编码已存在：" + item.getFeeCode());
            }
            item.setStatus(FeeItem.STATUS_ENABLED);
            item.setCreateBy(operator);
            item.setCreateTime(now);
        }
        item.setUpdateBy(operator);
        item.setUpdateTime(now);
        saveOrUpdate(item);
        log.info("[附加费配置] 落库 id={} code={} calcType={} 操作人={}",
                item.getId(), item.getFeeCode(), item.getCalcType(), operator);
        return item;
    }

    @Override
    public boolean disable(Long id, Long operator) {
        FeeItem db = getById(id);
        if (db == null || db.getStatus() != null && db.getStatus() == FeeItem.STATUS_DISABLED) {
            return false;
        }
        db.setStatus(FeeItem.STATUS_DISABLED);
        db.setUpdateBy(operator);
        db.setUpdateTime(LocalDateTime.now());
        updateById(db);
        return true;
    }

    @Override
    public Page<FeeItem> page(Integer page, Integer pageSize, String feeCode, Integer status) {
        int p = page == null || page < 1 ? 1 : page;
        int ps = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 500);
        LambdaQueryWrapper<FeeItem> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(feeCode)) {
            wrapper.eq(FeeItem::getFeeCode, feeCode);
        }
        if (status != null) {
            wrapper.eq(FeeItem::getStatus, status);
        }
        wrapper.orderByDesc(FeeItem::getId);
        return this.page(new Page<>(p, ps), wrapper);
    }
}
