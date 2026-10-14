package com.xinfei.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ID 类型转换工具
 *
 * <p>disp_ 新表外键全部落 BIGINT（Long），而跨域 Feign 边界协议存在两种口径：
 * <ul>
 *   <li>pd-auth（机构/用户）、pd-work（运单/任务/司机作业）已 Long 化；</li>
 *   <li>pd-oms（订单）、pd-base（线路/车次/车辆）仍以 String 协议交互。</li>
 * </ul>
 * 本工具负责落库点与 Feign 调用边界的 String ↔ Long 转换，空白/非法值返回 null，不抛异常。</p>
 */
public final class IdConvert {

    private IdConvert() {
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
     * String ID 集合 → Long 集合（保持迭代顺序，剔除空白/非法元素）
     */
    public static Set<Long> toLongSet(Collection<String> ids) {
        Set<Long> result = new LinkedHashSet<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            Long converted = toLong(id);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }

    /**
     * Long ID 集合 → String 集合（保持迭代顺序，剔除 null 元素）
     */
    public static Set<String> toStrSet(Collection<Long> ids) {
        Set<String> result = new LinkedHashSet<>();
        if (ids == null) {
            return result;
        }
        for (Long id : ids) {
            if (id != null) {
                result.add(id.toString());
            }
        }
        return result;
    }

    /**
     * String ID 列表 → Long 列表（保持迭代顺序，剔除空白/非法元素）
     */
    public static List<Long> toLongList(Collection<String> ids) {
        List<Long> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            Long converted = toLong(id);
            if (converted != null) {
                result.add(converted);
            }
        }
        return result;
    }
}
