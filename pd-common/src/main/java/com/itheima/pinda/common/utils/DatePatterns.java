package com.itheima.pinda.common.utils;

/**
 * 统一日期格式常量（下沉自旧 pd-tools-core 的 DateUtils）。
 * 供 Jackson 序列化/反序列化及各服务全局配置复用。
 */
public final class DatePatterns {
    /** 日期格式：yyyy-MM-dd */
    public static final String DATE = "yyyy-MM-dd";

    /** 日期时间格式：yyyy-MM-dd HH:mm:ss */
    public static final String DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    /** 时间格式：HH:mm:ss */
    public static final String TIME = "HH:mm:ss";

    private DatePatterns() {
    }
}
