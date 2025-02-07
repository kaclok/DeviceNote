package com.smlj.common.configurer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

// https://mp.weixin.qq.com/s/efex3S0M6wKd8r-HsDxPzQ
// https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao
// 对controller 层中 ResponseBody 注解方法，进行增强拦截
@Slf4j
@Configuration
public class ReturnResolver implements HandlerMethodReturnValueHandler {
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        // return returnType.getParameterType().equals(Result.class);
        return false;
    }

    // https://www.cnblogs.com/chenss15060100790/p/9095496.html
    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        // 如果已经是 CommonResult 类型，则直接返回
    }
}
