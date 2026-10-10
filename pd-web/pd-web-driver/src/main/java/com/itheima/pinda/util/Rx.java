package com.itheima.pinda.util;

import com.itheima.pinda.common.utils.PageResponse;

import java.util.Collections;
import java.util.List;

/**
 * 聚合层远程调用结果安全取值工具。
 * <p>
 * 远程调用（Feign）在超时、熔断或服务异常时可能返回 null，
 * 直接 {@code list.stream()} 等调用会触发 NPE。统一通过本工具取数，避免 HTTP 500。
 *
 * @author code-review
 */
public final class Rx {
    private Rx() {
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
