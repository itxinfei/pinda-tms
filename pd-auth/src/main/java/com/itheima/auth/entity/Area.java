package com.itheima.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 行政区划。全局共享数据，id 为国家标准行政区划代码（如 110000），不参与租户隔离。
 * 层级 level：0 省级 1 市级 2 县级 3 镇级 4 乡村级。
 */
@Data
@TableName("pd_area")
public class Area {

    /** 国家标准行政区划代码，由外部数据提供，不使用雪花ID */
    @TableId(type = IdType.INPUT)
    private Long id;

    /** 父级行政区划代码，省级为 0 */
    private Long parentId;

    /** 全称 */
    private String name;

    /** 行政区划代码 */
    private String areaCode;

    /** 城市代码 */
    private String cityCode;

    /** 完整合并名称，如 北京市,北京市,东城区 */
    private String mergerName;

    /** 简称 */
    private String shortName;

    /** 邮编 */
    private String zipCode;

    /** 层级 0省 1市 2县 3镇 4乡村 */
    private Integer level;

    /** 经度 */
    private String lng;

    /** 纬度 */
    private String lat;

    /** 拼音 */
    private String pinyin;

    /** 名称首字母 */
    private String first;

    private LocalDateTime updateTime;
    private Long updateUser;
    private LocalDateTime createTime;
    private Long createUser;
}
