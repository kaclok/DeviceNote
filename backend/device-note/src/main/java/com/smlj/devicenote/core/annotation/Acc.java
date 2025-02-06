package com.smlj.devicenote.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Target(PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Acc {
}
