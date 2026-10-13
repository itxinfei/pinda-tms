package com.itheima.auth.common.v1;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用下拉/树选项（对齐前端 OptionItem）。
 */
@Data
public class OptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String value;
    private String label;
    private List<OptionVO> children = new ArrayList<>();
}
