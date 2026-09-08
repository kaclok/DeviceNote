package com.smlj.singledevice_note.core.service;

import com.smlj.singledevice_note.core.exception.BizException;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadBeginDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadResultDTO;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.o.vo.table.dao.TFileInfoDao;
import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import com.smlj.singledevice_note.core.utils.MinoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 文件业务逻辑层
 * 上传流程: beginUpload → 前端直传 MinIO → endUpload
 * 下载流程: getDownloadUrl → 前端直连 MinIO 下载
 *
 * 约定: Service 只返回基础对象，失败抛 BizException(ResultCode)，由 GlobalExceptionHandler 统一捕获
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final MinoUtil minoUtil;
    private final TFileInfoDao fileDao;

    /** 预签名 URL 有效期: 30 分钟 */
    private static final int PRESIGN_EXPIRE_SECONDS = 30 * 60;

    /**
     * 开始上传: 生成 fileId + presigned upload URL，存文件元数据到 DB
     * 如果 MD5 已存在则返回 is_exist=true（秒传）
     */
    public FileUploadResultDTO beginUpload(FileUploadBeginDTO dto) {
        // 查询是否已存在同 MD5 的有效记录（排除已删除 status=2）
        TFileInfo existing = fileDao.queryByMd5(dto.getFile_md5());

        // 秒传：已存在且已上传完成（status=1）
        if (existing != null && existing.getUpload_status() == 1) {
            log.info("秒传命中: fileId={}, objectKey={}", existing.getId(), existing.getObject_key());
            FileUploadResultDTO result = new FileUploadResultDTO();
            result.setFile_id(existing.getId());
            result.setIs_exist(true);
            return result;
        }

        // 断点续传：已存在但未上传完成（status=0），复用旧记录，重新生成 presigned URL
        if (existing != null && existing.getUpload_status() == 0) {
            log.info("断点续传: fileId={}, objectKey={}", existing.getId(), existing.getObject_key());
            return resumeUpload(existing);
        }

        // 新文件：生成 fileId + objectKey
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = fileId + "/" + dto.getOriginal_name();

        // 存 DB（并发兜底：同 MD5 唯一索引冲突时复用已有记录）
        TFileInfo entity = new TFileInfo();
        entity.setId(fileId);
        entity.setOriginal_name(dto.getOriginal_name());
        entity.setObject_key(objectKey);
        entity.setFile_size(dto.getFile_size());
        entity.setFile_md5(dto.getFile_md5());
        entity.setUpload_status(0);
        try {
            fileDao.insert(entity);
        } catch (DuplicateKeyException e) {
            // 并发上传同一文件：另一请求已先插入，重查复用其记录走断点续传
            log.info("并发上传冲突，复用已有记录: md5={}", dto.getFile_md5());
            TFileInfo concurrent = fileDao.queryByMd5(dto.getFile_md5());
            if (concurrent == null) {
                log.error("并发冲突后重查未找到记录: md5={}", dto.getFile_md5());
                throw new BizException(ResultCode.RC10506);
            }
            return resumeUpload(concurrent);
        }

        return resumeUpload(entity);
    }

    /**
     * 断点续传：复用已有未完成记录，重新生成 presigned upload URL
     */
    private FileUploadResultDTO resumeUpload(TFileInfo existing) {
        String uploadUrl = generateUploadUrlOrFail(existing.getId(), existing.getObject_key());
        FileUploadResultDTO result = new FileUploadResultDTO();
        result.setFile_id(existing.getId());
        result.setUpload_url(uploadUrl);
        result.setIs_exist(false);
        return result;
    }

    /**
     * 生成预签名上传 URL，失败抛 BizException(RC10505)
     */
    private String generateUploadUrlOrFail(String fileId, String objectKey) {
        try {
            return minoUtil.generatePresignedUploadUrl(objectKey, PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成上传URL失败: fileId={}, objectKey={}", fileId, objectKey, e);
            throw new BizException(ResultCode.RC10505);
        }
    }

    /**
     * 结束上传: 校验 MinIO 对象存在 → 更新 DB status=1
     */
    public void endUpload(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            throw new BizException(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 2) {
            throw new BizException(ResultCode.RC10504);
        }
        if (entity.getUpload_status() == 1) {
            log.info("文件已确认上传过，跳过: {}", fileId);
            return;
        }
        boolean exists = minoUtil.doesObjectExist(entity.getObject_key());
        if (!exists) {
            throw new BizException(ResultCode.RC10503);
        }
        fileDao.updateUploadStatus(fileId, 1);
        log.info("文件上传确认完成: fileId={}, objectKey={}", fileId, entity.getObject_key());
    }

    /**
     * 获取下载 URL: 生成 presigned download URL
     */
    public String getDownloadUrl(String fileId) {
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
            return minoUtil.generatePresignedDownloadUrl(entity.getObject_key(), PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成下载URL失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            throw new BizException(ResultCode.RC10505);
        }
    }

    /**
     * 删除文件: MinIO 删除对象 + DB 软删除
     */
    public void deleteFile(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            throw new BizException(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 1) {
            try {
                minoUtil.deleteObject(entity.getObject_key());
            } catch (Exception e) {
                log.error("MinIO删除文件失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
                throw new BizException(ResultCode.RC10508);
            }
        }
        fileDao.markDeleted(fileId);
        log.info("文件删除完成: fileId={}, objectKey={}", fileId, entity.getObject_key());
    }
}
