package com.smlj.singledevice_note.core.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import com.smlj.singledevice_note.core.service.FileViewerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件预览接口 (fileview 页面专用)
 * 与 CMinio(filetest) 相互独立：
 * - GET /x/fileview/list      → 可预览文件列表
 * - GET /x/fileview/url/{id}  → MinIO 预签名 GET 直链，前端拿 URL 后浏览器直接访问 MinIO 预览
 *
 * 说明: 预览数据流与上传一致(浏览器直连 MinIO)，故不再由后端代理文件字节流。
 * 该路径不在 AccessInterceptor(/cghtz/**) 拦截范围，无需登录态。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/fileview")
public class CFileViewer {

    private final FileViewerService fileViewerService;

    /**
     * 可预览文件列表
     */
    @GetMapping("/list")
    public Result<List<TFileInfo>> list() {
        List<TFileInfo> list = fileViewerService.list();
        log.info("查询可预览文件列表: {}", list.size());
        return Result.success(list);
    }

    /**
     * 预览直链: MinIO 预签名 GET URL(30分钟有效)，前端直接访问 MinIO
     */
    @GetMapping("/url/{fileId}")
    public Result<String> url(@PathVariable String fileId) {
        String url = fileViewerService.getPreviewUrl(fileId);
        log.info("生成预览直链: fileId={}", fileId);
        return Result.success(url);
    }
}
