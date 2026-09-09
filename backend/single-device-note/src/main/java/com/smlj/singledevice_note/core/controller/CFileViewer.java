package com.smlj.singledevice_note.core.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import com.smlj.singledevice_note.core.service.FileViewerService;
import com.smlj.singledevice_note.core.service.FileViewerService.RawContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件预览接口 (fileview 页面专用)
 * 与 CMinio(filetest) 相互独立：
 * - GET /x/fileview/list      → 可预览文件列表
 * - GET /x/fileview/url/{id}  → MinIO 预签名 GET 直链(首选: 前端直接访问 MinIO 预览)
 * - GET /x/fileview/raw/{id}  → 同源字节流(降级通道: 直链受 CORS/网络限制时,前端 fetch→Blob 预览)
 *
 * 说明: 该路径不在 AccessInterceptor(/cghtz/**) 拦截范围，无需登录态。
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

    /**
     * 同源字节流(降级通道): 给前端 fetch→Blob 预览, 绕过客户端下载加速器拦截
     *
     * 关键设计:
     * 1) Content-Type 固定返回 application/octet-stream ——
     *    客户端若装了 IDM / FDM / EagleGet 等下载加速器, 默认会按标准 MIME
     *    (application/pdf / video/mp4 / application/zip 等) 自动拦截响应并弹下载框;
     *    用 octet-stream 绕过这类默认抓取规则。
     * 2) Content-Disposition 用 inline + 文件名 preview.bin —— .bin 不在 IDM 默认文件类型白名单,
     *    inline 告诉浏览器内联渲染(浏览器实际不会渲染, 该响应只用于前端 axios 拉成 Blob)。
     * 3) Open File Viewer 仍能正确识别格式: 前端把真实 fileName (xxx.pdf) 作为 prop 传入,
     *    viewer 按扩展名路由到 pdfPlugin; pdfjs 解析 Blob 不依赖 Content-Type, 通过 %PDF- 签名识别。
     * 4) 完整流程: 前端 fetch(/raw) → axios 拿到 Blob → createObjectURL → 喂给 viewer → pdfjs 渲染。
     */
    @GetMapping("/raw/{fileId}")
    public ResponseEntity<InputStreamResource> raw(@PathVariable String fileId) {
        RawContent rc = fileViewerService.openRaw(fileId);
        log.info("打开文件流: fileId={}", fileId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("preview.bin", StandardCharsets.UTF_8).build().toString())
                .body(new InputStreamResource(rc.stream()));
    }
}
