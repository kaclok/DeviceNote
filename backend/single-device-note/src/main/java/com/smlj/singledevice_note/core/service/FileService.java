package com.smlj.singledevice_note.core.service;

import com.smlj.singledevice_note.core.o.dto.file.FileUploadInitDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileUploadResultDTO;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import com.smlj.singledevice_note.core.o.vo.table.dao.TFileInfoDao;
import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import com.smlj.singledevice_note.core.utils.MinoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 文件业务逻辑层
 * 上传流程: initUpload → 前端直传 MinIO → completeUpload
 * 下载流程: getDownloadUrl → 前端直连 MinIO 下载
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
     * 初始化上传: 生成 fileId + presigned upload URL，存文件元数据到 DB
     * 如果 MD5 已存在则返回 isExist=true（秒传）
     */
    public Result<FileUploadResultDTO> initUpload(FileUploadInitDTO dto) {
        // 秒传检查
        TFileInfo existing = fileDao.queryByMd5(dto.getFileMd5());
        if (existing != null && existing.getUpload_status() == 1) {
            log.info("秒传命中: fileId={}, objectKey={}", existing.getId(), existing.getObject_key());
            FileUploadResultDTO result = new FileUploadResultDTO();
            result.setFileId(existing.getId());
            result.setIsExist(true);
            return Result.success(result);
        }

        // 生成 fileId + objectKey
        String fileId = UUID.randomUUID().toString().replace("-", "");
        String objectKey = fileId + "/" + dto.getOriginalName();

        // 存 DB
        TFileInfo entity = new TFileInfo();
        entity.setId(fileId);
        entity.setOriginal_name(dto.getOriginalName());
        entity.setObject_key(objectKey);
        entity.setFile_size(dto.getFileSize());
        entity.setFile_md5(dto.getFileMd5());
        entity.setUpload_status(0);
        fileDao.insert(entity);

        // 生成 presigned upload URL
        String uploadUrl;
        try {
            uploadUrl = minoUtil.generatePresignedUploadUrl(objectKey, PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成上传URL失败: fileId={}, objectKey={}", fileId, objectKey, e);
            return Result.fail(ResultCode.RC10505);
        }

        FileUploadResultDTO result = new FileUploadResultDTO();
        result.setFileId(fileId);
        result.setUploadUrl(uploadUrl);
        result.setIsExist(false);
        return Result.success(result);
    }

    /**
     * 确认上传完成: 校验 MinIO 对象存在 → 更新 DB status=1
     */
    public Result<Void> completeUpload(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            return Result.fail(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 2) {
            return Result.fail(ResultCode.RC10504);
        }
        if (entity.getUpload_status() == 1) {
            log.info("文件已确认上传过，跳过: {}", fileId);
            return Result.success();
        }
        boolean exists = minoUtil.doesObjectExist(entity.getObject_key());
        if (!exists) {
            return Result.fail(ResultCode.RC10503);
        }
        fileDao.updateUploadStatus(fileId, 1);
        log.info("文件上传确认完成: fileId={}, objectKey={}", fileId, entity.getObject_key());
        return Result.success();
    }

    /**
     * 获取下载 URL: 生成 presigned download URL
     */
    public Result<String> getDownloadUrl(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            return Result.fail(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 2) {
            return Result.fail(ResultCode.RC10504);
        }
        if (entity.getUpload_status() != 1) {
            return Result.fail(ResultCode.RC10502);
        }
        String downloadUrl;
        try {
            downloadUrl = minoUtil.generatePresignedDownloadUrl(entity.getObject_key(), PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成下载URL失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            return Result.fail(ResultCode.RC10505);
        }
        return Result.success(downloadUrl);
    }

    /**
     * 删除文件: MinIO 删除对象 + DB 软删除
     */
    public Result<Void> deleteFile(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            return Result.fail(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 1) {
            try {
                minoUtil.deleteObject(entity.getObject_key());
            } catch (Exception e) {
                log.error("MinIO删除文件失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
                return Result.fail(ResultCode.RC10508);
            }
        }
        fileDao.markDeleted(fileId);
        log.info("文件删除完成: fileId={}, objectKey={}", fileId, entity.getObject_key());
        return Result.success();
    }
}
