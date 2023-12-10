package com.itheima.pinda.authority.api.hystrix;

import com.itheima.pinda.authority.api.LogApi;
import com.itheima.pinda.base.R;
import com.itheima.pinda.log.entity.OptLogDTO;

import org.springframework.stereotype.Component;

/**
 * 日志操作 熔断
 */
@Component
public class LogApiHystrix implements LogApi {
    @Override
    public R<OptLogDTO> save(OptLogDTO log) {
        return R.timeout();
    }
}
