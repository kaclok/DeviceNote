package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.SignIgnore;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

@Slf4j
@Component
public class SignInterceptor implements HandlerInterceptor {
    // 允许的时间误差（5分钟）
    private static final long ALLOWED_TIME_DRIFT = 5 * 60 * 1000;

    // Header名称（与前端保持一致）
    private static final String HEADER_SIGN = "__sign__";
    private static final String HEADER_TIMESTAMP = "__timestamp__";
    private static final String HEADER_NONCE = "__nonce__";

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, @Nullable Exception ex) throws Exception {
        log.info("afterCompletion -> {}", handler);

        // 执行完毕之后，删除用户信息,防止Tomcat的 线程池数据残留 以及 内存泄露
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
            if (method.isAnnotationPresent(SignIgnore.class)) {
                return true;
            }

            return true;
        } catch (Exception e) {
            log.error("SignInterceptor preHandle error", e);
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
