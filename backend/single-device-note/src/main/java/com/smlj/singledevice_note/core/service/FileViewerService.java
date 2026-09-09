package com.smlj.singledevice_note.core.service;

import com.smlj.singledevice_note.core.exception.BizException;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.o.vo.table.dao.TFileInfoDao;
import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import com.smlj.singledevice_note.core.utils.MinoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * 文件预览业务逻辑层 (fileview 页面专用)
 * 职责:
 *  - list:         列出可预览文件
 *  - getPreviewUrl: 校验记录后签发 MinIO 预签名 GET 直链(首选: 浏览器直接访问 MinIO)
 *  - openRaw:      打开同源字节流(降级通道: 直链受 CORS/网络限制时,前端 Blob 预览用)
 *
 * 预览数据流:
 *   首选: 前端拿直链 → 浏览器直接访问 MinIO(前提: 浏览器可达 MinIO 且 CORS 放行)。
 *   降级: 直链 fetch 失败 → 前端改请求本接口(同源) → 拉字节为 Blob → 交给 viewer,
 *         与上传(预签名 PUT 直传)同源同机制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileViewerService {

    /** 预签名 URL 有效期: 30 分钟 */
    private static final int PRESIGN_EXPIRE_SECONDS = 30 * 60;

    private final MinoUtil minoUtil;
    private final TFileInfoDao fileDao;

    /**
     * 查询可预览文件列表(已上传完成 status=1，按时间倒序)
     */
    public List<TFileInfo> list() {
        return fileDao.queryValidList();
    }

    /**
     * 生成预览直链: 校验记录状态 → 推断 Content-Type → MinIO 预签名 GET URL
     * 失败抛 BizException(ResultCode)，由 GlobalExceptionHandler 统一处理
     */
    public String getPreviewUrl(String fileId) {
        TFileInfo entity = requireValid(fileId);
        try {
            return minoUtil.generatePresignedPreviewUrl(
                    entity.getObject_key(), resolveContentType(entity), PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成预览URL失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            throw new BizException(ResultCode.RC10505);
        }
    }

    /** 同源降级通道返回载体: 字节流 + 响应元信息 */
    public record RawContent(InputStream stream, String contentType, String fileName) {
    }

    /**
     * 打开文件字节流(同源降级通道，供前端 fetch → Blob 预览)
     * 校验逻辑与 getPreviewUrl 完全一致; 调用方负责关闭 stream
     */
    public RawContent openRaw(String fileId) {
        TFileInfo entity = requireValid(fileId);
        try {
            InputStream in = minoUtil.getObject(entity.getObject_key());
            String name = (entity.getOriginal_name() == null || entity.getOriginal_name().isBlank())
                    ? "file" : entity.getOriginal_name();
            return new RawContent(in, resolveContentType(entity), name);
        } catch (Exception e) {
            log.error("读取文件流失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            throw new BizException(ResultCode.RC10507);
        }
    }

    /**
     * 公共校验: 记录存在 且 状态为已上传完成(1)
     */
    private TFileInfo requireValid(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            throw new BizException(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 2) {
            throw new BizException(ResultCode.RC10504);
        }
        if (entity.getUpload_status() != 1) {
            throw new BizException(ResultCode.RC10502);
        }
        return entity;
    }

    /**
     * 推断 Content-Type: 优先取元数据(排除无意义的 octet-stream)，否则按文件名后缀猜测
     */
    private String resolveContentType(TFileInfo entity) {
        String stored = entity.getContent_type();
        boolean useless = stored == null || stored.isBlank()
                || "application/octet-stream".equalsIgnoreCase(stored)
                || "binary/octet-stream".equalsIgnoreCase(stored);
        if (!useless) {
            return stored;
        }
        Optional<org.springframework.http.MediaType> mt =
                MediaTypeFactory.getMediaType(entity.getOriginal_name() == null ? "" : entity.getOriginal_name());
        return mt.map(org.springframework.http.MediaType::toString).orElse("application/octet-stream");
    }
}
