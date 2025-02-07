package com.smlj.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// https://mp.weixin.qq.com/s/QwKNwYWfKFFPjAR6B6mu-Q
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface LimitHzPerSecond {
    // 每秒请求次数
    public int hz() default 1;
}
