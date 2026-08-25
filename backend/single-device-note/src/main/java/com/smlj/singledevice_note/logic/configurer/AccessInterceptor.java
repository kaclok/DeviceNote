package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.annotation.RequireRole;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class AccessInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        log.info("afterCompletion -> {}", handler);

        // 执行完毕之后，删除用户信息,防止Tomcat的 线程池数据残留 以及 内存泄露
        // 不在postHandler中执行，是因为怕controller的代码发生异常导致postHandler没有执行
        // CurUserService.remove();
        // HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // CurUserService.remove();
    }

    // https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
    // https://blog.csdn.net/Top_L398/article/details/109361680
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 如果不是映射到方法，直接通过
            // https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
            if (!(handler instanceof HandlerMethod handlerMethod)) {
                return true;
            }

            // 如果方法有JwtIgnore注解，直接通过
            // https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
            Method method = handlerMethod.getMethod();

            // @JwtIgnore 注解：跳过认证与鉴权
            if (method.isAnnotationPresent(JwtIgnore.class)) {
                return true;
            }

            // ---- 1. 身份认证：校验 JWT ----
            String at = request.getHeader(JwtUtil.AT_HEADER);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");

            // token 缺失：未登录
            if (at == null || at.isBlank()) {
                response.getWriter().write(Result.fail(ResultCode.RC10002).toJson());
                return false;
            }

            var jwt = JwtUtil.getJWTByToken(at);

            // 解析失败（token 格式错误）
            if (jwt == null) {
                response.getWriter().write(Result.fail(ResultCode.RC10005).toJson());
                return false;
            }

            // 签名校验
            if (!JwtUtil.verifyOnly(jwt)) {
                response.getWriter().write(Result.fail(ResultCode.RC10005).toJson());
                return false;
            }

            // 过期校验
            if (JwtUtil.isExpired(jwt)) {
                // 签名有效但已过期：前端凭refresh token无感刷新
                String json = Result.fail(ResultCode.RC10000).toJson();
                response.getWriter().write(json);
                return false;
            }

            // ---- 2. 权限/角色鉴权 ----
            Map<String, Object> claims = JwtUtil.parseToken(jwt);
            if (claims == null || claims.isEmpty()) {
                response.getWriter().write(Result.fail(ResultCode.RC10005).toJson());
                return false;
            }

            // 从 JWT payload 提取当前登录用户的 role_code 和 perms
            // claims 结构: {user: {account, username, role_code, role: {role_code, perms: [...]}}}
            String userRoleCode = null;
            Set<String> userPerms = new HashSet<>();

            Object userObj = claims.get("user");
            if (userObj instanceof Map<?, ?> userMap) {
                Object rc = userMap.get("role_code");
                if (rc != null) userRoleCode = rc.toString();

                Object roleObj = userMap.get("role");
                if (roleObj instanceof Map<?, ?> roleMap) {
                    Object permsObj = roleMap.get("perms");
                    if (permsObj instanceof java.util.List<?> permList) {
                        for (Object p : permList) {
                            if (p != null) userPerms.add(p.toString());
                        }
                    } else if (permsObj != null) {
                        // 兼容 JSON 反序列化为数组的情况
                        if (permsObj.getClass().isArray()) {
                            userPerms.addAll(Arrays.asList((Object[]) permsObj).stream()
                                    .filter(java.util.Objects::nonNull)
                                    .map(Object::toString)
                                    .toList());
                        }
                    }
                }
            }

            // 校验 @RequireRole
            if (method.isAnnotationPresent(RequireRole.class)) {
                RequireRole ann = method.getAnnotation(RequireRole.class);
                String[] requiredRoles = ann.value();
                boolean hasRole = false;
                if (userRoleCode != null) {
                    for (String r : requiredRoles) {
                        if (r.equals(userRoleCode)) {
                            hasRole = true;
                            break;
                        }
                    }
                }
                if (!hasRole) {
                    response.getWriter().write(Result.fail(ResultCode.RC10307).toJson());
                    return false;
                }
            }

            // 校验 @RequirePermission
            if (method.isAnnotationPresent(RequirePermission.class)) {
                RequirePermission ann = method.getAnnotation(RequirePermission.class);
                String[] requiredPerms = ann.value();
                boolean hasPerm = false;
                for (String p : requiredPerms) {
                    if (userPerms.contains(p)) {
                        hasPerm = true;
                        break;
                    }
                }
                if (!hasPerm) {
                    response.getWriter().write(Result.fail(ResultCode.RC10307).toJson());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error("AccessInterceptor preHandle error", e);
            // 不能吞异常：必须给前端一个可解析的 JSON 响应，否则前端拿到空 body 无法处理
            try {
                response.getWriter().write(Result.fail(ResultCode.RC_1).toJson());
            } catch (Exception ignored) {
            }
            return false;
        } finally {
            log.info("preHandle finally -> {}", handler);
        }
    }
}
