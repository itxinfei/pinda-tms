package com.xinfei.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.xinfei.entity.FreightDetail;
import com.xinfei.mapper.FreightDetailMapper;
import com.xinfei.service.IFreightDetailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 运费明细 Service 实现
 */
@Slf4j
@Service
public class FreightDetailServiceImpl extends ServiceImpl<FreightDetailMapper, FreightDetail>
        implements IFreightDetailService {
}
