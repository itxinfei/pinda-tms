package com.itheima.pinda.controller;


import com.itheima.pinda.DTO.AreaDTO;
import com.itheima.pinda.DTO.OrgDTO;
import com.itheima.pinda.DTO.UserDTO;
import com.itheima.pinda.DTO.UserProfileDTO;
import com.itheima.pinda.DTO.user.CourierScopeDto;
import com.itheima.pinda.common.context.RequestContext;
import com.itheima.pinda.common.utils.Result;
import com.itheima.pinda.feign.AreaFeign;
import com.itheima.pinda.feign.OrgFeign;
import com.itheima.pinda.feign.UserFeign;
import com.itheima.pinda.feign.user.CourierScopeFeign;
import com.itheima.pinda.util.Rx;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * 运单表 前端控制器
 * </p>
 *
 * @author diesel
 * @since 2020-03-19
 */
@Slf4j
@Tag(name = "用户管理")
@Controller
@RequestMapping("user")
public class UserController {

    private final UserFeign userFeign;

    private final OrgFeign orgFeign;

    private final CourierScopeFeign courierScopeFeign;

    private final AreaFeign areaFeign;

    public UserController(UserFeign userFeign, OrgFeign orgFeign, CourierScopeFeign courierScopeFeign, AreaFeign areaFeign) {
        this.userFeign = userFeign;
        this.orgFeign = orgFeign;
        this.courierScopeFeign = courierScopeFeign;
        this.areaFeign = areaFeign;
    }

    @SneakyThrows
    @Operation(summary = "我的信息")
    @ResponseBody
    @GetMapping("profile")
    public Result profile() {

        //  快递员id  并放入参数
        String courierId = RequestContext.getUserId();
        // 基本信息
        // 修改点：远程调用可能返回 null（fallback），统一通过 Rx 安全取值，避免 NPE
        UserDTO user = Rx.data(userFeign.get(Long.valueOf(courierId)));
        if (user == null) {
            return Result.error("用户信息不存在");
        }
        // 所属机构
        // 修改点：远程调用可能返回 null（fallback），统一通过 Rx 安全取值，避免 NPE
        OrgDTO org = Rx.data(orgFeign.get(user.getOrgId()));
        //
        // 修改点：Feign 直接返回 List 可能为 null，统一通过 Rx 安全取值
        List<CourierScopeDto> courierScopeDtos = Rx.list(courierScopeFeign.findAllCourierScope(null, user.getId().toString()));
        List<Long> areaIds = courierScopeDtos.stream().map(item -> Long.valueOf(item.getAreaId())).collect(Collectors.toList());
        // 修改点：远程调用结果可能为 null，统一通过 Rx 安全取值
        List<AreaDTO> areas = Rx.dataList(areaFeign.findAll(null, areaIds));
        return Result.ok().put("data", UserProfileDTO.builder()
                .id(user.getId().toString())
                .avatar(user.getAvatar())
                .name(user.getName())
                .phone(user.getMobile())
                .manager(org != null ? org.getName() : "")
                .areas(areas)
                .build());
    }
}
