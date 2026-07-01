package com.itheima.pinda.mapper.state;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.state.StatusTransitionHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 状态流转历史Mapper接口
 *
 * @author Claude Code
 * @since 2026-07-01
 */
@Mapper
public interface StatusTransitionHistoryMapper extends BaseMapper<StatusTransitionHistory> {
}
