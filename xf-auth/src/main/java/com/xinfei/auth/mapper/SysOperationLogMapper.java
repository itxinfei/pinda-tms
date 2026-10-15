package com.xinfei.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.auth.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 操作日志 Mapper（含统计查询）。
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {

    /** 按天统计 PV/UV */
    @Select("<script>" +
            "SELECT DATE(create_time) AS day, COUNT(*) AS pv, COUNT(DISTINCT operator_id) AS uv " +
            "FROM sys_operation_log " +
            "WHERE create_time BETWEEN #{start} AND #{end} " +
            "GROUP BY DATE(create_time) ORDER BY day" +
            "</script>")
    List<Map<String, Object>> groupByDay(@Param("start") String start, @Param("end") String end);

    @Select("SELECT COUNT(*) FROM sys_operation_log WHERE DATE(create_time) = #{day}")
    Long countByDay(@Param("day") String day);

    @Select("SELECT COUNT(DISTINCT operator_id) FROM sys_operation_log WHERE DATE(create_time) = #{day}")
    Long distinctOperatorByDay(@Param("day") String day);

    @Select("SELECT COUNT(*) FROM sys_operation_log")
    Long totalCount();

    @Select("SELECT COUNT(DISTINCT operator_id) FROM sys_operation_log")
    Long totalDistinctOperator();
}
