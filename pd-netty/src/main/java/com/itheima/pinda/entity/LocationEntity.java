package com.itheima.pinda.entity;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("位置信息")
public class LocationEntity {
    public String getId() {
        return businessId + "#" + type + "#" + currentTime;
    }

    /**
     * 车辆Id
     */
    @ApiModelProperty("业务id, 快递员id 或者  车辆id")
    private String businessId;

    /**
     * 司机名称
     */
    @ApiModelProperty("司机名称")
    private String name;

    /**
     * 司机电话
     */
    @ApiModelProperty("司机电话")
    private String phone;

    /**
     * 车牌号
     */
    @ApiModelProperty("licensePlate")
    private String licensePlate;

    /**
     * 类型
     */
    @ApiModelProperty("类型，车辆：truck,快递员：courier")
    private String type;

    /**
     * 经度
     */
    @ApiModelProperty("经度")
    private String lng;

    /**
     * 维度
     */
    @ApiModelProperty("维度")
    private String lat;

    /**
     * 当前时间
     */
    @ApiModelProperty("当前时间 格式：yyyyMMddHHmmss")
    private String currentTime;

    @ApiModelProperty("所属车队")
    private String team;

    @ApiModelProperty("运输任务id")
    private String transportTaskId;

    /**
     * 坐标系标识（P0-4 北斗字段最小改造 · D-13）
     * <p>取值见 {@link com.itheima.pinda.enums.CoordSystem}：
     * WGS84 / GCJ02 / BD09 / CGCS2000。
     * 缺失时由消费端兜底为 BD09（与后端 BaiduMapUtils 一致）。</p>
     */
    @ApiModelProperty("坐标系标识 WGS84/GCJ02/BD09/CGCS2000，缺失默认 BD09")
    private String coordSystem;

    /**
     * 数据来源（P0-4 北斗字段最小改造 · D-13）
     * <p>取值见 {@link com.itheima.pinda.enums.LocationSource}：
     * MOBILE / PDA / JT808 / NETTY_TCP / HTTP。
     * 缺失时由消费端按入口通道兜底（HTTP 入口=MOBILE，TCP 入口=NETTY_TCP）。</p>
     */
    @ApiModelProperty("数据来源 MOBILE/PDA/JT808/NETTY_TCP/HTTP，缺失按入口兜底")
    private String source;
}