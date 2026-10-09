package com.itheima.pinda.enums;

/**
 * 定位数据来源枚举（P0-4 北斗字段最小改造 · D-13）
 *
 * <p>标识 GPS 数据的来源设备/通道，便于后续按来源分流处理
 * （如 JT/T 808 终端走标准协议解析、移动端走 HTTP 推送等）。</p>
 */
public enum LocationSource {

    /** 移动端 HTTP 推送（司机小程序/快递员 App 走 /netty/push） */
    MOBILE("MOBILE", "移动端推送"),

    /** 快递员 PDA 手持设备 */
    PDA("PDA", "PDA 手持"),

    /** JT/T 808 北斗终端（P1 完整接入后启用） */
    JT808("JT808", "JT/T 808 北斗终端"),

    /** 其它 TCP 设备（NettyServerHandler 直接接收的 TCP 报文） */
    NETTY_TCP("NETTY_TCP", "TCP 设备"),

    /** HTTP 兜底通道（未识别来源时） */
    HTTP("HTTP", "HTTP 通道");

    private final String code;
    private final String description;

    LocationSource(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static LocationSource of(String code) {
        if (code == null) {
            return null;
        }
        for (LocationSource ls : values()) {
            if (ls.code.equalsIgnoreCase(code.trim())) {
                return ls;
            }
        }
        return null;
    }

    public static boolean isValid(String code) {
        return of(code) != null;
    }
}
