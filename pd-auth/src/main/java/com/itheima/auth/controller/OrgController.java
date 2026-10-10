package com.itheima.auth.controller;

import com.itheima.auth.common.R;
import com.itheima.auth.entity.CoreOrg;
import com.itheima.auth.service.OrgService;
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
    public R<Long> save(@RequestBody CoreOrg org) {
        return R.success(orgService.save(org));
    }

    /** 修改组织 */
    @PutMapping
    public R<Void> update(@RequestBody CoreOrg org) {
        orgService.update(org);
        return R.success();
    }

    /** 删除组织 */
    @DeleteMapping
    public R<Void> remove(@RequestParam Long id) {
        orgService.remove(id);
        return R.success();
    }
}
