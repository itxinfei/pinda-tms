package com.itheima.pinda.controller.support;

import java.math.BigDecimal;

/**
 * 坐标字符串转换工具（Controller 边界）
 *
 * <p>共享 DTO 用单个字符串 "经度,纬度" 表达坐标，新表 oms_order_location 拆为
 * send/receive 各经纬度两列。本工具负责双向转换：</p>
 * <ul>
 *   <li>按英文逗号拆分并 trim，两段都能解析为 BigDecimal 才有效，否则返回 null；</li>
 *   <li>反向仅当经纬度都非空才拼接，避免出现 "null,null"。</li>
 * </ul>
 */
public final class CoordinateParser {

    private CoordinateParser() {
    }

    /**
     * 坐标值（经度、纬度）
     */
    public record Coordinate(BigDecimal longitude, BigDecimal latitude) {
    }

    /**
     * 解析 "经度,纬度"；格式非法返回 null。
     *
     * @param text 坐标字符串
     * @return 坐标值
     */
    public static Coordinate parse(String text) {
        if (text == null) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            BigDecimal longitude = new BigDecimal(parts[0].trim());
            BigDecimal latitude = new BigDecimal(parts[1].trim());
            return new Coordinate(longitude, latitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 拼接 "经度,纬度"；任一为空返回 null。
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @return 坐标字符串
     */
    public static String format(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            return null;
        }
        return longitude.toPlainString() + "," + latitude.toPlainString();
    }
}
