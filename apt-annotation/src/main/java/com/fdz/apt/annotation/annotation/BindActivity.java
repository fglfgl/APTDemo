package com.fdz.apt.annotation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author : fangguiliang
 * @version : 1.0.0
 * @since : 2017/11/6
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface BindActivity {
}
