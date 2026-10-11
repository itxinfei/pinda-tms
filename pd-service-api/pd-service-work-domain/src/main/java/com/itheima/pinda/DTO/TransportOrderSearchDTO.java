package com.itheima.pinda.DTO;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class TransportOrderSearchDTO extends TransportOrderDTO {


    /**
     * 订单ID 集合
     */
    private List<Long> orderIds;

}
