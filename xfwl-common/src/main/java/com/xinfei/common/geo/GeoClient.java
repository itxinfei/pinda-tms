package com.xinfei.common.geo;

import java.util.Map;

/**
 * 地理服务统一客户端（多厂商地图改造·批3 收敛点，D-66）。
 *
 * <p>收敛前全仓两套并行实现（{@code BaiduMapUtils} Bean 版 + {@code EntCoordSyncJob} 静态版）
 * 直连百度 HTTP，本接口是后端唯一的地理能力出口，厂商实现可替换（批4 厂商配置化据此切换）。</p>
 *
 * <p><b>坐标序契约（拍板 2026-10-17）：全部方法的坐标字符串一律为 {@code "lng,lat"}</b>。
 * 百度 Web 服务 API 的 URL 入参为 {@code lat,lng}，序适配由厂商实现内部在拼 URL 边界完成，
 * 业务方永远只见 {@code lng,lat}。</p>
 *
 * <p><b>计费距离锁定（D-66 ②）</b>：订单算价的驾车距离必须显式注入百度实现
 * （{@code @Qualifier("baiduGeoClient")}），批4 切换展示厂商时不得静默改变计费源。</p>
 */
public interface GeoClient {

    /**
     * 地理编码：地址 → 坐标。
     *
     * @param address 地址（实现内部做去空格与「#」→「栋」预处理）
     * @return 坐标字符串 {@code "lng,lat"}；地址为空或解析失败返回 {@code null}
     */
    String geocode(String address);

    /**
     * 逆地理编码：坐标 → 行政区划等地址组件。
     *
     * @param lngLat 坐标字符串 {@code "lng,lat"}（WGS84）
     * @return 百度返回的 addressComponent Map（含 {@code adcode}、{@code formatted_address} 等）；失败返回 {@code null}
     */
    Map<String, Object> reverseGeocode(String lngLat);

    /**
     * 驾车路线距离（计费距离源，D-66 ②）。
     *
     * @param origin      起点 {@code "lng,lat"}
     * @param destination 终点 {@code "lng,lat"}
     * @return 距离（米）；失败返回 {@code null}
     */
    Double drivingDistance(String origin, String destination);
}
