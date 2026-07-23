package com.itheima.pinda.feign.hystrix;

import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.DriverJobFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 司机作业单Feign接口熔断降级
 */
@Slf4j
@Component
public class DriverJobFeignFallback implements DriverJobFeign {

    @Override
    public DriverJobDTO save(DriverJobDTO dto) {
        log.error("远程调用 pd-work 失败: save({}), 返回null", dto);
        return null;
    }

    @Override
    public DriverJobDTO updateById(String id, DriverJobDTO dto) {
        log.error("远程调用 pd-work 失败: updateById({}), 返回null", id);
        return null;
    }

    @Override
    public PageResponse<DriverJobDTO> findByPage(DriverJobDTO dto) {
        log.error("远程调用 pd-work 失败: findByPage({}), 返回空分页", dto);
        PageResponse<DriverJobDTO> result = new PageResponse<>();
        result.setCounts(0L);
        result.setPagesize(0);
        result.setPages(0L);
        result.setPage(0);
        result.setItems(Collections.emptyList());
        return result;
    }

    @Override
    public DriverJobDTO findById(String id) {
        log.error("远程调用 pd-work 失败: findById({}), 返回null", id);
        return null;
    }

    @Override
    public List<DriverJobDTO> findAll(DriverJobDTO dto) {
        log.error("远程调用 pd-work 失败: findAll({}), 返回空列表", dto);
        return Collections.emptyList();
    }
}
