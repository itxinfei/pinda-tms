package com.itheima.pinda.validator.extract;

import java.util.Collection;
import java.util.List;

import com.itheima.pinda.validator.model.FieldValidatorDesc;
import com.itheima.pinda.validator.model.ValidConstraint;
import com.itheima.pinda.validator.model.FieldValidatorDesc;
import com.itheima.pinda.validator.model.ValidConstraint;


/**
 * 提取指定表单验证规则
 */
public interface IConstraintExtract {

    /**
     * 提取指定表单验证规则
     *
     * @param constraints
     * @return
     * @throws Exception
     */
    Collection<FieldValidatorDesc> extract(List<ValidConstraint> constraints) throws Exception;
}
