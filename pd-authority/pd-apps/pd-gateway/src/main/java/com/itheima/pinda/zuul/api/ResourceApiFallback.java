package com.itheima.pinda.zuul.api;

import com.itheima.pinda.authority.dto.auth.ResourceQueryDTO;
import com.itheima.pinda.authority.entity.auth.Resource;
import com.itheima.pinda.base.R;
import org.springframework.stereotype.Component;

import java.util.List;
/**
 * 资源API熔断
 */
@Component
public class ResourceApiFallback implements ResourceApi {
    @Override
    public R<List> list() {
        // 鉴权服务不可用时返回超时错误态（禁止返回 null，否则 AccessFilter 直接 NPE）
        return R.timeout();
    }

    @Override
    public R<List<Resource>> visible(ResourceQueryDTO resource) {
        // 同上，错误态交由 AccessFilter fail-closed 处理，不返回 null
        return R.timeout();
    }
}
