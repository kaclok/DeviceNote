package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.annotation.RequireRole;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.utils.JwtUtil;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTRole;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCGHTUser;
import com.smlj.singledevice_note.logic.service.CurUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {
    /**
     * 实时用户解析（account -> 用户 + 角色，带 30s TTL 缓存）。
     * token 里只有 account，凡是"需要 username / role / perms / data_scope / 停用状态"的地方
     * 都必须走它，不要再去读 JWT 里的快照。
     */
    private final CurUserService curUserService;

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) throws Exception {
        log.info("afterCompletion -> {}", handler);

        // 执行完毕之后，删除用户信息,防止Tomcat的 线程池数据残留 以及 内存泄露
        // 不在postHandler中执行，是因为怕controller的代码发生异常导致postHandler没有执行
        // CurUserService.remove();
        // HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    @Override
    public void postHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, ModelAndView modelAndView) throws Exception {
        // CurUserService.remove();
    }

    // https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
    // https://blog.csdn.net/Top_L398/article/details/109361680
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
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

            // token 里只有 account（见 JwtUtil.ACCOUNT_CLAIM），用户信息与权限一律按 account 现查 t_user。
            // 这一条就是"admin 改了我的信息、本地 token 不及时"的解药：改姓名/角色/权限/数据范围后
            // 最长 TTL 内生效，不需要用户重新登录；账号被停用或删除则下一个请求就被拒。
            Object account = claims.get(JwtUtil.ACCOUNT_CLAIM);
            if (account == null || account.toString().isBlank()) {
                // 拿不到 account 的 token（含改造前签发、payload 里存整个 user 对象的旧 token）
                // 一律判失效，让前端走重新登录，而不是猜一个字段出来继续用。
                response.getWriter().write(Result.fail(ResultCode.RC10005).toJson());
                return false;
            }

            TCGHTUser user = curUserService.byAccount(account.toString());
            // fail-closed：解析不出用户（账号不存在 / 已停用）视为登录态失效。
            // 用 RC10005 而不是 RC10301：对前端这与"token 校验不正确"是同一类处理
            // （清登录态 + 跳登录页），同时不向拿着旧 token 的人暴露"该账号是否存在"。
            if (user == null) {
                response.getWriter().write(Result.fail(ResultCode.RC10005).toJson());
                return false;
            }
            request.setAttribute("Acc", user);

            // 注意：这里 setAttribute 的 user 与 token 无关，是本次请求现查的实时账号行。
            // 业务层通过 @Acc 拿到的 username / role_code / data_scope 因此都是实时的 ——
            // 不再有"列表按 token 快照分档、expandScope 按库分档"这种口径分裂。
            String userRoleCode = user.getRole_code();
            Set<String> userPerms = new HashSet<>();
            TCGHTRole role = user.getRole();
            if (role != null && role.getPerms() != null) {
                for (String p : role.getPerms()) {
                    if (p != null) userPerms.add(p);
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
            log.error("TokenInterceptor preHandle error", e);
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
