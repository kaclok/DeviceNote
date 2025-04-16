package com.jthx.x.core.o.to;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
// https://mp.weixin.qq.com/s/efex3S0M6wKd8r-HsDxPzQ
// https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao
// 对controller 层中 ResponseBody 注解方法，进行增强拦截
@RestControllerAdvice(basePackages = "com.smlj.train")
public class ResponseResultAdvice implements ResponseBodyAdvice<Object> {
    // 是否开启支持功能
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 含有ResultPackIgnore的返回false
        return false;
    }

    // 如果开启，就会对返回结果进行处理, controller层无需返回Result对象类型，直接改成对应的业务对象即可
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        // 如果已经是 Result 类型，则直接返回
        if (body instanceof Result) {
            return body;
        }

        // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
        // 如果不是，则包装成 Result 类型
        // 如何处理失败的情况 https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao
        // 约定就是成功返回，所以使用 CommonResult#success(T data) 方法，进行包装成成功的 CommonResult 返回。那么，如果我们希望 API 接口是失败的返回呢？我们约定在 Controller 抛出异常，这点我们会在 「5. 全局异常处理」 看到。
        // 失败的情况走全局异常：CError的@ExceptionHandler(Exception.class)
        return Result.success(body);
    }
}
