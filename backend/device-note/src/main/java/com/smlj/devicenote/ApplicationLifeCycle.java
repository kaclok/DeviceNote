package com.smlj.devicenote;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import java.util.Arrays;

// https://blog.csdn.net/study_study_know/article/details/111400695
// https://mp.weixin.qq.com/s/kN_H5zqcppuzgdmJVR_VVQ
@Slf4j
@Component
class ApplicationCloseListener implements ApplicationListener<ContextClosedEvent> {
    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
    }
}

@Slf4j
@Component
class ApplicationStartListener implements ApplicationListener<ContextStartedEvent> {
    @Resource
    private ApplicationArguments args;

    @Override
    public void onApplicationEvent(ContextStartedEvent event) {
        log.info("启动参数():" + (Arrays.toString(args.getSourceArgs())));
    }

    @PostConstruct
    public void start() {
    }
}

// https://mp.weixin.qq.com/s/GTE9U0qnF2Ezuq0HjINwHg
@Slf4j
@Component
class ServletTimeCountListener implements ApplicationListener<ServletRequestHandledEvent> {
    @Value("${smlj.servlet.showRequestDetail}")
    private boolean showRequestDetail;

    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        if (!showRequestDetail) {
            return;
        }

        Throwable failureCause = event.getFailureCause();
        if (failureCause != null) {
            // log.debug("错误原因: {}", failureCause.getMessage());
            System.err.printf("错误原因: %s%n", failureCause.getMessage());
        }

        // log.debug("请求信息：{}", event.getDescription());
        System.err.printf("请求信息：%s\n", event.getDescription());
    }
}