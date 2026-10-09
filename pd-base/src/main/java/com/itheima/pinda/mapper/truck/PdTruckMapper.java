package com.itheima.pinda.mapper.truck;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itheima.pinda.entity.truck.PdTruck;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * <p>
 * 车辆信息表  Mapper 接口
 * </p>
 *
 * @author itcast
 * @since 2019-12-20
 */
@Mapper
public interface PdTruckMapper extends BaseMapper<PdTruck> {

    /**
     * 更新车辆在线状态与心跳时间（P0-4 心跳键错配修复）
     *
     * <p>由 pd-netty GpsTraceConsumer 收到 GPS 上报后调用。
     * 上报消息中的 businessId 语义就是车辆主键 id（种子数据 device_gps_id 全为 NULL，
     * 旧 SQL 按 device_gps_id 匹配永远命中 0 行，导致所有车恒离线），
     * 故更新条件改为按主键 id 匹配。
     * 使用 UPDATE 直接操作，避免 MybatisPlus 自动填充干扰。</p>
     *
     * @param truckId 车辆主键 id（上报 businessId）
     * @param heartbeatTime 心跳时间（pd-netty 服务端时间）
     * @return 受影响行数
     */
    @Update("UPDATE pd_truck SET online_status = 1, last_heartbeat_time = #{heartbeatTime} " +
            "WHERE id = #{truckId} AND status = 1")
    int updateHeartbeat(@Param("truckId") String truckId,
                        @Param("heartbeatTime") LocalDateTime heartbeatTime);

    /**
     * 批量将心跳超时车辆置为离线（P0-4 北斗字段最小改造 · D-13）
     *
     * <p>由 pd-netty @Scheduled 定时任务调用（每 60s 扫描），
     * 将 last_heartbeat_time 早于阈值的车辆 online_status 置 0。
     * 只处理 status=1 正常车辆，避免禁用车辆被反复更新。</p>
     *
     * @param threshold 心跳超时阈值
     * @return 受影响行数
     */
    @Update("UPDATE pd_truck SET online_status = 0 " +
            "WHERE status = 1 AND online_status = 1 " +
            "AND (last_heartbeat_time IS NULL OR last_heartbeat_time < #{threshold})")
    int markOfflineByHeartbeat(@Param("threshold") LocalDateTime threshold);
}
