package com.xinfei.auth.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.xinfei.auth.common.R;
import com.xinfei.auth.entity.CoreOrg;
import com.xinfei.auth.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 组织管理。经网关访问路径 /api/auth/org/**。
 *
 * <p>读接口（tree/detail）不设权限码：组织树是用户管理、营业网点下拉等表单的
 * 公共数据源（对齐《62》§11.1 CommonController 纪律，登录即可读）；
 * 写接口用按钮权限码双控（sys_menu 注册 = 前端 v-hasPerm）。</p>
 */
@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /** 组织树 */
    @GetMapping("/tree")
    public R<List<CoreOrg>> tree() {
        return R.success(orgService.tree());
    }

    /** 组织详情 */
    @GetMapping("/detail")
    public R<CoreOrg> detail(@RequestParam Long id) {
        return R.success(orgService.detail(id));
    }

    /** 新增组织 */
    @PostMapping
    @SaCheckPermission("sys:org:create")
    public R<Long> save(@RequestBody CoreOrg org) {
        return R.success(orgService.save(org));
    }

    /** 修改组织 */
    @PutMapping
    @SaCheckPermission("sys:org:update")
    public R<Void> update(@RequestBody CoreOrg org) {
        orgService.update(org);
        return R.success();
    }

    /** 删除组织 */
    @DeleteMapping
    @SaCheckPermission("sys:org:delete")
    public R<Void> remove(@RequestParam Long id) {
        orgService.remove(id);
        return R.success();
    }
}
