package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.DeptForm;
import com.xinfei.auth.common.v1.DeptVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.entity.SysDept;
import com.xinfei.auth.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 部门管理（youlai 契约 /api/v1/depts）：树形结构，内存组树。
 */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final SysDeptMapper deptMapper;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long ROOT = 0L;

    public List<DeptVO> tree(String keywords, Integer status) {
        List<SysDept> all = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .and(StringUtils.hasText(keywords), w -> w
                        .like(SysDept::getName, keywords)
                        .or().like(SysDept::getCode, keywords))
                .eq(status != null, SysDept::getStatus, status)
                .orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
        return build(all);
    }

    public List<OptionVO> options() {
        List<SysDept> all = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, 1).orderByAsc(SysDept::getSort).orderByAsc(SysDept::getId));
        Map<Long, List<SysDept>> byParent = group(all);
        return toOptions(byParent.getOrDefault(ROOT, new ArrayList<>()), byParent);
    }

    public DeptForm form(Long id) {
        SysDept d = mustGet(id);
        DeptForm f = new DeptForm();
        f.setId(String.valueOf(d.getId()));
        f.setName(d.getName());
        f.setCode(d.getCode());
        f.setParentId(String.valueOf(d.getParentId() == null ? ROOT : d.getParentId()));
        f.setSort(d.getSort());
        f.setStatus(d.getStatus());
        return f;
    }

    @Transactional
    public Long create(DeptForm form) {
        if (!StringUtils.hasText(form.getName())) {
            throw BizException.validFail("部门名称不能为空");
        }
        Long parentId = parseId(form.getParentId(), ROOT);
        SysDept d = new SysDept();
        d.setName(form.getName().trim());
        d.setCode(form.getCode());
        d.setParentId(parentId);
        d.setSort(form.getSort() == null ? 1 : form.getSort());
        d.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        d.setTreePath(pathOf(parentId));
        deptMapper.insert(d);
        // 落库后回填自身到路径
        deptMapper.update(null, Wrappers.<SysDept>lambdaUpdate()
                .eq(SysDept::getId, d.getId())
                .set(SysDept::getTreePath, pathOf(parentId) + d.getId() + ","));
        return d.getId();
    }

    @Transactional
    public void update(Long id, DeptForm form) {
        SysDept d = mustGet(id);
        Long parentId = parseId(form.getParentId(), ROOT);
        if (parentId.equals(id)) {
            throw BizException.validFail("上级部门不能选择自己");
        }
        if (StringUtils.hasText(form.getName())) {
            d.setName(form.getName().trim());
        }
        d.setCode(form.getCode());
        if (form.getSort() != null) {
            d.setSort(form.getSort());
        }
        if (form.getStatus() != null) {
            d.setStatus(form.getStatus());
        }
        if (!parentId.equals(d.getParentId() == null ? ROOT : d.getParentId())) {
            checkNotDescendant(id, parentId);
            d.setParentId(parentId);
            String newPrefix = pathOf(parentId);
            deptMapper.updateById(d);
            String oldPath = d.getTreePath();
            deptMapper.update(null, Wrappers.<SysDept>lambdaUpdate()
                    .eq(SysDept::getId, id)
                    .set(SysDept::getTreePath, newPrefix + id + ","));
            // 后代路径同步前缀替换
            if (oldPath != null && !oldPath.isBlank()) {
                deptMapper.update(null, Wrappers.<SysDept>lambdaUpdate()
                        .likeRight(SysDept::getTreePath, oldPath)
                        .ne(SysDept::getId, id)
                        .setSql("tree_path = REPLACE(tree_path, '" + oldPath + "', '" + newPrefix + id + "," + "')"));
            }
        } else {
            deptMapper.updateById(d);
        }
    }

    @Transactional
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            if (deptMapper.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, id)) > 0) {
                throw BizException.validFail("存在子部门，请先删除子部门");
            }
        }
        deptMapper.deleteByIds(ids);
    }

    private void checkNotDescendant(Long id, Long newParentId) {
        SysDept parent = deptMapper.selectById(newParentId);
        if (parent != null && parent.getTreePath() != null && parent.getTreePath().contains(id + ",")) {
            throw BizException.validFail("上级部门不能选择自己的下级");
        }
    }

    private String pathOf(Long parentId) {
        if (parentId == null || parentId == ROOT) {
            return "0,";
        }
        SysDept p = deptMapper.selectById(parentId);
        return (p != null && p.getTreePath() != null && !p.getTreePath().isBlank())
                ? p.getTreePath() : "0,";
    }

    private Long parseId(String s, long def) {
        if (!StringUtils.hasText(s)) {
            return def;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private List<DeptVO> build(List<SysDept> all) {
        Map<Long, List<SysDept>> byParent = group(all);
        Set<Long> ids = new HashSet<>();
        all.forEach(d -> ids.add(d.getId()));
        List<SysDept> roots = new ArrayList<>();
        byParent.getOrDefault(ROOT, new ArrayList<>()).forEach(roots::add);
        // 关键字过滤时父节点可能不在集合内，这些节点提升为根
        all.forEach(d -> {
            long pid = d.getParentId() == null ? ROOT : d.getParentId();
            if (pid != ROOT && !ids.contains(pid) && !roots.contains(d)) {
                roots.add(d);
            }
        });
        return toVOs(roots, byParent);
    }

    private Map<Long, List<SysDept>> group(List<SysDept> all) {
        return all.stream().collect(java.util.stream.Collectors.groupingBy(
                d -> d.getParentId() == null ? ROOT : d.getParentId(),
                LinkedHashMap::new, java.util.stream.Collectors.toList()));
    }

    private List<DeptVO> toVOs(List<SysDept> level, Map<Long, List<SysDept>> byParent) {
        List<DeptVO> out = new ArrayList<>();
        for (SysDept d : level) {
            DeptVO v = toVO(d);
            v.setChildren(toVOs(byParent.getOrDefault(d.getId(), new ArrayList<>()), byParent));
            out.add(v);
        }
        return out;
    }

    private List<OptionVO> toOptions(List<SysDept> level, Map<Long, List<SysDept>> byParent) {
        List<OptionVO> out = new ArrayList<>();
        for (SysDept d : level) {
            OptionVO o = new OptionVO();
            o.setValue(String.valueOf(d.getId()));
            o.setLabel(d.getName());
            o.setChildren(toOptions(byParent.getOrDefault(d.getId(), new ArrayList<>()), byParent));
            out.add(o);
        }
        return out;
    }

    private DeptVO toVO(SysDept d) {
        DeptVO v = new DeptVO();
        v.setId(String.valueOf(d.getId()));
        v.setParentId(String.valueOf(d.getParentId() == null ? ROOT : d.getParentId()));
        v.setName(d.getName());
        v.setCode(d.getCode());
        v.setSort(d.getSort());
        v.setStatus(d.getStatus());
        v.setTreePath(d.getTreePath());
        v.setCreateTime(d.getCreateTime() == null ? null : d.getCreateTime().format(FMT));
        v.setUpdateTime(d.getUpdateTime() == null ? null : d.getUpdateTime().format(FMT));
        return v;
    }

    private SysDept mustGet(Long id) {
        SysDept d = deptMapper.selectById(id);
        if (d == null) {
            throw BizException.validFail("部门不存在");
        }
        return d;
    }
}