package com.itheima.pinda.utils;

import com.itheima.pinda.entity.LocationEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

/**
 * GPS 上报数据校验工具
 *
 * <p>统一 HTTP（{@code NettyController}）与 TCP（{@code NettyServerHandler}）两个入口的
 * 校验口径，非法数据一律不进 RabbitMQ：
 * <ul>
 *   <li>经纬度必须是合法数字（拒绝 NaN / Infinity / 科学计数法以外的脏字符串）；</li>
 *   <li>经度范围 [-180,180]，纬度范围 [-90,90]；</li>
 *   <li>收窄到中国境内：本系统是国内物流 TMS，境外坐标直接拒绝。</li>
 * </ul>
 *
 * @author Claude Code
 * @since 2026-10-07
 */
public final class GpsLocationValidator {

    private GpsLocationValidator() {
    }

    /**
     * 十进制数字格式（可选负号 + 整数位 + 可选小数位），从格式上排除 NaN/Infinity
     */
    private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    /**
     * 设备时间格式：yyyyMMddHHmmss
     */
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{14}$");

    /**
     * 严格时间解析器（uuuu 适配 STRICT 模式，避免 yyyy 缺纪元报错；非法日期如 0230 日直接拒绝）
     */
    private static final DateTimeFormatter STRICT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);

    /**
     * 校验经纬度字符串：数字格式 + 全球范围
     *
     * @param lngStr 经度字符串
     * @param latStr 纬度字符串
     * @return true-合法
     */
    public static boolean isValidLngLat(String lngStr, String latStr) {
        if (lngStr == null || latStr == null
                || !NUMBER_PATTERN.matcher(lngStr.trim()).matches()
                || !NUMBER_PATTERN.matcher(latStr.trim()).matches()) {
            return false;
        }
        double lng;
        double lat;
        try {
            lng = Double.parseDouble(lngStr.trim());
            lat = Double.parseDouble(latStr.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        return lng >= -180.0 && lng <= 180.0 && lat >= -90.0 && lat <= 90.0;
    }

    /**
     * 校验坐标是否位于中国境内（口径与 {@link CoordTransform#outOfChina} 完全一致）
     *
     * @param lng 经度
     * @param lat 纬度
     * @return true-境内
     */
    public static boolean isWithinChina(double lng, double lat) {
        return !CoordTransform.outOfChina(lng, lat);
    }

    /**
     * 校验设备上报时间：必须为 yyyyMMddHHmmss 且为真实存在的日期时间
     *
     * @param currentTime 设备时间字符串
     * @return true-合法
     */
    public static boolean isValidCurrentTime(String currentTime) {
        if (currentTime == null || !TIME_PATTERN.matcher(currentTime.trim()).matches()) {
            return false;
        }
        try {
            LocalDateTime.parse(currentTime.trim(), STRICT_TIME_FORMATTER);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 上报实体必填字段统一校验（HTTP/TCP 两入口共用）
     *
     * <p>校验项：businessId、type（仅 truck/courier）、currentTime（yyyyMMddHHmmss）、
     * lng/lat（数字、范围、中国境内）。</p>
     *
     * @param location 上报位置实体
     * @return null 表示校验通过；否则为可直接返回给调用方的中文错误信息
     */
    public static String validateRequired(LocationEntity location) {
        if (location == null) {
            return "上报数据不能为空";
        }
        if (isBlank(location.getBusinessId())) {
            return "businessId 不能为空（车辆id或快递员id）";
        }
        if (isBlank(location.getType())) {
            return "type 不能为空（truck/courier）";
        }
        String type = location.getType().trim();
        if (!"truck".equalsIgnoreCase(type) && !"courier".equalsIgnoreCase(type)) {
            return "type 非法，仅支持 truck/courier";
        }
        if (!isValidCurrentTime(location.getCurrentTime())) {
            return "currentTime 非法，格式必须为真实的 yyyyMMddHHmmss（如 20261007143000）";
        }
        if (isBlank(location.getLng()) || isBlank(location.getLat())) {
            return "经纬度不能为空";
        }
        if (!isValidLngLat(location.getLng(), location.getLat())) {
            return "经纬度格式或范围非法，要求经度[-180,180]、纬度[-90,90]的十进制数字";
        }
        double lng = Double.parseDouble(location.getLng().trim());
        double lat = Double.parseDouble(location.getLat().trim());
        if (!isWithinChina(lng, lat)) {
            return "仅支持中国境内经纬度";
        }
        return null;
    }

    /**
     * 空白判断（不引入对具体 StringUtils 实现的依赖，保证工具可独立使用）
     */
    private static boolean isBlank(String value) {
        if (value == null) {
            return true;
        }
        int len = value.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
