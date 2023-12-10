package com.itheima.pinda.validator.mateconstraint;

import java.lang.annotation.Annotation;

import com.itheima.pinda.validator.model.ConstraintInfo;
import com.itheima.pinda.validator.model.ConstraintInfo;


/**
 * 约束转换器
 */
public interface IConstraintConverter {

    /**
     * 支持的类型
     *
     * @param clazz
     * @return
     */
    boolean support(Class<? extends Annotation> clazz);

    /**
     * 转换
     *
     * @param ano
     * @return
     * @throws Exception
     */
    ConstraintInfo converter(Annotation ano) throws Exception;
}
