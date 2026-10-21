package com.xinfei.auth.service.v1;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xinfei.auth.common.BizException;
import com.xinfei.auth.common.v1.DictItemForm;
import com.xinfei.auth.common.v1.DictItemOptionVO;
import com.xinfei.auth.common.v1.DictItemVO;
import com.xinfei.auth.common.v1.DictTypeForm;
import com.xinfei.auth.common.v1.DictTypeVO;
import com.xinfei.auth.common.v1.OptionVO;
import com.xinfei.auth.common.v1.PageResult;
import com.xinfei.auth.entity.DictItem;
import com.xinfei.auth.entity.DictType;
import com.xinfei.auth.mapper.DictItemMapper;
import com.xinfei.auth.mapper.DictTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 数据字典（《62》§4.3/§6，sys_dict_type / sys_dict_item）。
 *
 * <p>只承载"运营可配分类"（性别、通知类型等），业务状态机枚举不入此表（§10.5）；
 * 全系统仅此一套字典表/服务（D-20/BA-18）。租户隔离由插件自动处理，编码租户内唯一。</p>
 *
 * <p>唯一性放本层校验而非 DB 唯一索引：两张表都是逻辑删除，唯一索引会让"删除后重建同编码"永久失败。
 * 字典项以编码寻址（前端 /dicts/{code}/items），改编码不影响已有项（项按 dict_type_id 归属）。</p>
 */
@Service
@RequiredArgsConstructor
public class DictService {

    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    /** 字典编码作为 URL 路径段，限定为字母/数字/下划线/连字符/冒号 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z0-9_:\\-]{1,64}$");

    // ------------------------------------------------------------------ 字典类型

    public PageResult<DictTypeVO> page(long pageNum, long pageSize, String keywords, Integer status) {
        IPage<DictType> result = dictTypeMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<DictType>lambdaQuery()
                        .eq(status != null, DictType::getStatus, status)
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(DictType::getName, keywords)
                                .or().like(DictType::getCode, keywords))
                        .orderByAsc(DictType::getId));
        List<DictTypeVO> list = result.getRecords().stream().map(this::toTypeVO).toList();
        return PageResult.of(list, result.getTotal());
    }

    /** 启用的字典类型选项（value=编码） */
    public List<OptionVO> options() {
        return dictTypeMapper.selectList(Wrappers.<DictType>lambdaQuery()
                        .eq(DictType::getStatus, 1)
                        .orderByAsc(DictType::getId))
                .stream().map(t -> {
                    OptionVO o = new OptionVO();
                    o.setValue(t.getCode());
                    o.setLabel(t.getName());
                    return o;
                }).toList();
    }

    public DictTypeForm typeFormData(Long id) {
        DictType t = mustGetType(id);
        DictTypeForm f = new DictTypeForm();
        f.setId(String.valueOf(t.getId()));
        f.setName(t.getName());
        f.setDictCode(t.getCode());
        f.setStatus(t.getStatus());
        f.setRemark(t.getRemark());
        return f;
    }

    public Long createType(DictTypeForm form) {
        validateType(form);
        if (countTypeByCode(form.getDictCode().trim(), null) > 0) {
            throw BizException.validFail("字典编码已存在：" + form.getDictCode());
        }
        DictType t = new DictType();
        t.setName(form.getName().trim());
        t.setCode(form.getDictCode().trim());
        t.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        t.setRemark(form.getRemark());
        dictTypeMapper.insert(t);
        return t.getId();
    }

    public void updateType(Long id, DictTypeForm form) {
        DictType exist = mustGetType(id);
        validateType(form);
        String code = form.getDictCode().trim();
        if (!code.equals(exist.getCode())) {
            if (countTypeByCode(code, id) > 0) {
                throw BizException.validFail("字典编码已存在：" + code);
            }
            exist.setCode(code);
        }
        exist.setName(form.getName().trim());
        if (form.getStatus() != null) {
            exist.setStatus(form.getStatus());
        }
        exist.setRemark(form.getRemark());
        dictTypeMapper.updateById(exist);
    }

