package com.itheima.auth.service.internal;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.mapper.AuthUserMapper;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户内部查询：供 /internal/user 端点调用，返回裸 DTO。
 */
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final AuthUserMapper userMapper;

    /**
     * 按ID查询
     */
    public UserDTO get(Long id) {
        AuthUser user = userMapper.selectById(id);
        return user == null ? null : toDTO(user);
    }

    /**
     * 多条件查询
     */
    public List<UserDTO> list(List<Long> ids, Long stationId, String name, Long orgId) {
        List<AuthUser> users = userMapper.selectList(Wrappers.<AuthUser>lambdaQuery()
                .in(ids != null && !ids.isEmpty(), AuthUser::getId, ids)
                .eq(stationId != null, AuthUser::getStationId, stationId)
                .like(StringUtils.hasText(name), AuthUser::getName, name)
                .eq(orgId != null, AuthUser::getOrgId, orgId)
                .orderByAsc(AuthUser::getId));
        return users.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 分页查询
     */
    public PageResponse<UserDTO> page(long page, long size, Long orgId, Long stationId,
                                      String name, String account, String mobile) {
        IPage<AuthUser> result = userMapper.selectPage(new Page<>(page, size),
                Wrappers.<AuthUser>lambdaQuery()
                        .eq(orgId != null, AuthUser::getOrgId, orgId)
                        .eq(stationId != null, AuthUser::getStationId, stationId)
                        .like(StringUtils.hasText(name), AuthUser::getName, name)
                        .like(StringUtils.hasText(account), AuthUser::getAccount, account)
                        .like(StringUtils.hasText(mobile), AuthUser::getMobile, mobile));
        List<UserDTO> items = result.getRecords().stream()
                .map(this::toDTO).collect(Collectors.toList());
        return PageResponse.<UserDTO>builder()
                .counts(result.getTotal())
                .page((int) result.getCurrent())
                .pagesize((int) result.getSize())
                .pages(result.getPages())
                .items(items)
                .build();
    }

    private UserDTO toDTO(AuthUser user) {
        UserDTO dto = new UserDTO();
        BeanUtils.copyProperties(user, dto);
        // status 实体为 Integer(1/0)、DTO 为 Boolean，类型不同 copyProperties 会跳过，手工转换
        dto.setStatus(user.getStatus() != null && user.getStatus() == 1);
        return dto;
    }
}
