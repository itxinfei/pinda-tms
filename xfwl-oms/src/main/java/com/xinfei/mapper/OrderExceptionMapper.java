package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.OrderException;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单异常记录 Mapper（ORD-04）
 */
@Mapper
public interface OrderExceptionMapper extends BaseMapper<OrderException> {
}
