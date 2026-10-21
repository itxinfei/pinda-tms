package com.xinfei.service;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.OrderClassifyEntity;
import java.util.List;

/**
 * 订单分类
 */
public interface IOrderClassifyService extends IService<OrderClassifyEntity> {
    List<OrderClassifyEntity> findByJobLogId(Long id);
}
