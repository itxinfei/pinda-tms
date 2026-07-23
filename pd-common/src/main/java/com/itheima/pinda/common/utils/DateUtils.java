package com.itheima.pinda.common.utils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public class DateUtils {
    /**
     * 将Asia/Shanghai时区的本地时间转换为UTC时间
     * @param shanghaiTime Asia/Shanghai时区的时间
     * @return UTC时间
     */
    public static LocalDateTime shanghaiToUTC(LocalDateTime shanghaiTime) {
        if (shanghaiTime == null) return null;
        ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
        ZonedDateTime shanghaiZoned = ZonedDateTime.of(shanghaiTime, shanghaiZone);
        return shanghaiZoned.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /**
     * 将UTC时间转换为Asia/Shanghai时区时间
     * @param utcTime UTC时间
     * @return Asia/Shanghai时区时间
     */
    public static LocalDateTime utcToShanghai(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        return utcTime.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime();
    }

    /**
     * 获取utc时间（保留兼容旧接口）
     * @param localDateTime 本地时间（按Asia/Shanghai处理）
     * @return UTC时间
     * @deprecated 请使用 {@link #shanghaiToUTC(LocalDateTime)} 替代
     */
    public static LocalDateTime getUTCTime(LocalDateTime localDateTime) {
        return shanghaiToUTC(localDateTime);
    }
}
