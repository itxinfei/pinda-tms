package com.xinfei.feign.fallback;

import com.xinfei.DTO.SysConfigDTO;
import com.xinfei.common.utils.PageResponse;
import com.xinfei.common.utils.Result;
import com.xinfei.feign.ConfigFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 参数配置 Feign 熔断降级：读返回空，写返回错误提示。
 */
@Slf4j
@Component
public class ConfigFeignFallback implements ConfigFeign {

    private static final String UNAVAILABLE = "参数配置服务暂不可用，请稍后再试";

    @Override
    public PageResponse<SysConfigDTO> page(Integer page, Integer pageSize, String keywords, Integer status) {
        log.warn("远程调用 pd-auth 失败: ConfigFeign.page, 返回空分页");
        return PageResponse.<SysConfigDTO>builder()
                .counts(0L)
                .pagesize(pageSize)
                .pages(0L)
                .page(page)
                .items(Collections.<SysConfigDTO>emptyList())
                .build();
    }

    @Override
    public SysConfigDTO detail(Long id) {
        log.warn("远程调用 pd-auth 失败: ConfigFeign.detail({}), 返回 null", id);
        return null;
    }

    @Override
    public Result save(SysConfigDTO dto) {
        log.warn("远程调用 pd-auth 失败: ConfigFeign.save");
        return Result.error(UNAVAILABLE);
    }

    @Override
    public Result update(Long id, SysConfigDTO dto) {
        log.warn("远程调用 pd-auth 失败: ConfigFeign.update({})", id);
        return Result.error(UNAVAILABLE);
    }

    @Override
    public Result remove(Long id) {
        log.warn("远程调用 pd-auth 失败: ConfigFeign.remove({})", id);
        return Result.error(UNAVAILABLE);
    }
}
