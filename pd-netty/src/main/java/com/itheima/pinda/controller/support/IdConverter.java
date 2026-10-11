package com.itheima.pinda.controller.support;

import java.util.ArrayList;
import java.util.List;

/**
 * ID 类型转换工具
 *
 * <p>设备上报协议模型（{@code LocationEntity}）对外仍使用 String 类型 ID，
 * 而 iot_ 新表实体已改为雪花 Long 主键。本工具负责 String ↔ Long 边界转换。
 * pd-netty 不依赖 pd-base，故在本模块单独维护一份。</p>
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
     * String ID 列表 → Long 列表；入参为 null 时返回 null（保持“不加 in 条件”语义），
     * 非法元素被剔除
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
}
