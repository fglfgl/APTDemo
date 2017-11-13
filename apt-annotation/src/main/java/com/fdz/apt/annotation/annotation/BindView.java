package com.fdz.apt.annotation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : fangguiliang
 * @version : 1.0.0
 * @since : 2017/11/6
 * 最终用在Android控件上
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BindView {
    /**
     * @return 返回View的Id
     */
    int value() default 0;
}
