package com.xinfei.controller.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller / Converter 层 ID 类型转换工具
 *
 * <p>对外 Feign DTO（pd-service-user-domain）仍使用 String 类型 ID，
 * 而 member_ 新表实体已改为雪花 Long 主键。本工具负责两类边界转换：
 * String ↔ Long、String ↔ Integer。</p>
 */
public final class IdConverter {

    private IdConverter() {
    }

    /**
     * String ID → Long，空白或非法时返回 null
     */
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

    /**
     * Long ID → String，null 安全
     */
    public static String toStr(Long id) {
        return id == null ? null : id.toString();
    }

    /**
     * String → Integer，空白或非法时返回 null
     */
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

    /**
     * String ID 列表 → Long 列表；入参为 null 时返回 null（保持"不加 in 条件"语义）
     */
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

    /**
     * Long ID 列表 → String 列表；入参为 null 时返回 null
     */
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
}
