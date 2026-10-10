package com.itheima.pinda.converter;

import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.entity.OrderCargo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 货物 Entity ↔ DTO 转换器（MapStruct 编译期生成实现，替代 BeanUtils.copyProperties）。
 *
 * <p>运单字段实体 {@code transportOrderId} 与 DTO {@code tranOrderId} 改名，用 @Mapping
 * 显式对应；Long↔String 由 MapStruct 内置转换。任一侧字段改名/类型不符会在编译期报错，
 * 避免反射拷贝在运行期静默漏字段。</p>
 */
@Mapper(componentModel = "spring")
public interface OrderCargoConverter {

    /** 实体转 DTO */
    @Mapping(source = "transportOrderId", target = "tranOrderId")
    OrderCargoDto toDto(OrderCargo orderCargo);

    /** DTO 转实体 */
    @Mapping(source = "tranOrderId", target = "transportOrderId")
    OrderCargo toEntity(OrderCargoDto orderCargoDto);

    /** 实体列表转 DTO 列表 */
    List<OrderCargoDto> toDtoList(List<OrderCargo> orderCargoList);
}
