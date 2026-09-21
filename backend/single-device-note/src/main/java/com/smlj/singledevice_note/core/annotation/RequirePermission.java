package com.smlj.singledevice_note.core.annotation;

import com.smlj.singledevice_note.core.o.to.DataScope;

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

    /**
     * 额外要求的最小数据范围档位（编号即包含序，取值见 DataScope）；默认 NONE = 不额外要求。
     * <p>
     * 为什么光有权限点还不够：权限点回答「能不能做这类事」，数据范围回答「能管多宽的面」。
     * 「账号写入」这类**管理他人**的操作，权限点可以由角色授予，但数据范围只有「本人」的账号
     * 连别人都看不到，就绝不该具备管别人的能力 —— 即使他的角色里带着 perm:assign。
     * 范围不足时按无权限拒绝（RC10307），与「没有该权限点」对外表现一致，不暴露中间状态。
     * <p>
     * ⚠️ 判据是当前账号**实时**的 data_scope（TokenInterceptor 按 account 现查 t_user），
     * 不是 JWT 快照，所以改档位后最长一个缓存 TTL 内生效，不需要用户重新登录。
     */
    DataScope minScope() default DataScope.NONE;
}
