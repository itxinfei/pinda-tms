package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.DecisionLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 合规决策留痕 Mapper（P0-9/FR-75）
 */
@Mapper
public interface DecisionLogMapper extends BaseMapper<DecisionLog> {
}
