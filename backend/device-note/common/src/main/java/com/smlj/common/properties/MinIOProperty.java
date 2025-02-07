package com.smlj.common.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Data
@Configuration
public class MinIOProperty {
    @Value("${minio.config.url}")
    private String url;

    @Value("${minio.config.accessKey}")
    private String accessKey;

    @Value("${minio.config.secretKey}")
    private String secretKey;

    @Value("${minio.config.secure}")
    private boolean secure;

    @Value("${minio.config.bucketName}")
    private String bucketName;

    public String getPrefixUrl() {
        return url + "/" + bucketName;
    }
}
