package com.smlj.singledevice_note.core.utils;

import com.smlj.singledevice_note.core.properties.MinIOProperty;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinoUtil {
    private final MinioClient minioClient;
    private final MinIOProperty minioConfig;

    /**
     * 生成预签名上传URL
     */
    public String generatePresignedUploadUrl(String objectKey, int expireSeconds) {
        try {
            ensureBucketExists();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成上传URL失败", e);
            throw new RuntimeException("生成上传URL失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名下载URL
     */
    public String generatePresignedDownloadUrl(String objectKey, int expireSeconds) {
        try {
            ensureBucketExists();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成下载URL失败", e);
            throw new RuntimeException("生成下载URL失败: " + e.getMessage());
        }
    }

    /**
     * 检查对象是否存在
     */
    public boolean doesObjectExist(String objectKey) {
        try {
            ensureBucketExists();
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 上传文件（直接流上传，不推荐大文件使用）
     */
    public void uploadFile(String objectKey, InputStream inputStream, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .stream(inputStream, (long) inputStream.available(), -1L)
                            .contentType(contentType)
                            .build()
            );
            log.info("文件上传成功: {}", objectKey);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件
     */
    public void deleteObject(String objectKey) {
        try {
            ensureBucketExists();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .build()
            );
            log.info("删除文件成功: {}", objectKey);
        } catch (Exception e) {
            log.error("删除文件失败", e);
            throw new RuntimeException("删除文件失败: " + e.getMessage());
        }
    }

    /**
     * 确保Bucket存在，不存在则创建
     */
    private void ensureBucketExists() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("创建Bucket成功: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("检查/创建Bucket失败", e);
            throw new RuntimeException("检查/创建Bucket失败: " + e.getMessage());
        }
    }
}
