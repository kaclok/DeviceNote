package com.smlj.singledevice_note.core.controller;

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
 * 下载流程: GET /download → 前端直连 MinIO 下载
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
    public Result<Void> endUpload(@RequestParam String file_id) {
        log.info("结束上传: {}", file_id);
        fileService.endUpload(file_id);
        return Result.success();
    }

    /**
     * 获取下载地址
     */
    @PostMapping("/download")
    public Result<String> getDownloadUrl(@RequestParam String file_id) {
        log.info("获取下载地址: {}", file_id);
        return Result.success(fileService.getDownloadUrl(file_id));
    }

    /**
     * 删除文件
     */
    @PostMapping("/delete")
    public Result<Void> deleteFile(@RequestParam String file_id) {
        log.info("删除文件: {}", file_id);
        fileService.deleteFile(file_id);
        return Result.success();
    }
}
