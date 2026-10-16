package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.Invoice;
import org.apache.ibatis.annotations.Mapper;

/**
 * 电子发票 Mapper（P0-6c 开票模块）
 */
@Mapper
public interface InvoiceMapper extends BaseMapper<Invoice> {
}
