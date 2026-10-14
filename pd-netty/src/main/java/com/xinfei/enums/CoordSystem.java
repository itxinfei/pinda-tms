package com.xinfei.enums;

/**
 * 坐标系类型枚举（P0-4 北斗字段最小改造 · D-13）
 *
 * <p>用于标识 GPS 上报数据所属的坐标参照系，便于后续坐标转换与合规追溯。
 * 默认 BD09，与后端 {@code BaiduMapUtils}（pd-oms 模块）保持一致——
 * 管理端轨迹回放直接渲染，无需前端做坐标转换。</p>
 */
public enum CoordSystem {

    /** WGS84：GPS 原始坐标系（北斗/GPS 终端默认） */
    WGS84(1, "WGS84", "WGS-84 原始坐标"),

    /** GCJ02：国测局火星坐标（高德/腾讯地图） */
    GCJ02(2, "GCJ02", "国测局火星坐标"),

    /** BD09：百度坐标（后端 BaiduMapUtils 默认） */
    BD09(3, "BD09", "百度坐标"),

    /** CGCS2000：国家大地坐标系（北斗民用） */
    CGCS2000(4, "CGCS2000", "国家大地坐标系 2000");

    /** 数字码（iot 表 coord_system TINYINT：1WGS84 2GCJ02 3BD09 4CGCS2000） */
    private final int numericCode;

    private final String code;
    private final String description;

    CoordSystem(int numericCode, String code, String description) {
        this.numericCode = numericCode;
        this.code = code;
        this.description = description;
    }

    public int getNumericCode() {
        return numericCode;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 字符串转枚举（不区分大小写），不匹配返回 null
     */
    public static CoordSystem of(String code) {
        if (code == null) {
            return null;
        }
        for (CoordSystem cs : values()) {
            if (cs.code.equalsIgnoreCase(code.trim())) {
                return cs;
            }
        }
        return null;
    }

    /**
     * 校验坐标系代码是否合法
     */
    public static boolean isValid(String code) {
        return of(code) != null;
    }
}
