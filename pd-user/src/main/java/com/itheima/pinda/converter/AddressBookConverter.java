package com.itheima.pinda.converter;

import com.itheima.pinda.DTO.AddressBookDTO;
import com.itheima.pinda.entity.AddressBook;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * 地址簿 Entity ↔ DTO 转换器（MapStruct 编译期生成）。
 * 基础字段同名自动映射；province/city/county/fullAddress 四个区域展示字段
 * 由 pd-web-customer 聚合层用 AreaFeign 填充，pd-user 不负责。
 */
@Mapper(componentModel = "spring")
public interface AddressBookConverter {

    /** 实体转 DTO */
    AddressBookDTO toDto(AddressBook addressBook);

    /** 实体列表转 DTO 列表 */
    List<AddressBookDTO> toDtoList(List<AddressBook> addressBookList);

    /** DTO 转实体（写入前转换） */
    AddressBook toEntity(AddressBookDTO addressBookDTO);
}
