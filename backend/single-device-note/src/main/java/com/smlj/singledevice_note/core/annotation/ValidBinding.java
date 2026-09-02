package com.smlj.singledevice_note.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBinding {
    // 标记需要处理 BindingResult 的方法
}
