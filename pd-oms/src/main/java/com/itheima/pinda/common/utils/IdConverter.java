package com.itheima.pinda.common.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * ID 类型转换工具
 *
 * <p>共享域 DTO 对外仍使用 String 类型 ID，而 oms_ 新表实体已改为雪花 Long 主键、
 * 省市区为 Integer。差异在 Service/Controller 边界用本工具手工转换。</p>
 */
public final class IdConverter {

    private IdConverter() {
    }

    public static Long toLong(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toStr(Long id) {
        return id == null ? null : id.toString();
    }

    public static Integer toInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static List<Long> toLongList(List<String> ids) {
        if (ids == null) {
            return null;
        }
        List<Long> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Long converted = toLong(id);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    public static List<String> toStrList(List<Long> ids) {
        if (ids == null) {
            return null;
        }
        List<String> result = new ArrayList<>(ids.size());
        for (Long id : ids) {
            result.add(toStr(id));
        }
        return result;
    }

    public static List<Integer> toIntegerList(List<String> values) {
        if (values == null) {
            return null;
        }
        List<Integer> result = new ArrayList<>(values.size());
        for (String value : values) {
            Integer converted = toInteger(value);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }
}
