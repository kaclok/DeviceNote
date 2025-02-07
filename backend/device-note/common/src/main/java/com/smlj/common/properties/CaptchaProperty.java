package com.smlj.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

// https://mp.weixin.qq.com/s/xYY7gcIywAImwSsgkRs5kQ
@Data
@Component // 标识为Bean
@Configuration
@ConfigurationProperties(prefix = "smlj.captcha")
@PropertySource(value = "classpath:smlj.yml", factory = YamlPropertySourceFactory.class, ignoreResourceNotFound = false, encoding = "UTF-8") // 配置文件路径
public class CaptchaProperty {
    private Integer width;
    private Integer height;
    private Integer codeCount;
    private Integer lineCount;

    private Session session; //使用自定义的Session类

    @Data
    public static class Session {
        private String key;
        private String date;
    }
}
