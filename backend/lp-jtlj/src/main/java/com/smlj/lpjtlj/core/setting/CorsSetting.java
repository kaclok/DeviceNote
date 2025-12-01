package com.smlj.lpjtlj.core.setting;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;

@Data
@Configuration
@Component
@ConfigurationProperties(prefix = "cors")
// https://mp.weixin.qq.com/s/_RTTH0NnRyIabtbX1bkz7g SpringBoot如何解决跨域问题？
public class CorsSetting {
    private String[] allowedOrigin;

    // https://www.bilibili.com/video/BV13z4y1F717/?spm_id_from=333.337.search-card.all.click&vd_source=5c9f5bd891aee351c325bcf632b5550f
    /*关于cors的理解：必须是浏览器支持、浏览器和服务器的交互的情况下才可能需要cors
    日报系统，浏览器访问 前端js代码，前端进程从后端服务器取数据的工作流程，其实是浏览器通过url访问到前端进程下发给浏览器的html和js代码，然后浏览器运行js代码，同时确定了源就是前端进程
    然后某个js请求需要前端进程请求后端服务器资源的时候，因为这个请求是从浏览器发起的（此时前端进程已经将js的代码下发给了浏览器），后端会将资源下发给浏览器，浏览器检查（浏览器不支持就不会检查这个头）下发http是或否包含allowcontrolorigin头
    且头的内容是否包含源（也就是前端进程的ip:端口），包含的话提交资源给浏览器渲染进程，否则的话拒绝渲染。

    此时cors是如何生效的：
    方法1：后端给所有下发的http头添加的allowcontrolorigin头，且头的内容包含了前端进程这个源
    方法2：避开浏览器和后端服务器的直接交互，中间添加一个nginx的代理，也就是浏览器通过nginx和后端服务器以及前端进程交互*/
    // https://www.iocoder.cn/Spring-Boot/SpringMVC/?yudao 8. Cors 跨域
    // https://blog.csdn.net/qq_37896194/article/details/102833430
    @Bean
    public org.springframework.web.filter.CorsFilter corsFilter() {
        // 1. 添加 CORS 配置信息
        CorsConfiguration config = new CorsConfiguration();

        // config.addAllowedOrigin("*");会导致：java.lang.IllegalArgumentException: When allowCredentials is true, allowedOrigins cannot contain the special value "*" since that cannot be set on the "Access-Control-Allow-Origin" response header. To allow credentials to a set of origins, list them explicitly or consider using "allowedOriginPatterns" instead.
        // config.addAllowedOrigin("*");

        var patterns = new ArrayList<String>();
        patterns.add("*");
        config.setAllowedOriginPatterns(patterns);

        // 1.1 允许的源
        if (allowedOrigin != null) {
            for (var i : allowedOrigin) {
                config.addAllowedOrigin(i);
            }
        }
        // 1.2 允许的请求方法
        config.addAllowedMethod(HttpMethod.GET);
        config.addAllowedMethod(HttpMethod.POST);
        config.addAllowedMethod(HttpMethod.DELETE);
        // 1.3 允许的请求头
        config.addAllowedHeader("*");
        // 1.4 允许携带证书
        config.setAllowCredentials(true);
        // config.setMaxAge(1800L); // 有效期 1800 秒，2 小时

        // 2. 创建 UrlBasedCorsConfigurationSource 对象
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 3. 为特定的 URL 路径注册 CORS 配置
        source.registerCorsConfiguration("/x/**", config);

        // 4. 返回 CorsFilter
        return new org.springframework.web.filter.CorsFilter(source);
    }
}
