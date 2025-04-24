package com.jthx.x.cms.watchdog.service;

import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringEndpointConfigurator extends ServerEndpointConfig.Configurator implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        // 从 Spring 容器中获取 WebSocket 实例，使其支持 @Autowired
        return context.getBean(endpointClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}