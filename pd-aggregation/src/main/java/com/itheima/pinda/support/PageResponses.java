package com.itheima.pinda.support;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.itheima.pinda.common.utils.PageResponse;

/**
 * 聚合查询分页工具：统一页码兜底与 PageResponse 组装（R6 聚合查询去重收口）。
 *
 * 页码兜底的原因：调用方可能不传 page/pageSize——null 直传 MyBatis-Plus 的 Page
 * 会因拆箱 NPE / LIMIT null,null 产生 SQL 异常（500）；page < 1 同理会产生非法
 * LIMIT offset。故统一收敛为「第 1 页 / 每页 10 条」（与管理端、司机端既有口径一致）。
 */
public final class PageResponses {

    private PageResponses() {
    }

    /** 页码兜底：null 或 &lt;1 → 1 */
    public static int page(Integer page) {
        return (page == null || page < 1) ? 1 : page;
    }

    /** 每页条数兜底：null 或 &lt;1 → 10 */
    public static int pageSize(Integer pageSize) {
        return (pageSize == null || pageSize < 1) ? 10 : pageSize;
    }

    /** 由 MyBatis-Plus 分页结果组装统一分页响应 */
    public static <T> PageResponse<T> of(IPage<T> iPage, int page, int pageSize) {
        return PageResponse.<T>builder()
                .counts(iPage.getTotal())
                .pages(iPage.getPages())
                .pagesize(pageSize)
                .page(page)
                .items(iPage.getRecords())
                .build();
    }
}
