package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作/登录日志 Mapper（跨服务审计埋点：仅写入，查询归 xf-auth 统一出口）。
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
