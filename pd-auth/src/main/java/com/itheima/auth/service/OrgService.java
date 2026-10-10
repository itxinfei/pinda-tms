package com.itheima.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.itheima.auth.common.BizException;
import com.itheima.auth.entity.CoreOrg;
import com.itheima.auth.mapper.CoreOrgMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织管理：树形查询、新增、修改（支持移动子树）、删除。
 * 租户隔离由 TenantLineInnerInterceptor 自动处理。
 */
@Service
@RequiredArgsConstructor
public class OrgService {

    private final CoreOrgMapper orgMapper;

    /**
     * 当前租户的组织树
     */
    public List<CoreOrg> tree() {
        List<CoreOrg> all = orgMapper.selectList(Wrappers.<CoreOrg>lambdaQuery()
                .orderByAsc(CoreOrg::getSortValue)
                .orderByAsc(CoreOrg::getId));
        Map<Long, List<CoreOrg>> childrenByParent = all.stream()
                .collect(Collectors.groupingBy(o -> o.getParentId() == null ? 0L : o.getParentId()));
        all.forEach(o -> o.setChildren(childrenByParent.get(o.getId())));
        return childrenByParent.getOrDefault(0L, List.of());
    }

    /**
     * 组织详情
     */
    public CoreOrg detail(Long id) {
        CoreOrg org = orgMapper.selectById(id);
        if (org == null) {
            throw new BizException("组织不存在");
        }
        return org;
    }

    /**
     * 新增组织，返回新ID
     */
    public Long save(CoreOrg org) {
        org.setId(null);
        Long parentId = org.getParentId() == null ? 0L : org.getParentId();
        org.setParentId(parentId);
        org.setTreePath(buildChildTreePath(parentId));
        org.setCreateBy(StpUtil.getLoginIdAsLong());
        org.setCreateTime(LocalDateTime.now());
        orgMapper.insert(org);
        return org.getId();
    }

    /**
     * 修改组织。parentId 变化时移动整棵子树并刷新 tree_path。
     */
    public void update(CoreOrg org) {
        if (org.getId() == null) {
            throw new BizException("缺少组织ID");
        }
        CoreOrg exist = orgMapper.selectById(org.getId());
        if (exist == null) {
            throw new BizException("组织不存在");
        }
        Long newParentId = org.getParentId() == null ? 0L : org.getParentId();
        String newTreePath = exist.getTreePath();
        if (!newParentId.equals(exist.getParentId())) {
            newTreePath = moveSubTree(exist, newParentId);
        }
        // treePath 已在移动中处理，避免被入参里的旧值覆盖；tenantId 不在此更新
        org.setTreePath(null);
        org.setUpdateBy(StpUtil.getLoginIdAsLong());
        org.setUpdateTime(LocalDateTime.now());
        orgMapper.updateById(org);
    }

    /**
     * 删除组织：存在下级时拒绝
     */
    public void remove(Long id) {
        Long childCount = orgMapper.selectCount(Wrappers.<CoreOrg>lambdaQuery()
                .eq(CoreOrg::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BizException("存在下级组织，请先删除下级组织");
        }
        orgMapper.deleteById(id);
    }

    /**
     * 计算挂到指定父节点下的子节点 tree_path
     */
    private String buildChildTreePath(Long parentId) {
        if (parentId == 0L) {
            return ",";
        }
        CoreOrg parent = orgMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException("所选上级组织不存在");
        }
        return parent.getTreePath() + parentId + ",";
    }

    /**
     * 校验移动合法性，并把本节点及全部后代的 tree_path 前缀替换为新位置，返回本节点新 tree_path。
     */
    private String moveSubTree(CoreOrg self, Long newParentId) {
        if (newParentId.equals(self.getId())) {
            throw new BizException("不能将组织自身设为上级");
        }
        String newTreePath;
        if (newParentId == 0L) {
            newTreePath = ",";
        } else {
            CoreOrg newParent = orgMapper.selectById(newParentId);
            if (newParent == null) {
                throw new BizException("所选上级组织不存在");
            }
            // 新父的祖先链里若含本节点，说明它是本节点的后代，移动会成环
            if (newParent.getTreePath() != null
                    && newParent.getTreePath().contains("," + self.getId() + ",")) {
                throw new BizException("不能将组织移动到它的下级组织下");
            }
            newTreePath = newParent.getTreePath() + newParentId + ",";
        }

        String oldTreePath = self.getTreePath();
        // 本节点 + 祖先链包含本节点的全部后代
        List<CoreOrg> subTree = orgMapper.selectList(Wrappers.<CoreOrg>lambdaQuery()
                .and(w -> w.eq(CoreOrg::getId, self.getId())
                        .or().like(CoreOrg::getTreePath, "," + self.getId() + ",")));
        for (CoreOrg node : subTree) {
            String shifted = newTreePath + node.getTreePath().substring(oldTreePath.length());
            CoreOrg patch = new CoreOrg();
            patch.setId(node.getId());
            patch.setTreePath(shifted);
            orgMapper.updateById(patch);
        }
        return newTreePath;
    }
}
