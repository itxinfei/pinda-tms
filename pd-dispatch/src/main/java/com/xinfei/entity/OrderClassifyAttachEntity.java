package com.xinfei.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 分类-派配关联表（disp_classify_attach）：车次/车辆/司机
 *
 * <p>车次/车辆/司机ID 落库为 BIGINT；pd-base 车次车辆 Feign 以 String 协议交互，
 * 落库点完成转换。主键雪花 Long；tenant_id 由 TenantLineInnerInterceptor 自动填充。</p>
 */
@Data
@TableName("disp_classify_attach")
public class OrderClassifyAttachEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键（雪花）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 分类ID
     */
    private Long classifyId;

    /**
     * 车次ID（base_transport_trips.id，落库 BIGINT）
     */
    private Long tripsId;

    /**
     * 车辆ID（base_truck.id，落库 BIGINT）
     */
    private Long truckId;

    /**
     * 司机ID（base_truck_driver.id / pd-auth 用户ID）
     */
    private Long driverId;

    /**
     * 创建时间
     */
    private Date createTime;
}
