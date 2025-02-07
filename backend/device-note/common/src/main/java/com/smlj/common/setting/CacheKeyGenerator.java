package com.smlj.common.setting;

import jakarta.validation.constraints.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

// springcache默认的连接符是::, 而redis是:,所以设置都是:
/*@Component
public class CacheKeyGenerator implements KeyGenerator {
    @NotNull
    @Override
    public Object generate(@NotNull Object target, @NotNull Method method, @NotNull Object... params) {
        StringBuilder key = new StringBuilder();
        if (params.length > 0) {
            String cacheName = getCacheNameFromAnnotation(method); // 假设这里有方法能获取到 @Cacheable 注解里的 cacheNames 值
            key.append(cacheName).append(":");
            for (Object param : params) {
                key.append(param);
                key.append(":");
            }
        }
        return key.toString();
    }

    private String getCacheNameFromAnnotation(Method method) {
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        if (cacheable != null && cacheable.cacheNames().length > 0) {
            return cacheable.cacheNames()[0];
        }
        return "";
    }
}*/
