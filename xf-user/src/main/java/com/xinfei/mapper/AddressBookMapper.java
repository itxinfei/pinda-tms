package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.AddressBook;
import org.apache.ibatis.annotations.Mapper;

/**
 * 地址簿Mapper接口
 */
@Mapper
public interface AddressBookMapper extends BaseMapper<AddressBook> {
}