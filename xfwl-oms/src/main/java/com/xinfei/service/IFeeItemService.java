package com.xinfei.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import com.xinfei.entity.FeeItem;

/**
 * 附加费费用项配置服务（POD-03 第一期 / FA-10）
 */
public interface IFeeItemService extends IService<FeeItem> {

    /**
     * 新增/修改费用项（id 空=新增；fee_code 全租户唯一；calc_type=1 必填 fixed_amount，=2 必填 ratio）
     *
     * @return 落库后的费用项；参数非法抛 IllegalArgumentException
     */
    FeeItem saveItem(FeeItem item, Long operator);

    /** 停用费用项（status→0）；返回 false=不存在或已停用 */
    boolean disable(Long id, Long operator);

    /** 分页查询（feeCode/status 过滤） */
    Page<FeeItem> page(Integer page, Integer pageSize, String feeCode, Integer status);
}
