package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * youlai 契约分页结果：{list,total}。
 *
 * <p>区别于心飞 {@code PageResponse{items,counts,page,pagesize,pages}}；
 * v1 控制器统一用本结构返回，前端 PageResult&lt;T&gt; 直接消费。</p>
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> list;
    private long total;

    public PageResult() {
    }

    public PageResult(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public static <T> PageResult<T> of(List<T> list, long total) {
        return new PageResult<>(list, total);
    }
}
