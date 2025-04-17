package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.JwtIgnore;
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
            if (method.isAnnotationPresent(JwtIgnore.class)) {
                return true;
            }

            // 从请求头获取token
            String token = request.getHeader(JwtUtil.AT_HEADER);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            var jwt = JwtUtil.getJWTByToken(token);
            if (!JwtUtil.verifyOnly(jwt)) {
                String json = Result.fail(ResultCode.RC10005).toJson();
                response.getWriter().write(json);
                return false;
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return false;
        } finally {
            log.info("preHandle finally -> {}", handler);
        }
    }
}
