package com.itheima.pinda.feign.transportline.hystrix;

import com.itheima.pinda.DTO.transportline.TransportLineDto;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.transportline.TransportLineFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 线路Feign接口熔断降级
 */
@Slf4j
@Component
public class TransportLineFeignFallback implements TransportLineFeign {

    @Override
    public TransportLineDto saveTransportLine(TransportLineDto dto) {
        log.warn("远程调用 pd-base 失败: saveTransportLine({}), 返回null", dto);
        return null;
    }

    @Override
    public TransportLineDto fineById(String id) {
        log.warn("远程调用 pd-base 失败: fineById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<TransportLineDto> findByPage(Integer page, Integer pageSize, String lineNumber, String name, String transportLineTypeId) {
        log.warn("远程调用 pd-base 失败: findByPage(page={}, pageSize={}, lineNumber={}, name={}, transportLineTypeId={}), 返回空分页", page, pageSize, lineNumber, name, transportLineTypeId);
        PageResponse<TransportLineDto> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(pageSize);
        result.setPages(0L);
        result.setPage(page);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public List<TransportLineDto> findAll(List<String> ids, String agencyId, List<String> agencyIds) {
        log.warn("远程调用 pd-base 失败: findAll(ids={}, agencyId={}, agencyIds={}), 返回空列表", ids, agencyId, agencyIds);
        return Collections.emptyList();
    }

    @Override
    public TransportLineDto update(String id, TransportLineDto dto) {
        log.warn("远程调用 pd-base 失败: update({}), 返回null", id);
        return null;
    }

    @Override
    public Result disable(String id) {
        log.warn("远程调用 pd-base 失败: disable({}), 返回失败结果", id);
        return Result.error("服务降级，disable执行失败");
    }

    @Override
    public List<TransportLineDto> list(TransportLineDto transportLineDto) {
        log.warn("远程调用 pd-base 失败: list({}), 返回空列表", transportLineDto);
        return Collections.emptyList();
    }
}
