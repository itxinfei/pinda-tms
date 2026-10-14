package com.xinfei.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.SettlementOrder;
import com.xinfei.mapper.SettlementOrderMapper;
import com.xinfei.service.ISettlementOrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算单 Service 实现
 */
@Slf4j
@Service
public class SettlementOrderServiceImpl extends ServiceImpl<SettlementOrderMapper, SettlementOrder>
        implements ISettlementOrderService {
}
