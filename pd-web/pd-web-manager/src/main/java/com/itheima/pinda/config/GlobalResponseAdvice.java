package com.itheima.pinda.config;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 管理端 BFF（pd-web-manager）统一响应壳。
 *
 * 背景：本服务大量读接口直接返回 PageResponse / VO / DTO / List（响应体无 code 字段），
 * 而管理端 request 拦截器约定 code===0 才算成功并取 data，裸返回会被误判
 * reject("系统出错")，表现为管理端列表页全部报错。此 Advice 在出口统一包成
 * {"code":0,"msg":"success","data":...}，无需逐个修改 47 个读方法签名；
 * 已自带 code 壳的 Result 原样透传，不做二次包装。
 *
 * 边界：
 * 1. basePackages 限定 com.itheima.pinda，不影响 springdoc / actuator 等第三方端点；
 * 2. String / byte[] / Resource 走非 JSON 转换器，包装成 Map 会引发 ClassCastException，
 *    supports 中直接跳过（当前服务无此类端点，纯防御）；
 * 3. 不使用 Result.put 构造壳——其重写的 put 每次 log.info，列表接口会刷噪音日志。
 */
@RestControllerAdvice(basePackages = "com.itheima.pinda")
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return !StringHttpMessageConverter.class.isAssignableFrom(converterType)
                && !ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType)
                && !ResourceHttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        // 已有统一响应壳（Result 等含 code 的 Map）原样返回，避免二次包装
        if (body instanceof Map<?, ?> map && map.containsKey("code")) {
            return body;
        }
        Map<String, Object> wrapper = new LinkedHashMap<>(4);
        wrapper.put("code", 0);
        wrapper.put("msg", "success");
        wrapper.put("data", body);
        return wrapper;
    }
}
