package com.itheima.pinda.authority.enumeration.common;

/**
 * 数据库岗位 ID 常量（对应 pd_core_station 表）
 *
 * <p><b>重要说明</b>：本常量是<b>数据库岗位ID</b>，与 pd-common 中 {@code Constant.UserStation}
 * 的客户端角色编码（COURIER=2、DRIVER=3）是<b>两套不同的编码体系</b>，映射关系为：
 * 客户端 COURIER(2) ↔ 本常量 COURIER_ID(3)；客户端 DRIVER(3) ↔ 本常量 DRIVER_ID(2)。
 * 使用时务必区分，切勿混用。</p>
 */
public class StaticStation {
    public static final Long COURIER_ID=3L;
    public static final Long DRIVER_ID=2L;
}
