package com.smlj.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Spring Boot结合Redis实现接口防止重复提交
// https://mp.weixin.qq.com/s/Q40PjpJgtBJjnhmmteMxFg
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatSubmit {
    long expireTime() default 2; // 默认过期时间2s
}
