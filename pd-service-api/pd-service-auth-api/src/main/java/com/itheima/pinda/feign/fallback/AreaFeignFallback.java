package com.itheima.pinda.feign.fallback;

import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.feign.AreaFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 行政区划 Feign 熔断降级
 */
@Slf4j
@Component
public class AreaFeignFallback implements AreaFeign {

    @Override
    public AreaDTO get(Long id) {
        log.warn("远程调用 pd-auth 失败: AreaFeign.get({}), 返回null", id);
        return null;
    }

    @Override
    public List<AreaDTO> findAll(Long parentId, List<Long> ids) {
        log.warn("远程调用 pd-auth 失败: AreaFeign.findAll, 返回空列表");
        return Collections.emptyList();
    }

    @Override
    public AreaDTO getByCode(String code) {
        log.warn("远程调用 pd-auth 失败: AreaFeign.getByCode({}), 返回null", code);
        return null;
    }
}
