package com.xinfei.converter;

import com.xinfei.DTO.AddressBookDTO;
import com.xinfei.controller.support.IdConverter;
import com.xinfei.entity.AddressBook;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 地址簿 Entity ↔ DTO 转换器（MapStruct 编译期生成）。
 *
 * <p>对外 DTO 字段名与类型保持不变（id/userId 为 String）；memberId ↔ userId、
 * contactName ↔ name 为改名映射；行政区划 Integer 实体 ↔ Long DTO。
 * province/city/county/fullAddress 四个展示字段由 pd-web-customer 聚合层用 AreaFeign 填充。</p>
 */
@Mapper(componentModel = "spring")
public interface AddressBookConverter {

    /** 实体转 DTO（province/city/county/fullAddress 由 pd-web-customer 聚合层用 AreaFeign 填充） */
    @Mapping(source = "memberId", target = "userId")
    @Mapping(source = "contactName", target = "name")
    @Mapping(target = "province", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "county", ignore = true)
    @Mapping(target = "fullAddress", ignore = true)
    AddressBookDTO toDto(AddressBook addressBook);

    List<AddressBookDTO> toDtoList(List<AddressBook> addressBookList);

    /** DTO 转实体（经纬度由地图服务写入，审计字段由 MyBatis-Plus 自动填充，DTO 不承载） */
    @Mapping(source = "userId", target = "memberId")
    @Mapping(source = "name", target = "contactName")
    @Mapping(target = "longitude", ignore = true)
    @Mapping(target = "latitude", ignore = true)
    @Mapping(target = "createBy", ignore = true)
    @Mapping(target = "updateBy", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    AddressBook toEntity(AddressBookDTO addressBookDTO);

    /** 雪花 ID：Long → String */
    default String mapId(Long id) {
        return IdConverter.toStr(id);
    }

    /** 雪花 ID：String → Long（非法值返回 null） */
    default Long mapId(String id) {
        return IdConverter.toLong(id);
    }

    /** 行政区划码：Integer 实体 → Long DTO */
    default Long mapArea(Integer areaId) {
        return areaId == null ? null : areaId.longValue();
    }

    /** 行政区划码：Long DTO → Integer 实体（超出 int 范围返回 null，区划码实际不会触发） */
    default Integer mapArea(Long areaId) {
        if (areaId == null) {
            return null;
        }
        return areaId > Integer.MAX_VALUE ? null : areaId.intValue();
    }
}
