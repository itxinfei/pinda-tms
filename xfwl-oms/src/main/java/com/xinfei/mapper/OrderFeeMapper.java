package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.OrderFee;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单附加费明细 Mapper（POD-03）
 */
@Mapper
public interface OrderFeeMapper extends BaseMapper<OrderFee> {
}
