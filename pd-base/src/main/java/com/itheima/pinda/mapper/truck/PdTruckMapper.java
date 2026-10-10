package com.itheima.pinda.mapper.truck;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.truck.PdTruck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 车辆信息表 Mapper 接口
 */
@Mapper
public interface PdTruckMapper extends BaseMapper<PdTruck> {

    /**
     * 更新车辆在线状态与心跳时间。
     *
     * <p>由 pd-netty 收到 GPS 上报后经 Feign 调用。businessId 语义为车辆主键 id
     * （种子数据 device_gps_id 全为 NULL，旧 SQL 按 device_gps_id 匹配永远命中 0 行）。
     * 走当前租户隔离，租户插件自动追加 tenant_id 条件。</p>
     *
     * @param truckId       车辆主键 id（上报 businessId）
     * @param heartbeatTime 心跳时间
     * @return 受影响行数
     */
    @Update("UPDATE base_truck SET online_status = 1, last_heartbeat_time = #{heartbeatTime} " +
            "WHERE id = #{truckId} AND status = 1 AND deleted = 0")
    int updateHeartbeat(@Param("truckId") Long truckId,
                        @Param("heartbeatTime") LocalDateTime heartbeatTime);

    /**
     * 批量将心跳超时车辆置为离线。
     *
     * <p>由 pd-netty 定时任务经 Feign 调用，需跨所有租户扫描，故用
     * {@link InterceptorIgnore} 关闭租户条件；只处理未删除的正常车辆。</p>
     *
     * @param threshold 心跳超时阈值
     * @return 受影响行数
     */
    @InterceptorIgnore(tenantLine = "true")
    @Update("UPDATE base_truck SET online_status = 0 " +
            "WHERE status = 1 AND online_status = 1 AND deleted = 0 " +
            "AND (last_heartbeat_time IS NULL OR last_heartbeat_time < #{threshold})")
    int markOfflineByHeartbeat(@Param("threshold") LocalDateTime threshold);
}
