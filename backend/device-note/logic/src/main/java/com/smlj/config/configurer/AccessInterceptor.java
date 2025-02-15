package com.smlj.config.configurer;

import com.smlj.annotation.JwtIgnore;
import com.smlj.o.to.AccountUser;
import com.smlj.service.CurUserService;
import com.smlj.utils.JwtUtil;
import io.micrometer.common.lang.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

//拦截器：创建拦截器interceptor来统一拦截令牌完成登录验证
// https://blog.csdn.net/2302_79840586/article/details/140874396
// https://www.bilibili.com/video/BV1mm4y1X7Hc/?p=5&spm_id_from=pageDriver&vd_source=f7f8eb7cb3d5fa76140706bd38e9f38e
// https://zhuanlan.zhihu.com/p/342744060 https://zhuanlan.zhihu.com/p/342746910 https://zhuanlan.zhihu.com/p/342755411 https://blog.csdn.net/m0_37834471/article/details/83144761
// https://docs.springframework.org.cn/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html
@Slf4j
@RequiredArgsConstructor
@Component
public class AccessInterceptor implements HandlerInterceptor {
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        log.info("afterCompletion -> {}", handler);

        // 执行完毕之后，删除用户信息,防止Tomcat的 线程池数据残留 以及 内存泄露
        // 不在postHandler中执行，是因为怕controller的代码发生异常导致postHandler没有执行
        CurUserService.remove();
        // HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           @org.springframework.lang.Nullable ModelAndView modelAndView) throws Exception {
        CurUserService.remove();
    }

    // https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
    // https://blog.csdn.net/Top_L398/article/details/109361680
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // request.getRequestURI();
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
            String token = request.getHeader(JwtUtil.JWT_HEADER);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("utf-8");
            var jwt = JwtUtil.getJWTByToken(token);
            if (!JwtUtil.verifyOnly(jwt)) {
                return false;
            }

            // 对于每次请求，检验jwt成功之后，记录当前用户
            AccountUser au = new AccountUser();
            // 将au保存
            request.setAttribute("AccProfile", au);

            CurUserService.set(au);
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