    /** 删除字典类型：先删项，避免已有项被孤立成"看不见的引用源" */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTypes(List<Long> ids) {
        for (Long id : ids) {
            DictType t = mustGetType(id);
            Long itemCount = dictItemMapper.selectCount(Wrappers.<DictItem>lambdaQuery()
                    .eq(DictItem::getDictTypeId, id));
            if (itemCount != null && itemCount > 0) {
                throw BizException.validFail("字典[" + t.getName() + "]下存在字典项，请先删除字典项");
            }
            dictTypeMapper.deleteById(id);
        }
    }

    // ------------------------------------------------------------------ 字典项

    /** 字典项分页（管理页，含停用项） */
    public PageResult<DictItemVO> itemPage(String dictCode, long pageNum, long pageSize, String keywords) {
        DictType type = mustGetTypeByCode(dictCode);
        IPage<DictItem> result = dictItemMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<DictItem>lambdaQuery()
                        .eq(DictItem::getDictTypeId, type.getId())
                        .and(StringUtils.hasText(keywords), w -> w
                                .like(DictItem::getLabel, keywords)
                                .or().like(DictItem::getValue, keywords))
                        .orderByAsc(DictItem::getSortValue).orderByAsc(DictItem::getId));
        List<DictItemVO> list = result.getRecords().stream()
                .map(i -> toItemVO(i, type.getCode())).toList();
        return PageResult.of(list, result.getTotal());
    }

    /**
     * 启用中的字典项（DictSelect 候选与 DictTag 取标签）。
     *
     * <p>只看项的 status，不看字典类型的 status：类型停用后，存量业务数据仍需能翻译出标签。</p>
     */
    public List<DictItemOptionVO> itemOptions(String dictCode) {
        DictType type = mustGetTypeByCode(dictCode);
        return dictItemMapper.selectList(Wrappers.<DictItem>lambdaQuery()
                        .eq(DictItem::getDictTypeId, type.getId())
                        .eq(DictItem::getStatus, 1)
                        .orderByAsc(DictItem::getSortValue).orderByAsc(DictItem::getId))
                .stream().map(i -> {
                    DictItemOptionVO o = new DictItemOptionVO();
                    o.setValue(i.getValue());
                    o.setLabel(i.getLabel());
                    o.setTagType(i.getTagType());
                    return o;
                }).toList();
    }

    public DictItemForm itemFormData(String dictCode, Long id) {
        DictType type = mustGetTypeByCode(dictCode);
        DictItem item = mustGetItem(type.getId(), id);
        DictItemForm f = new DictItemForm();
        f.setId(String.valueOf(item.getId()));
        f.setDictCode(type.getCode());
        f.setLabel(item.getLabel());
        f.setValue(item.getValue());
        f.setStatus(item.getStatus());
        f.setSort(item.getSortValue());
        f.setTagType(item.getTagType());
        f.setRemark(item.getRemark());
        return f;
    }

    public Long createItem(String dictCode, DictItemForm form) {
        DictType type = mustGetTypeByCode(dictCode);
        validateItem(form);
        String value = form.getValue().trim();
        if (countItemByValue(type.getId(), value, null) > 0) {
            throw BizException.validFail("字典值已存在：" + value);
        }
        DictItem item = new DictItem();
        item.setDictTypeId(type.getId());
        item.setLabel(form.getLabel().trim());
        item.setValue(value);
        item.setTagType(normalizeTagType(form.getTagType()));
        item.setSortValue(form.getSort() == null ? 1 : form.getSort());
        item.setStatus(form.getStatus() == null ? 1 : form.getStatus());
        item.setRemark(form.getRemark());
        dictItemMapper.insert(item);
        return item.getId();
    }

    public void updateItem(String dictCode, Long id, DictItemForm form) {
        DictType type = mustGetTypeByCode(dictCode);
        DictItem exist = mustGetItem(type.getId(), id);
        validateItem(form);
        String value = form.getValue().trim();
        if (!value.equals(exist.getValue()) && countItemByValue(type.getId(), value, id) > 0) {
            throw BizException.validFail("字典值已存在：" + value);
        }
        exist.setLabel(form.getLabel().trim());
        exist.setValue(value);
        exist.setTagType(normalizeTagType(form.getTagType()));
        if (form.getSort() != null) {
            exist.setSortValue(form.getSort());
        }
        if (form.getStatus() != null) {
            exist.setStatus(form.getStatus());
        }
        exist.setRemark(form.getRemark());
        dictItemMapper.updateById(exist);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItems(String dictCode, List<Long> ids) {
        DictType type = mustGetTypeByCode(dictCode);
        for (Long id : ids) {
            mustGetItem(type.getId(), id);
            dictItemMapper.deleteById(id);
        }
    }

    // ------------------------------------------------------------------ 内部

    private DictType mustGetType(Long id) {
        DictType t = dictTypeMapper.selectById(id);
        if (t == null) {
            throw BizException.validFail("字典不存在");
        }
        return t;
    }

    private DictType mustGetTypeByCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw BizException.validFail("缺少字典编码");
        }
        DictType t = dictTypeMapper.selectOne(Wrappers.<DictType>lambdaQuery()
                .eq(DictType::getCode, code.trim()));
        if (t == null) {
            throw BizException.validFail("字典不存在：" + code);
        }
        return t;
    }

    /** 字典项必须属于该字典，跨字典 id 一律视为不存在 */
    private DictItem mustGetItem(Long dictTypeId, Long id) {
        DictItem item = dictItemMapper.selectById(id);
        if (item == null || !item.getDictTypeId().equals(dictTypeId)) {
            throw BizException.validFail("字典项不存在");
        }
        return item;
    }

    private Long countTypeByCode(String code, Long excludeId) {
        return dictTypeMapper.selectCount(Wrappers.<DictType>lambdaQuery()
                .eq(DictType::getCode, code)
                .ne(excludeId != null, DictType::getId, excludeId));
    }

    private Long countItemByValue(Long dictTypeId, String value, Long excludeId) {
        return dictItemMapper.selectCount(Wrappers.<DictItem>lambdaQuery()
                .eq(DictItem::getDictTypeId, dictTypeId)
                .eq(DictItem::getValue, value)
                .ne(excludeId != null, DictItem::getId, excludeId));
    }

    private void validateType(DictTypeForm form) {
        if (!StringUtils.hasText(form.getName())) {
            throw BizException.validFail("字典名称不能为空");
        }
        if (!StringUtils.hasText(form.getDictCode())) {
            throw BizException.validFail("字典编码不能为空");
        }
        if (!CODE_PATTERN.matcher(form.getDictCode().trim()).matches()) {
            throw BizException.validFail("字典编码只能包含字母、数字、下划线、连字符和冒号");
        }
    }

    private void validateItem(DictItemForm form) {
        if (!StringUtils.hasText(form.getLabel())) {
            throw BizException.validFail("字典项标签不能为空");
        }
        if (!StringUtils.hasText(form.getValue())) {
            throw BizException.validFail("字典项值不能为空");
        }
    }

    /** 空标签样式落库为空串，避免前端解码出 null */
    private String normalizeTagType(String tagType) {
        return StringUtils.hasText(tagType) ? tagType.trim() : "";
    }

    private DictTypeVO toTypeVO(DictType t) {
        DictTypeVO v = new DictTypeVO();
        v.setId(String.valueOf(t.getId()));
        v.setName(t.getName());
        v.setDictCode(t.getCode());
        v.setStatus(t.getStatus());
        return v;
    }

    private DictItemVO toItemVO(DictItem i, String dictCode) {
        DictItemVO v = new DictItemVO();
        v.setId(String.valueOf(i.getId()));
        v.setDictCode(dictCode);
        v.setLabel(i.getLabel());
        v.setValue(i.getValue());
        v.setStatus(i.getStatus());
        v.setSort(i.getSortValue());
        v.setTagType(i.getTagType());
        v.setRemark(i.getRemark());
        return v;
    }
}
