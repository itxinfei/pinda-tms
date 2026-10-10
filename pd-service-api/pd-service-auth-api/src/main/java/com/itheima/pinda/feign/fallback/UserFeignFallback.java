package com.itheima.pinda.feign.fallback;

import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.feign.UserFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户 Feign 熔断降级
 */
@Slf4j
@Component
public class UserFeignFallback implements UserFeign {

    @Override
    public UserDTO get(Long id) {
        log.warn("远程调用 pd-auth 失败: UserFeign.get({}), 返回null", id);
        return null;
    }

    @Override
    public List<UserDTO> list(List<Long> ids, Long stationId, String name, Long orgId) {
        log.warn("远程调用 pd-auth 失败: UserFeign.list, 返回空列表");
        return Collections.emptyList();
    }

    @Override
    public PageResponse<UserDTO> page(long page, long size, Long orgId, Long stationId,
                                      String name, String account, String mobile) {
        log.warn("远程调用 pd-auth 失败: UserFeign.page, 返回空分页");
        return PageResponse.<UserDTO>builder()
                .counts(0L)
                .pagesize(0)
                .pages(0L)
                .page(0)
                .items(Collections.emptyList())
                .build();
    }
}
