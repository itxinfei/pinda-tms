package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.PriceAgreement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户价格协议 Mapper（FIN-1.1 / POD-06 费率卡）
 */
@Mapper
public interface PriceAgreementMapper extends BaseMapper<PriceAgreement> {
}
