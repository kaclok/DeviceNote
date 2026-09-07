package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
import com.smlj.singledevice_note.core.annotation.RequirePermission;
import com.smlj.singledevice_note.core.annotation.RequireRole;
import com.smlj.singledevice_note.core.annotation.SignIgnore;
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
public class SignInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
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
