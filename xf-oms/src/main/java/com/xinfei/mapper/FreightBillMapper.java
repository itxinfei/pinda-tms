package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.FreightBill;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户运费账单 Mapper（P0-6b 应收对账闭环）
 */
@Mapper
public interface FreightBillMapper extends BaseMapper<FreightBill> {
}
