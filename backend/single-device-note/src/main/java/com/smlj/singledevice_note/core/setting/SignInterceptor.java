package com.smlj.singledevice_note.core.setting;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

// https://mp.weixin.qq.com/s/QYIOUNK5FZVu_jdiWGnMZg
public class SignInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String appId = request.getHeader("appId");
        String timeStamp = request.getHeader("timeStamp");
        String sign = request.getHeader("sign");

        return true;
    }
}
