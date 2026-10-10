package com.itheima.auth.controller.internal;

import com.itheima.auth.service.internal.UserQueryService;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.common.utils.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户内部端点：路径与 UserFeign 对齐。
 */
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserQueryService userQueryService;

    @GetMapping("/{id}")
    public UserDTO get(@PathVariable("id") Long id) {
        return userQueryService.get(id);
    }

    @GetMapping("/list")
    public List<UserDTO> list(@RequestParam(value = "ids", required = false) List<Long> ids,
                              @RequestParam(value = "stationId", required = false) Long stationId,
                              @RequestParam(value = "name", required = false) String name,
                              @RequestParam(value = "orgId", required = false) Long orgId) {
        return userQueryService.list(ids, stationId, name, orgId);
    }

    @GetMapping("/page")
    public PageResponse<UserDTO> page(@RequestParam("page") long page,
                                      @RequestParam("size") long size,
                                      @RequestParam(value = "orgId", required = false) Long orgId,
                                      @RequestParam(value = "stationId", required = false) Long stationId,
                                      @RequestParam(value = "name", required = false) String name,
                                      @RequestParam(value = "account", required = false) String account,
                                      @RequestParam(value = "mobile", required = false) String mobile) {
        return userQueryService.page(page, size, orgId, stationId, name, account, mobile);
    }
}
