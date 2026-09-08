package com.smlj.singledevice_note.core.controller;

import com.smlj.singledevice_note.core.o.dto.file.FileUploadInitDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadResultDTO;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文件管理接口
 * 上传流程: POST /init → 前端直传 MinIO → POST /complete/{fileId}
 * 下载流程: GET /download/{fileId} → 前端直连 MinIO 下载
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/minio")
public class CMinio {

    private final FileService fileService;

    /**
     * 初始化上传 - 获取预签名上传地址
     */
    @PostMapping("/init")
    public Result<FileUploadResultDTO> initUpload(@Valid @RequestBody FileUploadInitDTO dto) {
        log.info("初始化上传请求: {}", dto.getOriginal_name());
        return Result.success(fileService.initUpload(dto));
    }

    /**
     * 确认上传完成
     */
    @PostMapping("/complete/{fileId}")
    public Result<Void> completeUpload(@PathVariable String fileId) {
        log.info("确认上传完成: {}", fileId);
        fileService.completeUpload(fileId);
        return Result.success();
    }

    /**
     * 获取下载地址
     */
    @GetMapping("/download/{fileId}")
    public Result<String> getDownloadUrl(@PathVariable String fileId) {
        log.info("获取下载地址: {}", fileId);
        return Result.success(fileService.getDownloadUrl(fileId));
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete/{fileId}")
    public Result<Void> deleteFile(@PathVariable String fileId) {
        log.info("删除文件: {}", fileId);
        fileService.deleteFile(fileId);
        return Result.success();
    }
}
