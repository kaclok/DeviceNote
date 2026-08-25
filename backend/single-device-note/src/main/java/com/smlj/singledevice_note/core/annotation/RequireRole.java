package com.smlj.singledevice_note.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色注解：标注在 Controller 方法上，声明调用此接口需要的角色 code。
 * 拦截器 AccessInterceptor 会从 JWT payload 中提取当前登录用户的 role_code，
 * 只要用户拥有 value 中任意一个角色即放行，否则返回 RC10307（无权限）。
 * <p>
 * 用法示例：
 * <pre>
 * &#64;RequireRole("ADMIN")
 * &#64;PostMapping("/contract/update")
 * public Result<?> contractUpdate(...) { ... }
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
