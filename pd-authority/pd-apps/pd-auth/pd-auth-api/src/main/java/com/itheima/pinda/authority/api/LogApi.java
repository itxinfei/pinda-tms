package com.itheima.pinda.authority.api;

import com.itheima.pinda.authority.api.hystrix.LogApiHystrix;
import com.itheima.pinda.base.R;
import com.itheima.pinda.log.entity.OptLogDTO;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 操作日志保存 API
 *
 */
@FeignClient(name = "${pinda.feign.authority-server:pd-auth-server}", fallback = LogApiHystrix.class)
public interface LogApi {
    /**
     * 保存日志
     *
     * @param log 日志
     * @return
     */
    @RequestMapping(value = "/optLog", method = RequestMethod.POST)
    R<OptLogDTO> save(@RequestBody OptLogDTO log);

}
