package com.smlj.devicenote;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Slf4j
@EnableAsync
@EnableWebMvc
// @EnableCaching
// 导入Gson https://springdoc.cn/spring-boot-gson/
// 有了 多数据源 和动态台数据源 需要屏蔽默认的数据源配置形式
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableAspectJAutoProxy(exposeProxy = true) // http://www.voidcn.com/article/p-zddcuyii-bpt.html
@MapperScan({"com.smlj.devicenote.logic.o.vo.table.dao"})
public class DeviceNoteApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeviceNoteApplication.class, args);
    }
}
