package com.itheima.pinda.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.itheima.pinda.entity.FreightDetail;
import com.itheima.pinda.mapper.FreightDetailMapper;
import com.itheima.pinda.service.IFreightDetailService;
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
