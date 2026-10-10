package com.itheima.pinda.controller;


import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-03-24
 */
@Slf4j
@Tag(name = "物品类型")
@Controller
@RequestMapping("goodsType")
public class GoodsTypeController {

    private final GoodsTypeFeign goodsTypeFeign;

    public GoodsTypeController(GoodsTypeFeign goodsTypeFeign) {
        this.goodsTypeFeign = goodsTypeFeign;
    }

    @SneakyThrows
    @Operation(summary = "全部类型")
    @ResponseBody
    @GetMapping("all")
    public Result all() {

        List<GoodsTypeDto> goodsType = goodsTypeFeign.findAll();

        return Result.ok().put("data", goodsType);
    }
}
