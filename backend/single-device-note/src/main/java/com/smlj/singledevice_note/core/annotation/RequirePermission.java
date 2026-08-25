package com.smlj.singledevice_note.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口权限注解：标注在 Controller 方法上，声明调用此接口需要的权限码。
 * 拦截器 AccessInterceptor 会从 JWT payload 中提取当前登录用户的 role.perms，
 * 只要用户拥有 value 中任意一个权限码即放行，否则返回 RC10307（无权限）。
 * <p>
 * 用法示例：
 * <pre>
 * &#64;RequirePermission("contract:create")
 * &#64;PostMapping("/contract/create")
 * public Result<?> contractCreate(...) { ... }
 * </pre>
 * 也支持多权限（满足任一即可）：
 * <pre>
 * &#64;RequirePermission({"contract:create", "contract:update"})
 * </pre>
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] value();
}
