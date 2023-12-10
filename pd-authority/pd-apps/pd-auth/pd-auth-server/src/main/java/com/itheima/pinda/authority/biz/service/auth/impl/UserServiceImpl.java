package com.itheima.pinda.authority.biz.service.auth.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.pinda.authority.biz.dao.auth.UserMapper;
import com.itheima.pinda.authority.biz.service.auth.RoleOrgService;
import com.itheima.pinda.authority.biz.service.auth.RoleService;
import com.itheima.pinda.authority.biz.service.auth.UserRoleService;
import com.itheima.pinda.authority.biz.service.auth.UserService;
import com.itheima.pinda.authority.dto.auth.UserUpdatePasswordDTO;
import com.itheima.pinda.authority.entity.auth.Role;
import com.itheima.pinda.authority.entity.auth.RoleOrg;
import com.itheima.pinda.authority.entity.auth.User;
import com.itheima.pinda.authority.entity.auth.UserRole;
import com.itheima.pinda.authority.entity.core.Org;
import com.itheima.pinda.common.constant.BizConstant;
import com.itheima.pinda.context.BaseContextHandler;
import com.itheima.pinda.database.mybatis.auth.DataScope;
import com.itheima.pinda.database.mybatis.auth.DataScopeType;
import com.itheima.pinda.database.mybatis.conditions.Wraps;
import com.itheima.pinda.database.mybatis.conditions.query.LbqWrapper;
import com.itheima.pinda.utils.BizAssert;

import cn.hutool.core.util.StrUtil;
import com.itheima.pinda.authority.biz.service.core.OrgService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 业务实现类
 * 账号
 * </p>
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private RoleService roleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleOrgService roleOrgService;
    @Autowired
    private OrgService orgService;

    @Override
    public IPage<User> findPage(IPage<User> page, LbqWrapper<User> wrapper) {
        DataScope dataScope = new DataScope();
        IPage<User> userIPage = baseMapper.findPage(page, wrapper, null);
        return userIPage;
    }

    @Override
    public int resetPassErrorNum(Long id) {
        return baseMapper.resetPassErrorNum(id);
    }

    @Override
    public Boolean updatePassword(UserUpdatePasswordDTO data) {
        BizAssert.equals(data.getConfirmPassword(), data.getPassword(), "密码与确认密码不一致");

        User user = getById(data.getId());
        BizAssert.notNull(user, "用户不存在");
        String oldPassword = DigestUtils.md5Hex(data.getOldPassword());
        BizAssert.equals(user.getPassword(), oldPassword, "旧密码错误");

        User build = User.builder().password(data.getPassword()).id(data.getId()).build();
        this.updateUser(build);
        return true;
    }

    @Override
    public User getByAccount(String account) {
        //TODO 缓存
        return super.getOne(Wraps.<User>lbQ().eq(User::getAccount, account));
    }

    @Override
    public List<User> findUserByRoleId(Long roleId, String keyword) {
        return baseMapper.findUserByRoleId(roleId, keyword);
    }

    @Override
    public Map<String, Object> getDataScopeById(Long userId) {
        Map<String, Object> map = new HashMap<>(2);
        List<Long> orgIds = new ArrayList<>();
        DataScopeType dsType = DataScopeType.SELF;

        List<Role> list = roleService.findRoleByUserId(userId);
        Optional<Role> min = list.stream().min(Comparator.comparingInt((item) -> item.getDsType().getVal()));

        if (min.isPresent()) {
            Role role = min.get();
            dsType = role.getDsType();

            if (DataScopeType.CUSTOMIZE.eq(dsType)) {
                LbqWrapper<RoleOrg> wrapper = Wraps.<RoleOrg>lbQ().select(RoleOrg::getOrgId).eq(RoleOrg::getRoleId, role.getId());
                List<RoleOrg> roleOrgList = roleOrgService.list(wrapper);

                orgIds = roleOrgList.stream().mapToLong(RoleOrg::getOrgId).boxed().collect(Collectors.toList());
            } else if (DataScopeType.THIS_LEVEL.eq(dsType)) {
                User user = super.getById(userId);
                if (user != null) {
                    orgIds.add(user.getOrgId());
                }
            } else if (DataScopeType.THIS_LEVEL_CHILDREN.eq(dsType)) {
                User user = super.getById(userId);
                if (user != null) {
                    List<Org> orgList = orgService.findChildren(Arrays.asList(user.getOrgId()));
                    orgIds = orgList.stream().mapToLong(Org::getId).boxed().collect(Collectors.toList());
                }
            }
        }
        map.put("dsType", dsType.getVal());
        map.put("orgIds", orgIds);
        return map;
    }

    @Override
    public boolean check(String account) {
        return super.count(Wraps.<User>lbQ().eq(User::getAccount, account)) > 0;
    }

    @Override
    public void updatePasswordErrorNumById(Long id) {
        baseMapper.incrPasswordErrorNumById(id);
    }

    @Override
    public void updateLoginTime(String account) {
//        baseMapper.update(User.builder().lastLoginTime(LocalDateTime.now()).build(), Wraps.<User>lbQ().eq(User::getAccount, account));
        baseMapper.updateLastLoginTime(account, LocalDateTime.now());
    }

    @Override
    public User saveUser(User user) {
        // 永不过期
        user.setPasswordExpireTime(null);

        user.setPassword(DigestUtils.md5Hex(user.getPassword()));
        user.setPasswordErrorNum(0);
        super.save(user);
        return user;
    }

    @Override
    public boolean reset(List<Long> ids) {
        LocalDateTime passwordExpireTime = null;

        String defPassword = BizConstant.DEF_PASSWORD;
        super.update(Wraps.<User>lbU().set(User::getPassword, defPassword).set(User::getPasswordErrorNum, 0L).set(User::getPasswordErrorLastTime, null).set(User::getPasswordExpireTime, passwordExpireTime).in(User::getId, ids));
        return true;
    }

    @Override
    public User updateUser(User user) {
        // 永不过期
        user.setPasswordExpireTime(null);

        if (StrUtil.isNotEmpty(user.getPassword())) {
            user.setPassword(DigestUtils.md5Hex(user.getPassword()));
        }
        super.updateById(user);
        return user;
    }

    @Override
    public boolean remove(List<Long> ids) {
        userRoleService.remove(Wraps.<UserRole>lbQ().in(UserRole::getUserId, ids));
        return super.removeByIds(ids);
    }
}
