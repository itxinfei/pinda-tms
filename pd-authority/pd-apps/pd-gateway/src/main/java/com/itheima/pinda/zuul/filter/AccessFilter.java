package com.itheima.pinda.zuul.filter;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.authority.dto.auth.ResourceQueryDTO;
import com.itheima.pinda.authority.entity.auth.Resource;
import com.itheima.pinda.base.R;
import com.itheima.pinda.common.constant.CacheKey;
import com.itheima.pinda.context.BaseContextConstants;
import com.itheima.pinda.exception.code.ExceptionCode;
import com.itheima.pinda.zuul.api.ResourceApi;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import net.oschina.j2cache.CacheChannel;
import net.oschina.j2cache.CacheObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.PRE_TYPE;

/**
 * 权限验证过滤器
 */
@Component
@Slf4j
public class AccessFilter extends BaseFilter {

    @Autowired
    private CacheChannel cacheChannel;

    @Autowired
    private ResourceApi resourceApi;

    @Override
    public String filterType() {
        return PRE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER + 10;
    }

    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 验证当前用户是否拥有某个URI的访问权限
     */
    @Override
    public Object run() {
        // 不进行拦截的地址
        if (isIgnoreToken()) {
            return null;
        }

        RequestContext requestContext = RequestContext.getCurrentContext();
        String requestURI = requestContext.getRequest().getRequestURI();
        requestURI = StrUtil.subSuf(requestURI, zuulPrefix.length());
        requestURI = StrUtil.subSuf(requestURI, requestURI.indexOf("/", 1));
        String method = requestContext.getRequest().getMethod();
        String permission = method + requestURI;

        //从缓存中获取所有需要进行鉴权的资源
        CacheObject resourceNeed2AuthObject = cacheChannel.get(CacheKey.RESOURCE, CacheKey.RESOURCE_NEED_TO_CHECK);
        List<String> resourceNeed2Auth = (List<String>) resourceNeed2AuthObject.getValue();
        if (resourceNeed2Auth == null) {
            // 缓存未命中，远程拉取全量受保护资源
            R<List> resourceResult = resourceApi.list();
            // fail-closed：结果为空、调用失败或无数据时一律拒绝，禁止在鉴权服务故障时跳过资源检查
            if (resourceResult == null || Boolean.TRUE.equals(resourceResult.getIsError())
                    || resourceResult.getData() == null) {
                log.error("鉴权资源列表获取失败，fail-closed 拒绝访问 {}（result={}）", permission, resourceResult);
                errorResponse(ExceptionCode.UNAUTHORIZED.getMsg(), ExceptionCode.UNAUTHORIZED.getCode(), 200);
                return null;
            }
            resourceNeed2Auth = resourceResult.getData();
            cacheChannel.set(CacheKey.RESOURCE, CacheKey.RESOURCE_NEED_TO_CHECK, resourceNeed2Auth);
        }
        long count = resourceNeed2Auth.stream().filter((String r) -> {
            // 加边界匹配，避免父路径前缀碰撞误授权（如 /ord 误放行 /order）
            return pathMatch(permission, r);
        }).count();
        if (count == 0) {
            //未知请求
            errorResponse(ExceptionCode.UNAUTHORIZED.getMsg(), ExceptionCode.UNAUTHORIZED.getCode(), 200);
            return null;
        }

        String userId = requestContext.getZuulRequestHeaders().get(BaseContextConstants.JWT_KEY_USER_ID);
        if (StrUtil.isEmpty(userId)) {
            errorResponse(ExceptionCode.UNAUTHORIZED.getMsg(), ExceptionCode.UNAUTHORIZED.getCode(), 200);
            return null;
        }

        CacheObject cacheObject = cacheChannel.get(CacheKey.USER_RESOURCE, userId);
        List<String> userResource = (List<String>) cacheObject.getValue();
        // 如果从缓存获取不到当前用户的资源权限，需要查询数据库获取，然后再放入缓存
        if (userResource == null || userResource.isEmpty()) {
            ResourceQueryDTO resourceQueryDTO = new ResourceQueryDTO();
            resourceQueryDTO.setUserId(new Long(userId));
            //通过Feign调用服务，查询当前用户拥有的权限
            R<List<Resource>> result = resourceApi.visible(resourceQueryDTO);
            if (result != null && result.getData() != null) {
                List<Resource> userResourceList = result.getData();
                userResource = userResourceList.stream().map((Resource r) -> {
                    return r.getMethod() + r.getUrl();
                }).collect(Collectors.toList());
                cacheChannel.set(CacheKey.USER_RESOURCE, userId, userResource);
            }
        }

        if (userResource == null || userResource.isEmpty()) {
            log.warn("用户{}没有权限信息，拒绝访问{}", userId, permission);
            errorResponse(ExceptionCode.UNAUTHORIZED.getMsg(), ExceptionCode.UNAUTHORIZED.getCode(), 200);
            return null;
        }

        long count = userResource.stream().filter((String r) -> {
            // 加边界匹配，避免父路径前缀碰撞误授权
            return pathMatch(permission, r);
        }).count();

        if (count > 0) {
            //有访问权限
            return null;
        } else {
            log.warn("用户{}没有访问{}资源的权限", userId, method + requestURI);
            errorResponse(ExceptionCode.UNAUTHORIZED.getMsg(), ExceptionCode.UNAUTHORIZED.getCode(), 200);
        }
        return null;
    }

    /**
     * 资源路径边界匹配：精确相等，或为其直接/多级子路径。
     * 用 r + "/" 作为前缀，避免注册父路径 /ord 时误放行字面相近的 /order，
     * 同时保留「路径变量接口注册为父路径」（如 GET /order-manager/order 匹配 /order-manager/order/123）。
     */
    private static boolean pathMatch(String permission, String resource) {
        return permission.equals(resource) || permission.startsWith(resource + "/");
    }
}
