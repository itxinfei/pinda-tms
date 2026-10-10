package com.itheima.pinda.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.pinda.DTO.OrderCargoDto;
import com.itheima.pinda.common.utils.IdConverter;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.converter.OrderCargoConverter;
import com.itheima.pinda.entity.OrderCargo;
import com.itheima.pinda.service.IOrderCargoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 货物
 */
@RestController
@RequestMapping("cargo")
public class CargoController {
    @Autowired
    private IOrderCargoService orderCargoService;
    @Autowired
    private OrderCargoConverter orderCargoConverter;

    /**
     * 获取货物列表
     *
     * @param tranOrderId 运单id
     * @param orderId     订单id
     * @return 货物列表
     */
    @GetMapping("")
    public List<OrderCargoDto> findAll(@RequestParam(name = "tranOrderId", required = false) String tranOrderId, @RequestParam(name = "orderId", required = false) String orderId) {
        return orderCargoConverter.toDtoList(orderCargoService.findAll(tranOrderId, orderId));
    }

    @GetMapping("/list")
    public List<OrderCargoDto> list(@RequestParam(name = "orderIds", required = false) List<String> orderIds) {
        LambdaQueryWrapper<OrderCargo> wrapper = new LambdaQueryWrapper<>();
        List<Long> cargoOrderIds = IdConverter.toLongList(orderIds);
        wrapper.in(!CollectionUtils.isEmpty(cargoOrderIds), OrderCargo::getOrderId, cargoOrderIds);

        return orderCargoConverter.toDtoList(orderCargoService.list(wrapper));
    }

    /**
     * 添加货物
     *
     * @param dto 货物信息
     * @return 货物信息
     */
    @PostMapping("")
    public OrderCargoDto save(@RequestBody OrderCargoDto dto) {
        OrderCargo orderCargo = orderCargoConverter.toEntity(dto);
        orderCargo = orderCargoService.saveSelective(orderCargo);
        return orderCargoConverter.toDto(orderCargo);
    }

    /**
     * 更新货物信息
     *
     * @param id  货物id
     * @param dto 货物信息
     * @return 货物信息
     */
    @PutMapping("/{id}")
    public OrderCargoDto update(@PathVariable(name = "id") String id, @RequestBody OrderCargoDto dto) {
        dto.setId(id);
        OrderCargo orderCargo = orderCargoConverter.toEntity(dto);
        orderCargoService.updateById(orderCargo);
        return dto;
    }

    /**
     * 删除货物信息
     *
     * @param id 货物id
     * @return 返回信息
     */
    @DeleteMapping("/{id}")
    public Result del(@PathVariable(name = "id") String id) {
        orderCargoService.removeById(IdConverter.toLong(id));
        return Result.ok();
    }


    /**
     * 根据id获取货物详情
     *
     * @param id 货物id
     * @return 货物详情
     */
    @GetMapping("/{id}")
    public OrderCargoDto findById(@PathVariable(name = "id") String id) {
        Long cargoId = IdConverter.toLong(id);
        if (cargoId == null) {
            return null;
        }
        OrderCargo orderCargo = orderCargoService.getById(cargoId);
        if (orderCargo == null) {
            return null;
        }
        return orderCargoConverter.toDto(orderCargo);
    }

}
