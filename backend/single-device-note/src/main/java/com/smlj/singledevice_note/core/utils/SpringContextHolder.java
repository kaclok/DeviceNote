package com.smlj.singledevice_note.core.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

// 自己new的类，或者其他插件创建的不被springboot接管的类，可以通过getBean被springboot接管。
@Component
public class SpringContextHolder implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }

    // 也可以手动一个类一个类创建
    /*@Configuration
    public class MyConfig {
        @Bean
        public MyCustomObject customObject() {
            return new MyCustomObject();  // 这也是 new，但返回后由 Spring 注册
        }
    }*/
}

/*
demo:
    var myService = SpringContextHolder.getBean(MyService.class);
    myService.doSomething();
*/
