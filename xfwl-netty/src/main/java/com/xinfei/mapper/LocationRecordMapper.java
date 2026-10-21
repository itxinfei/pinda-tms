package com.xinfei.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xinfei.entity.LocationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * GPS轨迹明细 Mapper
 */
@Mapper
public interface LocationRecordMapper extends BaseMapper<LocationRecord> {

    /**
     * 批量查询每个业务对象（business_id + business_type）的最新一条位置
     *
     * <p>自联结取 MAX(report_time)，供管理端在途监控一次性拉取全部车辆实时位置。
     * 分组带 business_type：车辆与快递员 ID 属不同表，仅按 business_id 分组会串行。</p>
     *
     * @param businessType 业务类型数字码（1车辆 2快递员，null 查全部）
     * @param licensePlate 车牌号模糊过滤（null 不过滤）
     * @return 每个业务对象最新一条轨迹记录
     */
    @Select("<script>"
            + "SELECT t.* FROM iot_truck_location t "
            + "INNER JOIN ("
            + "  SELECT business_id, business_type, MAX(report_time) AS max_time"
            + "  FROM iot_truck_location"
            + "  <where>"
            + "    <if test='businessType != null'>business_type = #{businessType}</if>"
            + "    <if test='licensePlate != null and licensePlate != \"\"'>"
            + "      AND license_plate LIKE CONCAT('%', #{licensePlate}, '%')"
            + "    </if>"
            + "  </where>"
            + "  GROUP BY business_id, business_type"
            + ") m ON t.business_id = m.business_id AND t.business_type = m.business_type"
            + " AND t.report_time = m.max_time"
            + " ORDER BY t.report_time DESC"
            + "</script>")
    List<LocationRecord> selectLatestPerBusiness(@Param("businessType") Integer businessType,
                                                 @Param("licensePlate") String licensePlate);
}
