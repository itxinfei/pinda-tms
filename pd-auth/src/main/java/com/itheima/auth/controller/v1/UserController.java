package com.itheima.auth.controller.v1;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.itheima.auth.common.v1.ApiResult;
import com.itheima.auth.common.v1.ExcelResult;
import com.itheima.auth.common.v1.OptionVO;
import com.itheima.auth.common.v1.PageResult;
import com.itheima.auth.common.v1.UserExcelRow;
import com.itheima.auth.common.v1.UserForm;
import com.itheima.auth.common.v1.UserVO;
import com.itheima.auth.config.StpInterfaceImpl;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.mapper.AuthUserMapper;
import com.itheima.auth.service.v1.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理（youlai 契约 /api/v1/users）。
 * 个人中心 /me、profile、手机邮箱绑定在 P1 补充；Excel 导入导出单独步骤。
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final StpInterfaceImpl stpInterface;
    private final AuthUserMapper userMapper;

    /**
     * 当前登录用户信息（前端守卫/按钮权限数据源）。仅需登录，不要求 sys:user 权限。
     * 返回 {userId,username,nickname,avatar,canSwitchTenant,roles,perms}。
     */
    @GetMapping("/me")
    public ApiResult<Map<String, Object>> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        AuthUser u = userMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", String.valueOf(userId));
        if (u != null) {
            data.put("username", u.getAccount());
            data.put("nickname", u.getName());
            data.put("avatar", u.getAvatar());
        }
        data.put("canSwitchTenant", false);
        data.put("roles", stpInterface.getRoleList(userId, null));
        data.put("perms", stpInterface.getPermissionList(userId, null));
        return ApiResult.success(data);
    }

    /** 分页（pageNum/pageSize/keywords/status/deptId），返回 {list,total} */
    @GetMapping
    @SaCheckPermission("sys:user:list")
    public ApiResult<PageResult<UserVO>> page(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "deptId", required = false) Long deptId) {
        return ApiResult.success(userService.page(pageNum, pageSize, keywords, status, deptId));
    }

    @GetMapping("/{id}/form")
    @SaCheckPermission("sys:user:list")
    public ApiResult<UserForm> form(@PathVariable Long id) {
        return ApiResult.success(userService.formData(id));
    }

    @PostMapping
    @SaCheckPermission("sys:user:add")
    public ApiResult<Long> create(@RequestBody UserForm form) {
        return ApiResult.success(userService.create(form));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("sys:user:edit")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody UserForm form) {
        userService.update(id, form);
        return ApiResult.success();
    }

    /** 重置密码（body: {password}；空则默认 123456） */
    @PutMapping("/{id}/password/reset")
    @SaCheckPermission("sys:user:resetPwd")
    public ApiResult<Void> resetPassword(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String pwd = body == null ? null : body.get("password");
        userService.resetPassword(id, pwd);
        return ApiResult.success();
    }

    /** 启停（前端开关） */
    @PutMapping("/{id}/status")
    @SaCheckPermission("sys:user:edit")
    public ApiResult<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.changeStatus(id, status);
        return ApiResult.success();
    }

    /** 批量删除（逗号分隔） */
    @DeleteMapping("/{ids}")
    @SaCheckPermission("sys:user:delete")
    public ApiResult<Void> delete(@PathVariable String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim).filter(s -> !s.isEmpty())
                .map(Long::valueOf).toList();
        userService.deleteByIds(idList);
        return ApiResult.success();
    }

    /** 用户简要选项（P1 占位，先返回空选项避免前端报错） */
    @GetMapping("/options")
    @SaCheckPermission("sys:user:list")
    public ApiResult<List<OptionVO>> options() {
        return ApiResult.success(List.of());
    }

    // ============ Excel 导入导出/模板（SYS#4）============

    @GetMapping("/template")
    @SaCheckPermission("sys:user:import")
    public void template(HttpServletResponse response) throws IOException {
        writeExcel(response, "用户导入模板", List.of());
    }

    @GetMapping("/export")
    @SaCheckPermission("sys:user:export")
    public void export(HttpServletResponse response,
                       @RequestParam(value = "keywords", required = false) String keywords,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "deptId", required = false) Long deptId) throws IOException {
        writeExcel(response, "用户列表", userService.exportRows(keywords, status, deptId));
    }

    @PostMapping("/import")
    @SaCheckPermission("sys:user:import")
    public ApiResult<ExcelResult> importUsers(@RequestParam("file") MultipartFile file) throws IOException {
        List<UserExcelRow> rows = EasyExcel.read(file.getInputStream())
                .head(UserExcelRow.class).sheet().doReadSync();
        return ApiResult.success(userService.importRows(rows));
    }

    private void writeExcel(HttpServletResponse response, String fileName, List<UserExcelRow> rows) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String name = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + name + ".xlsx");
        EasyExcel.write(response.getOutputStream(), UserExcelRow.class).sheet("用户").doWrite(rows);
    }

    // ============ 个人中心（仅登录，不要求 sys:user 权限）============

    @GetMapping("/profile")
    public ApiResult<UserVO> profile() {
        return ApiResult.success(userService.profile(StpUtil.getLoginIdAsLong()));
    }

    @PutMapping("/profile")
    public ApiResult<Void> updateProfile(@RequestBody Map<String, Object> body) {
        Long uid = StpUtil.getLoginIdAsLong();
        userService.updateProfile(uid,
                str(body.get("nickname")),
                str(body.get("avatar")),
                intOrNull(body.get("gender")));
        return ApiResult.success();
    }

    @PutMapping("/password")
    public ApiResult<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(StpUtil.getLoginIdAsLong(),
                body.get("oldPassword"), body.get("newPassword"));
        return ApiResult.success();
    }

    @PostMapping("/mobile/code")
    public ApiResult<Void> sendMobileCode(@RequestParam String mobile) {
        userService.sendMobileCode(mobile);
        return ApiResult.success();
    }

    @PutMapping("/mobile")
    public ApiResult<Void> bindMobile(@RequestBody Map<String, String> body) {
        userService.bindMobile(StpUtil.getLoginIdAsLong(), body.get("mobile"), body.get("code"));
        return ApiResult.success();
    }

    @DeleteMapping("/mobile")
    public ApiResult<Void> unbindMobile(@RequestBody Map<String, String> body) {
        userService.unbindContact(StpUtil.getLoginIdAsLong(), body.get("password"), true);
        return ApiResult.success();
    }

    @PostMapping("/email/code")
    public ApiResult<Void> sendEmailCode(@RequestParam String email) {
        userService.sendEmailCode(email);
        return ApiResult.success();
    }

    @PutMapping("/email")
    public ApiResult<Void> bindEmail(@RequestBody Map<String, String> body) {
        userService.bindEmail(StpUtil.getLoginIdAsLong(), body.get("email"), body.get("code"));
        return ApiResult.success();
    }

    @DeleteMapping("/email")
    public ApiResult<Void> unbindEmail(@RequestBody Map<String, String> body) {
        userService.unbindContact(StpUtil.getLoginIdAsLong(), body.get("password"), false);
        return ApiResult.success();
    }

    private String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private Integer intOrNull(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
