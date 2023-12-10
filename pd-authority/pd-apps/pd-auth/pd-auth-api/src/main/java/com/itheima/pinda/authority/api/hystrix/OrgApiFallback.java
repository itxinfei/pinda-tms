package com.itheima.pinda.authority.api.hystrix;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.api.OrgApi;
import com.itheima.pinda.authority.dto.core.OrgTreeDTO;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.base.R;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 组织API熔断
 */
@Component
public class OrgApiFallback implements OrgApi {

    @Override
    public R<Org> get(Long id) {
        return R.fail("未获取到组织");
    }

    @Override
    public R<List<Org>> list(Integer orgType, List<Long> ids, Long countyId, Long pid, List<Long> pids) {
        return R.success(new ArrayList<>());
    }

    @Override
    public R<List<OrgTreeDTO>> tree(String name, Boolean status) {
        return R.success(new ArrayList<>());
    }

    @Override
    public R<Page> pageLike(Integer size, Integer current, String keyword, Long cityId, String latitude, String longitude) {
        return R.timeout();
    }

    @Override
    public R<List<Org>> listByCountyIds(Integer orgType, List<Long> countyIds) {
        return R.timeout();
    }
}
