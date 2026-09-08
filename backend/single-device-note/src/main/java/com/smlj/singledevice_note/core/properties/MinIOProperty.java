package com.smlj.singledevice_note.core.properties;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Slf4j
@Data
@Configuration
@ConfigurationProperties
public class MinIOProperty {
    @Value("${minio.config.url}")
    private String url;

    @Value("${minio.config.accessKey}")
    private String accessKey;

    @Value("${minio.config.secretKey}")
    private String secretKey;

    @Value("${minio.config.secure}")
    private boolean secure = false;

    @Value("${minio.config.bucketName}")
    private String bucketName;

    @Bean
    public MinioClient getCli() {
        return MinioClient.builder().endpoint(url).credentials(accessKey, secretKey).build();
    }

    public String getPrefixUrl() {
        return url + "/" + bucketName;
    }
}
