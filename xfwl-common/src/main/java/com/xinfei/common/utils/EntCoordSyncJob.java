package com.xinfei.common.utils;

import java.awt.geom.Path2D;
import java.util.List;
import java.util.Map;

/**
 * 区域多边形命中判定（纯计算，无外网依赖）。
 *
 * <p>批3（多厂商地图改造·MapGeoClient 收敛）后，本类原先持有的百度 HTTP 调用
 * （地理编码/逆地理/驾车距离）已全部迁往 {@code com.xinfei.common.geo.GeoClient}
 * 唯一出口，此处仅保留 {@link #isInScope} 围栏命中纯计算，勿再往回加外呼。</p>
 */
public class EntCoordSyncJob {

    /**
     * 判断点是否在区域内
     *
     * @param polygon   区域经纬度集合
     * @param longitude 经度
     * @param latitude  纬度
     * @return
     */
    public static boolean isInScope(List<Map> polygon, double longitude, double latitude) {
        if (polygon == null || polygon.isEmpty()) {
            return false;
        }

        Path2D.Double generalPath = new Path2D.Double();

        //获取第一个起点经纬度的坐标（仅读取，禁止修改入参，否则同一多边形被重复判定时会逐步丢失顶点）
        Map first = polygon.get(0);

        //通过移动到以double精度指定的指定坐标，把第一个起点添加到路径中
        generalPath.moveTo(Double.parseDouble(first.getOrDefault("lng", "").toString()), Double.parseDouble(first.getOrDefault("lat", "").toString()));

        //从第二个顶点开始依次连线，避免 remove(0) 对调用方集合产生副作用
        for (int i = 1; i < polygon.size(); i++) {
            Map d = polygon.get(i);
            generalPath.lineTo(Double.parseDouble(d.getOrDefault("lng", "").toString()), Double.parseDouble(d.getOrDefault("lat", "").toString()));
        }

        // 最后对多边形进行封闭，起点及终点
        generalPath.lineTo(Double.parseDouble(first.getOrDefault("lng", "").toString()), Double.parseDouble(first.getOrDefault("lat", "").toString()));

        //将直线绘制回最后一个 moveTo的坐标来关闭当前子路径。
        generalPath.closePath();

        //true如果指定的坐标在Shape边界内; 否则为false 。
        return generalPath.contains(longitude, latitude);
    }
}
