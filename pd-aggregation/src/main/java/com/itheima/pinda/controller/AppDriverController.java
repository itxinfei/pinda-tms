package com.itheima.pinda.controller;

import com.itheima.pinda.DTO.AppDriverQueryDTO;
import com.itheima.pinda.DTO.DriverJobDTO;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.utils.PageResponse;
import com.itheima.pinda.service.DriverService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Api(tags = "司机聚合平台")
@RestController
@RequestMapping("appDriver")
public class AppDriverController {

    @Autowired
    private DriverService driverService;

    @PostMapping("/page")
    PageResponse<DriverJobDTO> findByPage(@RequestBody AppDriverQueryDTO dto,
                                          HttpServletResponse response) throws IOException {
        // 身份强制取自网关解析 JWT 后透传的当前登录用户（userid 头），
        // 忽略请求体中的 driverId，防止传入他人 id 水平越权查看他人任务
        String userId = RequestContext.getUserId();
        if (userId == null || userId.trim().isEmpty()) {
            log.warn("[司机任务] 缺少登录身份，拒绝分页查询");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录或身份信息缺失，请通过网关访问\"}");
            return null;
        }
        dto.setDriverId(userId);
        return driverService.findByPage(dto);
    }
}
