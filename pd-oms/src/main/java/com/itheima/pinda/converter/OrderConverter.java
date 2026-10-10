package com.itheima.pinda.converter;

import com.itheima.pinda.DTO.OrderDTO;
import com.itheima.pinda.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * 订单 Entity ↔ DTO 转换器（MapStruct 编译期生成）。
 *
 * <p>实体主键/省市区为 Long/Integer，共享 DTO 为 String，MapStruct 内置 Number↔String
 * 自动转换；当前网点字段实体 {@code currentOrgId} 与 DTO {@code currentAgencyId} 改名，
 * 用 @Mapping 显式对应。tenantId 由多租户插件在写入时自动补齐，DTO 不承载。</p>
 */
@Mapper(componentModel = "spring")
public interface OrderConverter {

    /** 实体转 DTO */
    @Mapping(source = "currentOrgId", target = "currentAgencyId")
    OrderDTO toDto(Order order);

    /** DTO 转实体 */
    @Mapping(source = "currentAgencyId", target = "currentOrgId")
    Order toEntity(OrderDTO orderDTO);

    /** 实体列表转 DTO 列表 */
    List<OrderDTO> toDtoList(List<Order> orderList);
}
