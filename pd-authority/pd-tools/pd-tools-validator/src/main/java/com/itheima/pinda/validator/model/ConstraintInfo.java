package com.itheima.pinda.validator.model;

import java.util.Map;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 检验约束信息
 */
@Data
@ToString
@Accessors(chain = true)
public class ConstraintInfo {
    /**
     * 约束对象的类型
     */
    private String type;
    /**
     * 约束属性
     */
    private Map<String, Object> attrs;

}
