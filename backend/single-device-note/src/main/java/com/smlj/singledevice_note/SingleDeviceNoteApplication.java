package com.smlj.singledevice_note;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@EnableCaching
@EnableAsync
@EnableWebMvc
// 有了 多数据源 和动态台数据源 需要屏蔽默认的数据源配置形式
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class/*, SecurityAutoConfiguration.class*/})
/*@ComponentScan({"com.smlj.singledevice_note.logic.o.vo.table", "com.smlj.singledevice_note.logic.controller"})*/
public class SingleDeviceNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(SingleDeviceNoteApplication.class, args);
    }
}
