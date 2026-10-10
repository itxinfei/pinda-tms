package com.itheima.pinda.constant;

/**
 * 数据库岗位 ID 常量（对应 pd_core_station 表）。
 *
 * <p>注意：这是数据库岗位ID，与客户端角色编码（COURIER、DRIVER）是两套体系，切勿混用。</p>
 */
public final class StaticStation {

    /** 快递员岗位ID */
    public static final Long COURIER_ID = 3L;

    /** 司机岗位ID */
    public static final Long DRIVER_ID = 2L;

    private StaticStation() {
    }
}
