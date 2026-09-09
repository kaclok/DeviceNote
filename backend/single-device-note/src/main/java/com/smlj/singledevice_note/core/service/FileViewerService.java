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

import java.util.List;
import java.util.Optional;

/**
 * 文件预览业务逻辑层 (fileview 页面专用)
 * 职责:
 *  - list:         列出可预览文件
 *  - getPreviewUrl: 校验记录后签发 MinIO 预签名 GET 直链
 *
 * 预览数据流: 前端拿直链 → 浏览器直接访问 MinIO。
 * 与上传(预签名 PUT 直传)同源同机制，前提是桶已配置允许前端源的 CORS。
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
        try {
            return minoUtil.generatePresignedPreviewUrl(
                    entity.getObject_key(), resolveContentType(entity), PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成预览URL失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            throw new BizException(ResultCode.RC10505);
        }
    }

    /**
     * 推断 Content-Type: 优先取元数据，否则按文件名后缀猜测
     */
    private String resolveContentType(TFileInfo entity) {
        if (entity.getContent_type() != null && !entity.getContent_type().isBlank()) {
            return entity.getContent_type();
        }
        Optional<org.springframework.http.MediaType> mt =
                MediaTypeFactory.getMediaType(entity.getOriginal_name() == null ? "" : entity.getOriginal_name());
        return mt.map(org.springframework.http.MediaType::toString).orElse("application/octet-stream");
    }
}
