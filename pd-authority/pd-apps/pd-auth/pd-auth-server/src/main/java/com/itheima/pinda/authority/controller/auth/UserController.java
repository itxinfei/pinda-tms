package com.itheima.pinda.authority.controller.auth;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.pinda.authority.biz.service.auth.RoleService;
import com.itheima.pinda.authority.biz.service.auth.UserService;
import com.itheima.pinda.authority.biz.service.core.OrgService;
import com.itheima.pinda.authority.biz.service.core.StationService;
import com.itheima.pinda.authority.dto.auth.*;
import com.itheima.pinda.authority.entity.auth.Role;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.authority.entity.core.Station;
import com.itheima.pinda.authority.enumeration.auth.Sex;
import com.itheima.pinda.base.BaseController;
import com.itheima.pinda.base.R;
import com.itheima.pinda.base.entity.SuperEntity;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.dozer.DozerUtils;
import com.itheima.pinda.log.annotation.SysLog;
import com.itheima.pinda.user.feign.UserQuery;
import com.itheima.pinda.user.model.SysOrg;
import com.itheima.pinda.user.model.SysRole;
import com.itheima.pinda.user.model.SysStation;
import com.itheima.pinda.user.model.SysUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 前端控制器
 * 用户
 * </p>
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/user")
@Api(value = "User", tags = "用户")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private OrgService orgService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private StationService stationService;
    @Autowired
    private DozerUtils dozer;

    /**
     * 分页查询用户
     *
     * @param userPage 分页查询对象
     * @return 查询结果
     */
    @ApiOperation(value = "分页查询用户", notes = "分页查询用户")
    @ApiImplicitParams({@ApiImplicitParam(name = "current", value = "页码", dataType = "long", paramType = "query", defaultValue = "1"), @ApiImplicitParam(name = "size", value = "分页条数", dataType = "long", paramType = "query", defaultValue = "10"),})
    @GetMapping("/page")
    @SysLog("分页查询用户")
    public R<Page<User>> page(UserPageDTO userPage) {
        Page<User> page = getPage();
        User user = dozer.map2(userPage, User.class);
        if (userPage.getOrgId() != null && userPage.getOrgId() >= 0) {
            user.setOrgId(null);
        }
        LbqWrapper<User> wrapper = Wraps.lbQ(user);
        if (userPage.getOrgId() != null && userPage.getOrgId() >= 0) {
            List<Org> children = orgService.findChildren(Arrays.asList(userPage.getOrgId()));
            wrapper.in(User::getOrgId, children.stream().mapToLong(Org::getId).boxed().collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(userPage.getName())) {
            wrapper.like(User::getName, userPage.getName());
        }
        if (StringUtils.isNotEmpty(userPage.getAccount())) {
            wrapper.like(User::getAccount, userPage.getAccount());
        }
        if (StringUtils.isNotEmpty(userPage.getEmail())) {
            wrapper.like(User::getEmail, userPage.getEmail());
        }
        if (StringUtils.isNotEmpty(userPage.getMobile())) {
            wrapper.like(User::getMobile, userPage.getMobile());
        }
        if (userPage.getSex() != null) {
            wrapper.eq(User::getSex, userPage.getSex());
        }
        if (userPage.getStatus() != null) {
            wrapper.eq(User::getStatus, userPage.getStatus());
        }
        if (userPage.getStartCreateTime() != null) {
            wrapper.geHeader(User::getCreateTime, userPage.getStartCreateTime());
        }
        if (userPage.getEndCreateTime() != null) {
            wrapper.leFooter(User::getCreateTime, userPage.getEndCreateTime());
        }
        if (userPage.getStationId() != null) {
            wrapper.eq(User::getStationId, userPage.getStationId());
        }
        wrapper.orderByDesc(User::getId);
        userService.findPage(page, wrapper);
        List<User> list = page.getRecords().stream().map(item -> {
            Org org = orgService.getById(item.getOrgId());
            if (org != null) {
                item.setOrgName(org.getName());
            } else {
                item.setOrgName("");
            }
            Station station = stationService.getById(item.getStationId());
            if (station != null) {
                item.setStationName(station.getName());
            } else {
                item.setStationName("");
            }
            return item;
        }).collect(Collectors.toList());
        page.setRecords(list);
        return success(page);
    }

    @ApiOperation(value = "根据条件获取用户列表", notes = "根据条件获取用户列表")
    @ApiImplicitParams({@ApiImplicitParam(name = "ids", value = "id列表", paramType = "query", defaultValue = "[1,2]"), @ApiImplicitParam(name = "stationId", value = "岗位id", dataType = "long", paramType = "query", defaultValue = "1"), @ApiImplicitParam(name = "name", value = "用户名称", dataType = "string", paramType = "query", defaultValue = "张三"), @ApiImplicitParam(name = "orgId", value = "组织id", dataType = "long", paramType = "query", defaultValue = "1"),})
    @GetMapping("")
    @SysLog("根据条件获取用户列表")
    public R<List<User>> list(@RequestParam(name = "ids", required = false) List<Long> ids, @RequestParam(name = "stationId", required = false) Long stationId, @RequestParam(name = "name", required = false) String name, @RequestParam(name = "orgId", required = false) Long orgId) {
        log.info("查询用户集合 ids:{} stationId:{} name:{} orgId:{}", ids, stationId, name, orgId);
        LbqWrapper<User> wrapper = Wraps.lbQ();
        if (ids != null && ids.size() > 0) {
            wrapper.in(User::getId, ids);
        }
        if (stationId != null && stationId != 0) {
            wrapper.eq(User::getStationId, stationId);
        }
        if (StringUtils.isNotEmpty(name)) {
            wrapper.like(User::getName, name);
        }
        if (orgId != null && orgId != 0) {
            wrapper.eq(User::getOrgId, orgId);
        }
        return R.success(userService.list(wrapper));
    }

    /**
     * 查询用户
     *
     * @param id 主键id
     * @return 查询结果
     */
    @ApiOperation(value = "查询用户", notes = "查询用户")
    @GetMapping("/{id}")
    @SysLog("查询用户")
    public R<User> get(@PathVariable Long id) {
        return success(userService.getById(id));
    }


    @ApiOperation(value = "查询所有用户", notes = "查询所有用户")
    @GetMapping("/find")
    @SysLog("查询所有用户")
    public R<List<Long>> findAllUserId() {
        return success(userService.list().stream().mapToLong(User::getId).boxed().collect(Collectors.toList()));
    }

    /**
     * 新增用户
     *
     * @param data 新增对象
     * @return 新增结果
     */
    @ApiOperation(value = "新增用户", notes = "新增用户不为空的字段")
    @PostMapping
    @SysLog("新增用户")
    public R<User> save(@RequestBody @Validated UserSaveDTO data) {
        User user = dozer.map(data, User.class);
        userService.saveUser(user);
        return success(user);
    }


    /**
     * 修改用户
     *
     * @param data 修改对象
     * @return 修改结果
     */
    @ApiOperation(value = "修改用户", notes = "修改用户不为空的字段")
    @PutMapping
    @SysLog("修改用户")
    public R<User> update(@RequestBody @Validated(SuperEntity.Update.class) UserUpdateDTO data) {
        User user = dozer.map(data, User.class);
        userService.updateUser(user);
        return success(user);
    }

    @ApiOperation(value = "修改头像", notes = "修改头像")
    @PutMapping("/avatar")
    @SysLog("修改头像")
    public R<User> avatar(@RequestBody @Validated(SuperEntity.Update.class) UserUpdateAvatarDTO data) {
        User user = dozer.map(data, User.class);
        userService.updateUser(user);
        return success(user);
    }

    @ApiOperation(value = "修改密码", notes = "修改密码")
    @PutMapping("/password")
    @SysLog("修改密码")
    public R<Boolean> updatePassword(@RequestBody UserUpdatePasswordDTO data) {
        return success(userService.updatePassword(data));
    }

    @ApiOperation(value = "重置密码", notes = "重置密码")
    @GetMapping("/reset")
    @SysLog("重置密码")
    public R<Boolean> resetTx(@RequestParam("ids[]") List<Long> ids) {
        userService.reset(ids);
        return success();
    }

    /**
     * 删除用户
     *
     * @param ids 主键id
     * @return 删除结果
     */
    @ApiOperation(value = "删除用户", notes = "根据id物理删除用户")
    @DeleteMapping
    @SysLog("删除用户")
    public R<Boolean> delete(@RequestParam("ids[]") List<Long> ids) {
        userService.remove(ids);
        return success(true);
    }


    /**
     * 单体查询用户
     *
     * @param id 主键id
     * @return 查询结果
     */
    @ApiOperation(value = "查询用户详细", notes = "查询用户详细")
    @PostMapping(value = "/anno/id/{id}")
    public R<SysUser> getById(@PathVariable Long id, @RequestBody UserQuery query) {
        User user = userService.getById(id);
        if (user == null) {
            return success(null);
        }
        SysUser sysUser = dozer.map(user, SysUser.class);

        if (query.getFull() || query.getOrg()) {
            sysUser.setOrg(dozer.map(orgService.getById(user.getOrgId()), SysOrg.class));
        }
        if (query.getFull() || query.getStation()) {
            sysUser.setStation(dozer.map(stationService.getById(user.getStationId()), SysStation.class));
        }

        if (query.getFull() || query.getRoles()) {
            List<Role> list = roleService.findRoleByUserId(id);
            sysUser.setRoles(dozer.mapList(list, SysRole.class));
        }

        return success(sysUser);
    }

    /**
     * 根据用户id，查询用户权限范围
     *
     * @param id 用户id
     * @return
     */
    @ApiOperation(value = "查询用户权限范围", notes = "根据用户id，查询用户权限范围")
    @GetMapping(value = "/ds/{id}")
    public Map<String, Object> getDataScopeById(@PathVariable("id") Long id) {
        return userService.getDataScopeById(id);
    }

    /**
     * 查询角色的已关联用户
     *
     * @param roleId  角色id
     * @param keyword 账号或名称
     * @return
     */
    @ApiOperation(value = "查询角色的已关联用户", notes = "查询角色的已关联用户")
    @GetMapping(value = "/role/{roleId}")
    public R<UserRoleDTO> findUserByRoleId(@PathVariable("roleId") Long roleId, @RequestParam(value = "keyword", required = false) String keyword) {
        List<User> list = userService.findUserByRoleId(roleId, keyword);
        List<Long> idList = list.stream().mapToLong(User::getId).boxed().collect(Collectors.toList());
        return success(UserRoleDTO.builder().idList(idList).userList(list).build());
    }
}
