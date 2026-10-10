package com.itheima.pinda.util;

import com.itheima.pinda.common.utils.PageResponse;

import java.util.Collections;
import java.util.List;

/**
 * 聚合层远程调用结果安全取值工具。
 * <p>
 * 远程调用（Feign）在超时、熔断或服务异常时可能返回 null（含 fallback 返回 null），
 * 直接 {@code data.stream()} 等调用会触发 NPE。统一通过本工具取数，避免 HTTP 500。
 * <p>
 * 新认证契约下 Feign 为裸返回（不再有 R 包装），{@link #data} 即对结果做 null 安全透传。
 *
 * @author code-review
 */
public final class Rx {
    private Rx() {
    }

    /**
     * 安全获取 Feign 裸返回的对象，结果为 null 时返回 null（不会触发 NPE）。
     *
     * @param value 远程调用结果
     * @param <T> 数据类型
     * @return 结果本身或 null
     */
    public static <T> T data(T value) {
        return value;
    }

    /**
     * 安全获取 Feign 裸返回的列表，为 null 时返回不可变空集合。
     *
     * @param list 远程调用返回的列表
     * @param <T> 列表元素类型
     * @return 列表（不会为 null）
     */
    public static <T> List<T> dataList(List<T> list) {
        return list(list);
    }

    /**
     * 安全获取 Feign 直接返回的列表（非 R 包装），为 null 时返回不可变空集合。
     *
     * @param list 列表
     * @param <T> 元素类型
     * @return 列表（不会为 null）
     */
    public static <T> List<T> list(List<T> list) {
        return list != null ? list : Collections.emptyList();
    }

    /**
     * 安全获取分页响应中的 items，为 null 时返回不可变空集合。
     *
     * @param page 分页响应
     * @param <T> 元素类型
     * @return items（不会为 null）
     */
    public static <T> List<T> items(PageResponse<T> page) {
        if (page != null && page.getItems() != null) {
            return page.getItems();
        }
        return Collections.emptyList();
    }
}
