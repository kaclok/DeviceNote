package com.smlj.singledevice_note.core.controller;

import com.smlj.singledevice_note.core.o.dto.file.FileChunkBeginDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileChunkBeginResultDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadBeginDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadResultDTO;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 文件管理接口
 * 上传流程: POST /begin → 前端直传 MinIO → POST /end
 * 分块上传: POST /chunk/begin → 前端分块直传 MinIO → POST /chunk/complete(服务端合并)
 * 下载流程: POST /download → 前端直连 MinIO 下载(可 Range 分块并发)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/minio")
public class CMinio {

    private final FileService fileService;

    /**
     * 开始上传 - 获取预签名上传地址
     */
    @PostMapping("/begin")
    public Result<FileUploadResultDTO> beginUpload(@Valid @RequestBody FileUploadBeginDTO dto) {
        log.info("开始上传请求: {}", dto.getOriginal_name());
        return Result.success(fileService.beginUpload(dto));
    }

    /**
     * 结束上传
     */
    @PostMapping("/end")
    public Result<Void> endUpload(@RequestBody String file_id) {
        log.info("结束上传: {}", file_id);
        fileService.endUpload(file_id);
        return Result.success();
    }

    /**
     * 开始分块上传 - 获取每个分块的预签名 PUT 直链(前端并发直传 MinIO)
     */
    @PostMapping("/chunk/begin")
    public Result<FileChunkBeginResultDTO> beginChunkUpload(@Valid @RequestBody FileChunkBeginDTO dto) {
        log.info("开始分块上传请求: {}", dto.getOriginal_name());
        return Result.success(fileService.beginChunkUpload(dto));
    }

    /**
     * 结束分块上传 - 后端触发 MinIO 服务端合并分块并登记完成
     */
    @PostMapping("/chunk/complete")
    public Result<Void> completeChunkUpload(@RequestBody String file_id) {
        log.info("结束分块上传: {}", file_id);
        fileService.completeChunkUpload(file_id);
        return Result.success();
    }

    /**
     * 中止分块上传 - 清理已上传的临时分块与 DB 记录
     */
    @PostMapping("/chunk/abort")
    public Result<Void> abortChunkUpload(@RequestBody String file_id) {
        log.info("中止分块上传: {}", file_id);
        fileService.abortChunkUpload(file_id);
        return Result.success();
    }

    /**
     * 获取下载地址(预签名 GET 直链，前端可 Range 分块并发下载)
     */
    @PostMapping("/download")
    public Result<String> getDownloadUrl(@RequestBody String file_id) {
        log.info("获取下载地址: {}", file_id);
        return Result.success(fileService.getDownloadUrl(file_id));
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete")
    public Result<Void> deleteFile(@RequestBody String file_id) {
        log.info("删除文件: {}", file_id);
        fileService.realDeleteFile(file_id);
        return Result.success();
    }
}
