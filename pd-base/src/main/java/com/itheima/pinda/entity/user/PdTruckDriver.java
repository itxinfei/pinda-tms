package com.itheima.pinda.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 司机（员工资料，登录账号为 sys_user）
 */
@Data
@TableName("base_truck_driver")
public class PdTruckDriver {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 关联账号 sys_user.id */
    private Long userId;

    /** 所属车队ID */
    private Long fleetId;

    /** 年龄 */
    private Integer age;

    /** 驾龄 */
    private Integer drivingAge;

    /** 照片 */
    private String picture;

    private Long createBy;
    private LocalDateTime createTime;
    private Long updateBy;
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
