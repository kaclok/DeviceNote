package com.smlj.singledevice_note.core.utils;

import com.smlj.singledevice_note.core.properties.MinIOProperty;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
     * 生成预签名预览URL（GET 直链，浏览器直接访问 MinIO）
     * 说明:
     * 1. 上传时对象 Content-Type 统一为 octet-stream，这里用 response-content-type 覆盖为真实类型，
     *    保证浏览器/viewer 能按正确 MIME 内联预览；
     * 2. 仅覆盖 content-type，不写 response-content-disposition，避免中文文件名 header 编码问题。
     */
    public String generatePresignedPreviewUrl(String objectKey, String contentType, int expireSeconds) {
        try {
            ensureBucketExists();
            Map<String, String> params = new HashMap<>();
            if (contentType != null && !contentType.isBlank()) {
                params.put("response-content-type", contentType);
            }
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(minioConfig.getBucketName())
                            .object(objectKey)
                            .expiry(expireSeconds, TimeUnit.SECONDS)
                            .extraQueryParams(params)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成预览URL失败: {}", objectKey, e);
            throw new RuntimeException("生成预览URL失败: " + e.getMessage());
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

    // ==================== 分块上传/下载支撑 ====================

    /** MinIO 对象概要(分块合并/清理用) */
    public record MinoObject(String objectName, long size) {
    }

    /**
     * 按前缀列出对象(含名称与大小)
     * 用于: 分块完整校验、中止上传时按前缀清理临时分块
     */
    public List<MinoObject> listObjects(String prefix) {
        List<MinoObject> list = new ArrayList<>();
        try {
            ensureBucketExists();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                list.add(new MinoObject(item.objectName(), item.size()));
            }
        } catch (Exception e) {
            log.error("列出对象失败: prefix={}", prefix, e);
            throw new RuntimeException("列出对象失败: " + e.getMessage());
        }
        return list;
    }

    /**
     * 服务端合并分块: 将 srcObjectKeys 按顺序在 MinIO 内部合并为 destObjectKey。
     * 数据仅在 MinIO 存储层搬运，不经后端进程/网络转发。
     */
    public void composeObjects(String destObjectKey, List<String> srcObjectKeys) {
        try {
            ensureBucketExists();
            List<ComposeSource> sources = srcObjectKeys.stream()
                    .map(k -> ComposeSource.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(k)
                            .build())
                    .collect(Collectors.toList());
            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(destObjectKey)
                            .sources(sources)
                            .build()
            );
            log.info("分块合并成功: dest={}, srcCount={}", destObjectKey, srcObjectKeys.size());
        } catch (Exception e) {
            log.error("分块合并失败: dest={}", destObjectKey, e);
            throw new RuntimeException("分块合并失败: " + e.getMessage());
        }
    }

    /**
     * 按前缀删除对象(幂等)
     * 用于: 中止/失败后清理分块临时对象
     */
    public void deleteObjectsByPrefix(String prefix) {
        List<MinoObject> objects = listObjects(prefix);
        if (objects.isEmpty()) {
            return;
        }
        for (MinoObject obj : objects) {
            deleteObject(obj.objectName());
        }
        log.info("按前缀清理对象完成: prefix={}, count={}", prefix, objects.size());
    }
}
