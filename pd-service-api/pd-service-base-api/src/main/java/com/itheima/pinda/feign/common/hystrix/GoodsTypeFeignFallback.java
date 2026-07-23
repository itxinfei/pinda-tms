package com.itheima.pinda.feign.common.hystrix;

import com.itheima.pinda.DTO.base.GoodsTypeDto;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.common.GoodsTypeFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 货物类型Feign接口熔断降级
 */
@Slf4j
@Component
public class GoodsTypeFeignFallback implements GoodsTypeFeign {

    @Override
    public GoodsTypeDto saveGoodsType(GoodsTypeDto dto) {
        log.error("远程调用 pd-base 失败: saveGoodsType({}), 返回null", dto);
        return null;
    }

    @Override
    public GoodsTypeDto fineById(String id) {
        log.error("远程调用 pd-base 失败: fineById({}), 返回null", id);
        return null;
    }

    @Override
    public List<GoodsTypeDto> findAll(List<String> ids) {
        log.error("远程调用 pd-base 失败: findAll({}), 返回空列表", ids);
        return Collections.emptyList();
    }

    @Override
    public List<GoodsTypeDto> findAll() {
        log.error("远程调用 pd-base 失败: findAll(), 返回空列表");
        return Collections.emptyList();
    }

    @Override
    public PageResponse<GoodsTypeDto> findByPage(Integer page, Integer pageSize, String name, String truckTypeId, String truckTypeName) {
        log.error("远程调用 pd-base 失败: findByPage(page={}, pageSize={}, name={}, truckTypeId={}, truckTypeName={}), 返回空分页", page, pageSize, name, truckTypeId, truckTypeName);
        PageResponse<GoodsTypeDto> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(pageSize);
        result.setPages(0L);
        result.setPage(page);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public GoodsTypeDto update(String id, GoodsTypeDto dto) {
        log.error("远程调用 pd-base 失败: update({}), 返回null", id);
        return null;
    }

    @Override
    public Result disable(String id) {
        log.error("远程调用 pd-base 失败: disable({}), 返回失败结果", id);
        return Result.error("服务降级，disable执行失败");
    }
}
