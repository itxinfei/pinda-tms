package com.itheima.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.auth.common.BizException;
import com.itheima.auth.common.v1.PageResult;
import com.itheima.auth.common.v1.UserExcelRow;
import com.itheima.auth.common.v1.UserForm;
import com.itheima.auth.common.v1.UserVO;
import com.itheima.auth.config.StpInterfaceImpl;
import com.itheima.auth.entity.AuthUser;
import com.itheima.auth.entity.AuthUserRole;
import com.itheima.auth.entity.CoreOrg;
import com.itheima.auth.entity.CoreStation;
import com.itheima.auth.entity.Role;
import com.itheima.auth.mapper.AuthUserMapper;
import com.itheima.auth.mapper.AuthUserRoleMapper;
import com.itheima.auth.mapper.CoreOrgMapper;
import com.itheima.auth.mapper.CoreStationMapper;
import com.itheima.auth.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端用户管理（youlai 契约，sys_user / sys_user_role）。
 *
 * <p>字段适配：前端 username/nickname/gender/deptId ↔ 实体 account/name/sex/orgId。
 * 多租户、逻辑删除、审计由插件/处理器统一处理。</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthUserMapper userMapper;
    private final AuthUserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final CoreOrgMapper orgMapper;
    private final CoreStationMapper stationMapper;
    private final StpInterfaceImpl stpInterface;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 默认初始密码（新建用户未指定密码时） */
    private static final String DEFAULT_PASSWORD = "123456";

    public PageResult<UserVO> page(long pageNum, long pageSize, String keywords,
                                   Integer status, Long deptId) {
        IPage<AuthUser> result = userMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<AuthUser>lambdaQuery()
                        .eq(status != null, AuthUser::getStatus, status)
                        .eq(deptId != null, AuthUser::getOrgId, deptId)
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(AuthUser::getAccount, keywords)
                                .or().like(AuthUser::getName, keywords)
                                .or().like(AuthUser::getMobile, keywords))
                        .orderByDesc(AuthUser::getId));
        List<UserVO> list = enrich(result.getRecords());
        return new PageResult<>(list, result.getTotal());
    }

    /** 表单回显（含角色/机构/岗位） */
    public UserForm formData(Long id) {
        AuthUser u = mustGet(id);
        UserForm f = new UserForm();
        f.setId(String.valueOf(u.getId()));
        f.setUsername(u.getAccount());
        f.setNickname(u.getName());
        f.setAvatar(u.getAvatar());
        f.setGender(u.getSex());
        f.setMobile(u.getMobile());
        f.setEmail(u.getEmail());
        f.setStatus(u.getStatus());
        f.setDeptId(u.getOrgId() == null ? null : String.valueOf(u.getOrgId()));
        f.setStationId(u.getStationId() == null ? null : String.valueOf(u.getStationId()));
        f.setRoleIds(roleIdsOf(id));
        return f;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(UserForm form) {
        if (!StringUtils.hasText(form.getUsername())) {
            throw BizException.validFail("登录账号不能为空");
        }
        if (!StringUtils.hasText(form.getNickname())) {
            throw BizException.validFail("姓名不能为空");
        }
        Long tenantScoped = countByAccount(form.getUsername(), null);
        if (tenantScoped > 0) {
            throw BizException.validFail("登录账号已存在：" + form.getUsername());
        }
        AuthUser u = new AuthUser();
        u.setAccount(form.getUsername().trim());
        u.setName(form.getNickname().trim());
        u.setAvatar(form.getAvatar());
        u.setSex(form.getGender());
        u.setMobile(form.getMobile());
        u.setEmail(form.getEmail());
        u.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        u.setOrgId(parseLong(form.getDeptId()));
        u.setStationId(parseLong(form.getStationId()));
        u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userMapper.insert(u);
        replaceRoles(u.getId(), form.getRoleIds());
        return u.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserForm form) {
        AuthUser exist = mustGet(id);
        if (StringUtils.hasText(form.getUsername()) && !form.getUsername().trim().equals(exist.getAccount())) {
            if (countByAccount(form.getUsername().trim(), id) > 0) {
                throw BizException.validFail("登录账号已存在：" + form.getUsername());
            }
            exist.setAccount(form.getUsername().trim());
        }
        if (StringUtils.hasText(form.getNickname())) {
            exist.setName(form.getNickname().trim());
        }
        exist.setAvatar(form.getAvatar());
        exist.setSex(form.getGender());
        exist.setMobile(form.getMobile());
        exist.setEmail(form.getEmail());
        if (form.getStatus() != null) {
            exist.setStatus(form.getStatus());
        }
        exist.setOrgId(parseLong(form.getDeptId()));
        exist.setStationId(parseLong(form.getStationId()));
        // 密码不在此修改；账号唯一性校验在上方
        userMapper.updateById(exist);
        if (form.getRoleIds() != null) {
            guardSuperAdmin(id, form.getStatus() == null ? exist.getStatus() : form.getStatus(),
                    form.getRoleIds());
            replaceRoles(id, form.getRoleIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        mustGet(id);
        String pwd = StringUtils.hasText(newPassword) ? newPassword.trim() : DEFAULT_PASSWORD;
        if (pwd.length() < 6) {
            throw BizException.validFail("新密码长度不能少于6位");
        }
        AuthUser u = new AuthUser();
        u.setId(id);
        u.setPassword(passwordEncoder.encode(pwd));
        userMapper.updateById(u);
    }

    /** 启停 */
    public void changeStatus(Long id, Integer status) {
        AuthUser u = mustGet(id);
        if (status == null || (status != 0 && status != 1)) {
            throw BizException.validFail("状态值非法");
        }
        guardSuperAdmin(id, status, roleIdsOf(id));
        u.setStatus(status);
        userMapper.updateById(u);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            if (stpInterface.isSuperAdmin(id)) {
                throw BizException.validFail("内置超级管理员账号不可删除");
            }
            userRoleMapper.delete(Wrappers.<AuthUserRole>lambdaQuery()
                    .eq(AuthUserRole::getUserId, id));
            userMapper.deleteById(id);
        }
    }

    // ============ Excel 导入导出（SYS#4）============

    /** 导出当前查询条件下的用户为 EasyExcel 行（写 HttpServletResponse 由 controller 负责） */
    public List<UserExcelRow> exportRows(String keywords, Integer status, Long deptId) {
        // 大结果集上限保护（管理端导出，默认 1 万）
        IPage<AuthUser> result = userMapper.selectPage(new Page<>(1, 10000),
                Wrappers.<AuthUser>lambdaQuery()
                        .eq(status != null, AuthUser::getStatus, status)
                        .eq(deptId != null, AuthUser::getOrgId, deptId)
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(AuthUser::getAccount, keywords)
                                .or().like(AuthUser::getName, keywords)
                                .or().like(AuthUser::getMobile, keywords))
                        .orderByDesc(AuthUser::getId));
        List<AuthUser> users = result.getRecords();
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> orgIds = users.stream().map(AuthUser::getOrgId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> stationIds = users.stream().map(AuthUser::getStationId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> orgNames = orgIds.isEmpty() ? Collections.emptyMap()
                : orgMapper.selectBatchIds(orgIds).stream().collect(Collectors.toMap(CoreOrg::getId, CoreOrg::getName, (a, b) -> a));
        Map<Long, String> stationNames = stationIds.isEmpty() ? Collections.emptyMap()
                : stationMapper.selectBatchIds(stationIds).stream().collect(Collectors.toMap(CoreStation::getId, CoreStation::getName, (a, b) -> a));
        Set<Long> userIds = users.stream().map(AuthUser::getId).collect(Collectors.toSet());
        Map<Long, List<String>> roleNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<AuthUserRole> links = userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                    .in(AuthUserRole::getUserId, userIds));
            Set<Long> roleIds = links.stream().map(AuthUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, String> rn = roleIds.isEmpty() ? Collections.emptyMap()
                    : roleMapper.selectBatchIds(roleIds).stream().collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));
            for (AuthUserRole l : links) {
                String n = rn.get(l.getRoleId());
                if (n != null) {
                    roleNameMap.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(n);
                }
            }
        }
        List<UserExcelRow> rows = new ArrayList<>();
        for (AuthUser u : users) {
            UserExcelRow r = new UserExcelRow();
            r.setUsername(u.getAccount());
            r.setNickname(u.getName());
            r.setGender(u.getSex());
            r.setMobile(u.getMobile());
            r.setEmail(u.getEmail());
            r.setStatus(u.getStatus());
            r.setDeptName(u.getOrgId() == null ? null : orgNames.get(u.getOrgId()));
            r.setStationName(u.getStationId() == null ? null : stationNames.get(u.getStationId()));
            List<String> rn = roleNameMap.get(u.getId());
            r.setRoleNames(rn == null ? null : String.join(",", rn));
            rows.add(r);
        }
        return rows;
    }

    /** 解析导入行并落库：按名称匹配机构/岗位/角色，逐行收集错误，整批无致命错误才提交 */
    @Transactional(rollbackFor = Exception.class)
    public com.itheima.auth.common.v1.ExcelResult importRows(List<UserExcelRow> rows) {
        int valid = 0;
        List<String> errors = new ArrayList<>();
        List<AuthUser> toInsert = new ArrayList<>();
        List<List<Long>> roleIdsPerRow = new ArrayList<>();

        // 名称→ID 映射（当前租户）
        Map<String, Long> orgByName = orgMapper.selectList(null).stream()
                .collect(Collectors.toMap(o -> o.getName().trim(), CoreOrg::getId, (a, b) -> a));
        Map<String, Long> stationByName = stationMapper.selectList(null).stream()
                .collect(Collectors.toMap(s -> s.getName().trim(), CoreStation::getId, (a, b) -> a));
        Map<String, Long> roleByName = roleMapper.selectList(Wrappers.<Role>lambdaQuery().eq(Role::getStatus, 1))
                .stream().collect(Collectors.toMap(r -> r.getName().trim(), Role::getId, (a, b) -> a));

        int rowNo = 1;
        for (UserExcelRow r : rows) {
            rowNo++;
            if (r == null || (!StringUtils.hasText(r.getUsername()) && !StringUtils.hasText(r.getNickname()))) {
                continue; // 空行跳过
            }
            String account = StringUtils.trimWhitespace(r.getUsername());
            String name = StringUtils.trimWhitespace(r.getNickname());
            if (!StringUtils.hasText(account)) {
                errors.add("第" + rowNo + "行：登录账号为空");
            }
            if (!StringUtils.hasText(name)) {
                errors.add("第" + rowNo + "行：姓名为空");
            }
            if (StringUtils.hasText(account) && countByAccount(account, null) > 0) {
                errors.add("第" + rowNo + "行：账号已存在 " + account);
            }
            Long orgId = matchByName(orgByName, r.getDeptName(), "机构", rowNo, errors);
            Long stationId = matchByName(stationByName, r.getStationName(), "岗位", rowNo, errors);
            List<Long> roleIds = new ArrayList<>();
            if (StringUtils.hasText(r.getRoleNames())) {
                for (String rn : r.getRoleNames().split(",")) {
                    Long rid = matchByName(roleByName, rn, "角色", rowNo, errors);
                    if (rid != null) {
                        roleIds.add(rid);
                    }
                }
            }
            final int currentRow = rowNo;
            if (errors.stream().anyMatch(e -> e.startsWith("第" + currentRow + "行"))) {
                continue; // 本行有错不计入
            }
            AuthUser u = new AuthUser();
            u.setAccount(account);
            u.setName(name);
            u.setSex(r.getGender());
            u.setMobile(StringUtils.trimWhitespace(r.getMobile()));
            u.setEmail(StringUtils.trimWhitespace(r.getEmail()));
            u.setStatus(r.getStatus() == null ? 1 : r.getStatus());
            u.setOrgId(orgId);
            u.setStationId(stationId);
            u.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
            toInsert.add(u);
            roleIdsPerRow.add(roleIds);
            valid++;
        }

        // 有任意逐行错误：整体回滚不落库，返回错误清单（前端按 invalidCount 提示）
        if (!errors.isEmpty()) {
            throw new ImportRowsException(com.itheima.auth.common.v1.ExcelResult.of(valid,
                    errors.size(), errors));
        }
        for (int i = 0; i < toInsert.size(); i++) {
            userMapper.insert(toInsert.get(i));
            replaceRoles(toInsert.get(i).getId(), roleIdsPerRow.get(i));
        }
        return com.itheima.auth.common.v1.ExcelResult.of(valid, 0, errors);
    }

    private Long matchByName(Map<String, Long> map, String name, String label, int rowNo, List<String> errors) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        Long id = map.get(StringUtils.trimWhitespace(name));
        if (id == null) {
            errors.add("第" + rowNo + "行：" + label + "名称不存在 [" + name + "]");
        }
        return id;
    }

    /** 携带已统计结果的导入异常（供 controller 返回结构化结果而非 500） */
    public static class ImportRowsException extends BizException {
        private final transient com.itheima.auth.common.v1.ExcelResult result;
        public ImportRowsException(com.itheima.auth.common.v1.ExcelResult result) {
            super("导入存在无效数据");
            this.result = result;
        }
        public com.itheima.auth.common.v1.ExcelResult getResult() {
            return result;
        }
    }

    // ============ 个人中心（当前登录用户，SYS#5）============

    /** dev 固定验证码（不接真实短信/邮件通道，仅日志输出）；上线前替换为短信/邮件服务 */
    public static final String DEV_VERIFY_CODE = "1234";

    /** 当前登录用户资料（对齐前端 UserProfileDetail） */
    public UserVO profile(Long userId) {
        AuthUser u = mustGet(userId);
        List<UserVO> one = enrich(List.of(u));
        return one.isEmpty() ? null : one.get(0);
    }

    /** 仅允许本人修改昵称/头像/性别等非敏感资料 */
    public void updateProfile(Long userId, String nickname, String avatar, Integer gender) {
        AuthUser u = mustGet(userId);
        if (StringUtils.hasText(nickname)) {
            u.setName(nickname.trim());
        }
        u.setAvatar(avatar);
        if (gender != null) {
            u.setSex(gender);
        }
        userMapper.updateById(u);
    }

    /** 修改自己的密码：校验旧密码 */
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        if (!StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw BizException.validFail("原密码与新密码不能为空");
        }
        if (newPassword.length() < 6) {
            throw BizException.validFail("新密码长度不能少于6位");
        }
        AuthUser u = mustGet(userId);
        if (!passwordEncoder.matches(oldPassword, u.getPassword())) {
            throw BizException.validFail("原密码不正确");
        }
        if (passwordEncoder.matches(newPassword, u.getPassword())) {
            throw BizException.validFail("新密码不能与原密码相同");
        }
        AuthUser up = new AuthUser();
        up.setId(userId);
        up.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(up);
    }

    /** 发送手机验证码（dev 固定码 + 日志） */
    public String sendMobileCode(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            throw BizException.validFail("手机号不能为空");
        }
        // 唯一占用校验：不能绑定到其他账号
        Long count = userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getMobile, mobile));
        if (count != null && count > 0) {
            throw BizException.validFail("该手机号已被其他账号绑定");
        }
        logCode("手机", mobile);
        return DEV_VERIFY_CODE;
    }

    /** 发送邮箱验证码（dev 固定码 + 日志） */
    public String sendEmailCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw BizException.validFail("邮箱不能为空");
        }
        Long count = userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getEmail, email));
        if (count != null && count > 0) {
            throw BizException.validFail("该邮箱已被其他账号绑定");
        }
        logCode("邮箱", email);
        return DEV_VERIFY_CODE;
    }

    /** 绑定/更换手机：校验验证码（dev 固定码） */
    public void bindMobile(Long userId, String mobile, String code) {
        verifyCode(code);
        if (!StringUtils.hasText(mobile)) {
            throw BizException.validFail("手机号不能为空");
        }
        Long other = userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getMobile, mobile).ne(AuthUser::getId, userId));
        if (other != null && other > 0) {
            throw BizException.validFail("该手机号已被其他账号绑定");
        }
        AuthUser up = new AuthUser();
        up.setId(userId);
        up.setMobile(mobile);
        userMapper.updateById(up);
    }

    /** 绑定/更换邮箱 */
    public void bindEmail(Long userId, String email, String code) {
        verifyCode(code);
        if (!StringUtils.hasText(email)) {
            throw BizException.validFail("邮箱不能为空");
        }
        Long other = userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getEmail, email).ne(AuthUser::getId, userId));
        if (other != null && other > 0) {
            throw BizException.validFail("该邮箱已被其他账号绑定");
        }
        AuthUser up = new AuthUser();
        up.setId(userId);
        up.setEmail(email);
        userMapper.updateById(up);
    }

    /** 解绑（手机/邮箱）：需验证当前密码 */
    public void unbindContact(Long userId, String password, boolean mobile) {
        AuthUser u = mustGet(userId);
        if (!StringUtils.hasText(password) || !passwordEncoder.matches(password, u.getPassword())) {
            throw BizException.validFail("密码验证失败，无法解绑");
        }
        AuthUser up = new AuthUser();
        up.setId(userId);
        if (mobile) {
            up.setMobile(null);
        } else {
            up.setEmail(null);
        }
        // updateById 对 null 字段默认不更新，改用显式 set
        userMapper.update(null, Wrappers.<AuthUser>lambdaUpdate()
                .eq(AuthUser::getId, userId)
                .set(mobile, AuthUser::getMobile, null)
                .set(!mobile, AuthUser::getEmail, null));
    }

    private void verifyCode(String code) {
        if (!DEV_VERIFY_CODE.equals(StringUtils.trimWhitespace(code))) {
            throw BizException.validFail("验证码不正确（dev 环境固定为 " + DEV_VERIFY_CODE + "）");
        }
    }

    private void logCode(String channel, String target) {
        // dev：不接真实通道，仅记录；验证码固定 DEV_VERIFY_CODE
        org.slf4j.LoggerFactory.getLogger(UserService.class)
                .info("[DEV验证码] {} 绑定目标={} 验证码={}", channel, target, DEV_VERIFY_CODE);
    }

    // ============ 内部辅助 ============

    private AuthUser mustGet(Long id) {
        AuthUser u = userMapper.selectById(id);
        if (u == null) {
            throw BizException.validFail("用户不存在");
        }
        return u;
    }

    private Long countByAccount(String account, Long excludeId) {
        return userMapper.selectCount(Wrappers.<AuthUser>lambdaQuery()
                .eq(AuthUser::getAccount, account)
                .ne(excludeId != null, AuthUser::getId, excludeId));
    }

    private List<Long> roleIdsOf(Long userId) {
        return userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                        .eq(AuthUserRole::getUserId, userId)).stream()
                .map(AuthUserRole::getRoleId).distinct().toList();
    }

    /** 全量重写用户-角色绑定（uk_user_role 去重） */
    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(Wrappers.<AuthUserRole>lambdaQuery()
                .eq(AuthUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        // 仅保留存在且启用的角色，去重
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                .in(Role::getId, roleIds).eq(Role::getStatus, 1));
        for (Role r : roles) {
            AuthUserRole link = new AuthUserRole();
            link.setUserId(userId);
            link.setRoleId(r.getId());
            // tenant_id 由租户插件在 insert 时填充（实体声明 tenantId、NOT NULL）
            userRoleMapper.insert(link);
        }
    }

    /** 禁止把超管停用或移除 ADMIN 角色（锁死最高权限防护） */
    private void guardSuperAdmin(Long userId, Integer newStatus, List<Long> newRoleIds) {
        if (stpInterface.isSuperAdmin(userId)) {
            if (newStatus != null && newStatus == 0) {
                throw BizException.validFail("内置超级管理员账号不可停用");
            }
            if (newRoleIds != null) {
                List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery()
                        .in(Role::getId, newRoleIds));
                boolean stillAdmin = roles.stream().anyMatch(r -> StpInterfaceImpl.SUPER_ROLE.equalsIgnoreCase(r.getCode()));
                if (!stillAdmin) {
                    throw BizException.validFail("不可移除超级管理员的 ADMIN 角色");
                }
            }
        }
    }

    private List<UserVO> enrich(List<AuthUser> users) {
        if (users.isEmpty()) {
            return new ArrayList<>();
        }
        // 机构/岗位名
        Set<Long> orgIds = users.stream().map(AuthUser::getOrgId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<Long> stationIds = users.stream().map(AuthUser::getStationId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> orgNames = orgIds.isEmpty() ? Collections.emptyMap()
                : orgMapper.selectBatchIds(orgIds).stream().collect(Collectors.toMap(CoreOrg::getId, CoreOrg::getName, (a, b) -> a));
        Map<Long, String> stationNames = stationIds.isEmpty() ? Collections.emptyMap()
                : stationMapper.selectBatchIds(stationIds).stream().collect(Collectors.toMap(CoreStation::getId, CoreStation::getName, (a, b) -> a));

        // 角色：一次查关联 + 一次查角色名，按 userId 分组
        Set<Long> userIds = users.stream().map(AuthUser::getId).collect(Collectors.toSet());
        Map<Long, List<Long>> roleIdMap = new HashMap<>();
        Map<Long, List<String>> roleNameMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<AuthUserRole> links = userRoleMapper.selectList(Wrappers.<AuthUserRole>lambdaQuery()
                    .in(AuthUserRole::getUserId, userIds));
            Set<Long> roleIds = links.stream().map(AuthUserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, String> roleNameById = roleIds.isEmpty() ? Collections.emptyMap()
                    : roleMapper.selectBatchIds(roleIds).stream()
                    .collect(Collectors.toMap(Role::getId, Role::getName, (a, b) -> a));
            for (AuthUserRole l : links) {
                roleIdMap.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(l.getRoleId());
                String nm = roleNameById.get(l.getRoleId());
                if (nm != null) {
                    roleNameMap.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(nm);
                }
            }
        }
        return users.stream().map(u -> toVO(u, orgNames, stationNames,
                roleIdMap.get(u.getId()), roleNameMap.get(u.getId()))).collect(Collectors.toList());
    }

    private UserVO toVO(AuthUser u, Map<Long, String> orgNames, Map<Long, String> stationNames,
                        List<Long> roleIds, List<String> roleNames) {
        UserVO v = new UserVO();
        v.setId(String.valueOf(u.getId()));
        v.setUsername(u.getAccount());
        v.setNickname(u.getName());
        v.setAvatar(u.getAvatar());
        v.setGender(u.getSex());
        v.setMobile(u.getMobile());
        v.setEmail(u.getEmail());
        v.setStatus(u.getStatus());
        v.setDeptId(u.getOrgId() == null ? null : String.valueOf(u.getOrgId()));
        v.setDeptName(u.getOrgId() == null ? null : orgNames.get(u.getOrgId()));
        v.setStationId(u.getStationId() == null ? null : String.valueOf(u.getStationId()));
        v.setStationName(u.getStationId() == null ? null : stationNames.get(u.getStationId()));
        v.setRoleIds(roleIds);
        v.setRoleNames(roleNames == null ? null : String.join(",", roleNames));
        v.setCreateTime(u.getCreateTime());
        return v;
    }

    private Long parseLong(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        try {
            return Long.valueOf(s.trim());
        } catch (NumberFormatException e) {
            throw BizException.validFail("ID 格式非法：" + s);
        }
    }
}
