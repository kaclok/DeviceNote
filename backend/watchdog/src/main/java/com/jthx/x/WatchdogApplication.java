package com.jthx.x;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableCaching
@EnableAsync
@EnableWebMvc
// 有了 多数据源 和动态台数据源 需要屏蔽默认的数据源配置形式
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class/*, SecurityAutoConfiguration.class*/})
/*@ComponentScan({"com.smlj.watchdog.logic.o.vo.table", "com.smlj.watchdog.logic.controller"})*/
public class WatchdogApplication {
    public static void main(String[] args) {
        SpringApplication.run(WatchdogApplication.class, args);
    }
}
