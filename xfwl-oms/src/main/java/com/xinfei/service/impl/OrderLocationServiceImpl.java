package com.xinfei.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.OrderLocation;
import com.xinfei.mapper.OrderLocationMapper;
import com.xinfei.service.IOrderLocationService;
import org.springframework.stereotype.Service;

/**
 * 位置信息服务实现
 */
@Service
public class OrderLocationServiceImpl extends ServiceImpl<OrderLocationMapper, OrderLocation> implements IOrderLocationService {

}