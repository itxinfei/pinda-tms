package com.itheima.pinda.feign.agency.hystrix;

import com.itheima.pinda.DTO.angency.AgencyScopeDto;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.agency.AgencyScopeFeign;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 机构业务范围Feign接口熔断降级
 */
@Slf4j
@Component
public class AgencyScopeFeignFallback implements AgencyScopeFeign {

    @Override
    public Result batchSaveAgencyScope(List<AgencyScopeDto> dtoList) {
        log.warn("远程调用 pd-base 失败: batchSaveAgencyScope({}), 返回失败结果", dtoList);
        return Result.error("服务降级，batchSaveAgencyScope执行失败");
    }

    @Override
    public Result deleteAgencyScope(AgencyScopeDto dto) {
        log.warn("远程调用 pd-base 失败: deleteAgencyScope({}), 返回失败结果", dto);
        return Result.error("服务降级，deleteAgencyScope执行失败");
    }

    @Override
    public List<AgencyScopeDto> findAllAgencyScope(String areaId, String agencyId, List<String> agencyIds, List<String> areaIds) {
        log.warn("远程调用 pd-base 失败: findAllAgencyScope(areaId={}, agencyId={}, agencyIds={}, areaIds={}), 返回空列表", areaId, agencyId, agencyIds, areaIds);
        return Collections.emptyList();
    }
}
