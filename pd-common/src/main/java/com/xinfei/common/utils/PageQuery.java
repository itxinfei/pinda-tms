package com.xinfei.common.utils;

/**
 * 分页参数规范化（《12》§8.4 / D-05）。
 *
 * <p>基准取 pd-netty 既有防御式校验口径：{@code page} 下限 1，
 * {@code pageSize} 区间 [1, 500]。null 或非法入参回退默认值（1 / 10），
 * 不再静默透传 {@code page=0}、{@code pageSize=100000}（会原样回显打穿前端分页器、
 * 并与空集富化叠加放大伤害）。</p>
 *
 * <p>新写分页接口禁止裸接收 {@code page}/{@code size}/{@code limit}/{@code offset}
 * 后直传下游，必须先过本工具。</p>
 */
public final class PageQuery {

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 500;

    private PageQuery() {
    }

    /**
     * 规范化分页入参。
     *
     * @return {@code int[]{page, pageSize}}，已 clamp 到合法区间
     */
    public static int[] normalize(Integer page, Integer pageSize) {
        int p = (page == null || page < 1) ? DEFAULT_PAGE : page;
        int s = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new int[]{p, s};
    }
}
