package com.smlj.devicenote.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ResultPackIgnore {
}
