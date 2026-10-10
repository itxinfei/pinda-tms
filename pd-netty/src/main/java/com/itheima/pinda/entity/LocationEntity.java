package com.itheima.pinda.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "位置信息")
public class LocationEntity {
    public String getId() {
        return businessId + "#" + type + "#" + currentTime;
    }

    /**
     * 车辆Id
     */
    @Schema(description = "业务id, 快递员id 或者  车辆id")
    private String businessId;

    /**
     * 司机名称
     */
    @Schema(description = "司机名称")
    private String name;

    /**
     * 司机电话
     */
    @Schema(description = "司机电话")
    private String phone;

    /**
     * 车牌号
     */
    @Schema(description = "licensePlate")
    private String licensePlate;

    /**
     * 类型
     */
    @Schema(description = "类型，车辆：truck,快递员：courier")
    private String type;

    /**
     * 经度
     */
    @Schema(description = "经度")
    private String lng;

    /**
     * 维度
     */
    @Schema(description = "维度")
    private String lat;

    /**
     * 当前时间
     */
    @Schema(description = "当前时间 格式：yyyyMMddHHmmss")
    private String currentTime;

    @Schema(description = "所属车队")
    private String team;

    @Schema(description = "运输任务id")
    private String transportTaskId;

    /**
     * 坐标系标识（P0-4 北斗字段最小改造 · D-13）
     * <p>取值见 {@link com.itheima.pinda.enums.CoordSystem}：
     * WGS84 / GCJ02 / BD09 / CGCS2000。
     * 缺失时由消费端兜底为 BD09（与后端 BaiduMapUtils 一致）。</p>
     */
    @Schema(description = "坐标系标识 WGS84/GCJ02/BD09/CGCS2000，缺失默认 BD09")
    private String coordSystem;

    /**
     * 数据来源（P0-4 北斗字段最小改造 · D-13）
     * <p>取值见 {@link com.itheima.pinda.enums.LocationSource}：
     * MOBILE / PDA / JT808 / NETTY_TCP / HTTP。
     * 缺失时由消费端按入口通道兜底（HTTP 入口=MOBILE，TCP 入口=NETTY_TCP）。</p>
     */
    @Schema(description = "数据来源 MOBILE/PDA/JT808/NETTY_TCP/HTTP，缺失按入口兜底")
    private String source;
}