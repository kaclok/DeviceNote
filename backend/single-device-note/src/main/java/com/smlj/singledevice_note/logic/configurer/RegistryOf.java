package com.smlj.singledevice_note.logic.configurer;

import com.smlj.singledevice_note.core.annotation.Acc;
import com.smlj.singledevice_note.core.annotation.AccProfile;
import com.smlj.singledevice_note.core.o.converter.Long2Date;
import com.smlj.singledevice_note.core.o.converter.String2Date;
import com.smlj.singledevice_note.core.o.converter.StringToKV;
import com.smlj.singledevice_note.logic.o.vo.converter.StringToTDeviceRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

//注册拦截器
//当需要对目标对象（target object）执行某种横切关注点（cross-cutting concern）操作，
//比如日志记录、事务处理等，开发者会将相关的拦截器注册到registry里。当匹配的条件触发时，
//registry会按照配置顺序查找并执行相应的拦截器，实现了对业务流程的动态增强。
// https://blog.csdn.net/2302_79840586/article/details/140640257 https://zhuanlan.zhihu.com/p/342744060
// https://docs.springframework.org.cn/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/arguments.html 每个控制器接口都自带很多默认参数
@EnableWebMvc // https://docs.springframework.org.cn/spring-framework/reference/web/webmvc/mvc-config/enable.html
@Slf4j
@RequiredArgsConstructor
@Configuration
public class RegistryOf implements WebMvcConfigurer {
    private final TokenInterceptor tokenInterceptor;
    private final SignInterceptor signInterceptor;

    // 侧重于处理接口参数
    // Formatter 优先于 Converter
    @Override
    public void addFormatters(FormatterRegistry registry) {
        // todo 为什么总是失败?
        //registry.addConverter(new StringTo<KV>(KV.class));

        registry.addConverter(new StringToTDeviceRecord());

        registry.addConverter(new StringToKV<Long, Float>());
        registry.addConverter(new StringToKV<Long, String>());
        registry.addConverter(new StringToKV<Integer, String>());
        registry.addConverter(new StringToKV<Integer, Float>());
        registry.addConverter(new StringToKV<Integer, Integer>());

        registry.addConverter(new Long2Date());

        registry.addConverter(new String2Date());

        // 无论你注册的是 Formatter 还是 Converter，最终都会变成统一的 Converter 接口
        // registry.addFormatter(new MultiDateFormatter());
    }

    // 拦截器针对方法，HandlerMethodArgumentResolver针对参数
    // https://docs.springframework.org.cn/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html
    @Override
    public void addInterceptors(InterceptorRegistry registry) { // registry是注册器，用来管理拦截器
        log.warn("========= addInterceptors ========= ");

        // https://docs.springframework.org.cn/spring-framework/reference/web/webmvc/mvc-servlet/localeresolver.html
        // registry.addInterceptor(new org.springframework.web.servlet.i18n.LocaleChangeInterceptor());

        /*var i = registry.addInterceptor(signInterceptor);
        i.addPathPatterns("/train/**");*/

        // 1、 处理token校验问题
        var i = registry.addInterceptor(tokenInterceptor);
        i.addPathPatterns("/cghtz/**");
        i.excludePathPatterns("/cghtz/account/login", "/x/getRefreshToken", "/x/getAccessToken", "/x/refreshAccessToken", "/swagger-ui/*");

        // 2、处理参数校验问题
        var si = registry.addInterceptor(signInterceptor);
        si.addPathPatterns("/cghtz/**");
        si.excludePathPatterns("/cghtz/account/login", "/x/getRefreshToken", "/x/getAccessToken", "/x/refreshAccessToken", "/swagger-ui/*");
    }

    // 用于处理接收消息 和 发送消息，比如将接收的消息转换为json, 侧重于处理消息
    @Override
    public void configureMessageConverters(@NonNull List<HttpMessageConverter<?>> converters) {

    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // 解决每个接口都需要AccountUserName.Get()的尴尬
        // https://blog.csdn.net/u013078871/article/details/124196337

        resolvers.add(new AnnoArgResolver<AccProfile>(AccProfile.class));
        resolvers.add(new AnnoArgResolver<Acc>(Acc.class));
    }

    /*@Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        // ResponseResultAdvice 实现将每个返回值都 包装为Result<?>的功能
        handlers.add(new ReturnResolver());
    }*/
}
