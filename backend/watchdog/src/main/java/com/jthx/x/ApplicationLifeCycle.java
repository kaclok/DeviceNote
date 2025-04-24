package com.jthx.x;

import com.jthx.x.cms.watchdog.Detector.ExceptionDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 应用已完全启动
        System.out.println("...应用已完全就绪，开始执行初始化逻辑...");
        // 你的业务代码
    }
}
