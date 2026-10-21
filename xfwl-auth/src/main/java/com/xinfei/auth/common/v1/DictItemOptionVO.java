package com.xinfei.auth.common.v1;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典项下拉/标签选项（对齐前端 DictItemOption：value/label/tagType）。
 *
 * <p>与通用 {@link OptionVO} 分开：本结构带 tagType，供前端 DictSelect 与 DictTag 渲染标签样式。</p>
 */
@Data
public class DictItemOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private String label;
    private String tagType;
}
