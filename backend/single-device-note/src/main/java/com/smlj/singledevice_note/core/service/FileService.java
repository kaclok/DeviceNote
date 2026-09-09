package com.smlj.singledevice_note.core.service;

import com.smlj.singledevice_note.core.exception.BizException;
import com.smlj.singledevice_note.core.o.dto.file.FileChunkBeginDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileChunkBeginResultDTO;
import com.smlj.singledevice_note.core.o.dto.file.FileChunkPartDTO;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    // ==================== 分块上传(前端直传 MinIO) ====================

    /** MinIO 服务端合并分块时，除最后一块外的单块下限(与 S3 5MB 规则一致) */
    private static final long MIN_CHUNK_SIZE = 5L * 1024 * 1024;
    /** 单次合并的分块上限(S3/MinIO compose 限制) */
    private static final int MAX_CHUNK_COUNT = 10000;
    /** 分块预签名 URL 有效期: 2 小时(分块上传耗时更长，放宽于整文件直传) */
    private static final int CHUNK_PRESIGN_EXPIRE_SECONDS = 2 * 60 * 60;
    /** 分块临时对象目录: {fileId}/parts/xxxxx */
    private static final String PART_DIR_PREFIX = "/parts/";

    /**
     * 分块上传 - 开始: 建/复用记录 + 为每个分块签发 MinIO 预签名 PUT 直链。
     * 数据流: 前端拿直链 → 并发直传 MinIO(不经后端) → completeChunkUpload 服务端合并。
     * 约定:
     *  1. file <= chunk_size 时不真正分块(单块直传最终对象键)，规避 compose 的 5MB 限制；
     *  2. 多块时每块(除末块)须 >= 5MB，否则 MinIO 服务端合并会拒绝；
     *  3. file_md5 为空则不走秒传/断点续传，每次新建记录。
     */
    public FileChunkBeginResultDTO beginChunkUpload(FileChunkBeginDTO dto) {
        long fileSize = dto.getFile_size();
        long chunkSize = dto.getChunk_size();
        long chunkCount = (fileSize + chunkSize - 1) / chunkSize;

        if (chunkCount > MAX_CHUNK_COUNT) {
            throw new BizException(ResultCode.RC10101,
                    "分块数量(" + chunkCount + ")超过上限" + MAX_CHUNK_COUNT + "，请调大分块大小");
        }
        if (chunkCount > 1 && chunkSize < MIN_CHUNK_SIZE) {
            throw new BizException(ResultCode.RC10101,
                    "需要分块时单块大小不能小于" + (MIN_CHUNK_SIZE / 1024 / 1024) + "MB(MinIO合并限制)");
        }

        TFileInfo entity = null;
        String md5 = dto.getFile_md5();
        boolean hasMd5 = md5 != null && !md5.isBlank();

        // 秒传/断点续传(与整文件上传同语义)
        if (hasMd5) {
            TFileInfo existing = fileDao.queryByMd5(md5);
            if (existing != null && existing.getUpload_status() == 1) {
                log.info("分块秒传命中: fileId={}", existing.getId());
                FileChunkBeginResultDTO hit = new FileChunkBeginResultDTO();
                hit.setFile_id(existing.getId());
                hit.setChunk_size(chunkSize);
                hit.setChunk_count(chunkCount);
                hit.setIs_exist(true);
                return hit;
            }
            entity = existing; // 未完成(status=0)则复用记录，断点续传
        }

        if (entity == null) {
            String fileId = UUID.randomUUID().toString().replace("-", "");
            TFileInfo fresh = new TFileInfo();
            fresh.setId(fileId);
            fresh.setOriginal_name(dto.getOriginal_name());
            fresh.setObject_key(fileId + "/" + dto.getOriginal_name());
            fresh.setFile_size(fileSize);
            fresh.setFile_md5(hasMd5 ? md5 : null);
            fresh.setUpload_status(0);
            try {
                fileDao.insert(fresh);
                entity = fresh;
            } catch (DuplicateKeyException e) {
                // 并发同 MD5: 复用已有记录
                TFileInfo concurrent = fileDao.queryByMd5(md5);
                if (concurrent == null) {
                    throw new BizException(ResultCode.RC10506);
                }
                entity = concurrent;
            }
        }

        String fileId = entity.getId();
        List<FileChunkPartDTO> parts = new ArrayList<>();
        if (chunkCount == 1) {
            // 单块直传最终对象键，等价于普通整文件上传
            // 断点续传探测: 最终对象已存在且大小一致 → exists=true，前端跳过直传直接 complete
            boolean exists = objectExistsWithSize(entity.getObject_key(), fileSize);
            parts.add(buildPart(0, generateChunkUrlOrFail(fileId, entity.getObject_key()), exists));
        } else {
            // 每块独立临时对象 {fileId}/parts/xxxxx
            String partsPrefix = fileId + PART_DIR_PREFIX;
            // 断点续传探测: 列出已上传的临时分块，与本次期望大小逐块比对。
            Map<String, Long> uploaded = new HashMap<>();
            for (MinoUtil.MinoObject obj : minoUtil.listObjects(partsPrefix)) {
                uploaded.put(obj.objectName(), obj.size());
            }
            if (!uploaded.isEmpty()) {
                // 残留块首块尺寸与本次 chunk_size 不一致(换了分块大小) → 旧块不可复用，清空重来
                String firstName = null;
                for (String name : uploaded.keySet()) {
                    if (firstName == null || name.compareTo(firstName) < 0) {
                        firstName = name;
                    }
                }
                if (firstName != null && uploaded.get(firstName) != chunkSize) {
                    minoUtil.deleteObjectsByPrefix(partsPrefix);
                    uploaded.clear();
                }
            }
            for (long i = 0; i < chunkCount; i++) {
                String partKey = fileId + PART_DIR_PREFIX + String.format("%05d", i);
                long expectSize = (i == chunkCount - 1) ? fileSize - (chunkCount - 1) * chunkSize : chunkSize;
                Long actual = uploaded.get(partKey);
                boolean exists = actual != null && actual == expectSize;
                parts.add(buildPart((int) i, generateChunkUrlOrFail(fileId, partKey), exists));
            }
        }

        FileChunkBeginResultDTO result = new FileChunkBeginResultDTO();
        result.setFile_id(fileId);
        result.setChunk_size(chunkSize);
        result.setChunk_count(chunkCount);
        result.setIs_exist(false);
        result.setParts(parts);
        log.info("分块上传开始: fileId={}, chunkCount={}, chunkSize={}", fileId, chunkCount, chunkSize);
        return result;
    }

    private FileChunkPartDTO buildPart(int index, String url, boolean exists) {
        FileChunkPartDTO part = new FileChunkPartDTO();
        part.setIndex(index);
        part.setUrl(url);
        part.setExists(exists);
        return part;
    }

    /**
     * 探测对象是否已完整存在(名称精确匹配且大小与期望一致)。
     * 用于单块直传场景的断点续传判定；多块场景走 listObjects(parts 前缀) 逐块比对。
     */
    private boolean objectExistsWithSize(String objectKey, long expectSize) {
        try {
            return minoUtil.listObjects(objectKey).stream()
                    .anyMatch(o -> o.objectName().equals(objectKey) && o.size() == expectSize);
        } catch (Exception e) {
            log.warn("单块续传探测失败: objectKey={}", objectKey, e);
            return false;
        }
    }

    /**
     * 生成分块预签名上传 URL(有效期放宽到 2 小时，大文件分块上传耗时更长)
     */
    private String generateChunkUrlOrFail(String fileId, String objectKey) {
        try {
            return minoUtil.generatePresignedUploadUrl(objectKey, CHUNK_PRESIGN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("生成分块上传URL失败: fileId={}, objectKey={}", fileId, objectKey, e);
            throw new BizException(ResultCode.RC10505);
        }
    }

    /**
     * 分块上传 - 结束: 校验分块完整 → MinIO 服务端合并 → 清理临时分块 → DB status=1。
     * 合并由 composeObject 在 MinIO 内部完成，分块字节流始终未经过后端。
     */
    public void completeChunkUpload(String fileId) {
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

        String partsPrefix = fileId + PART_DIR_PREFIX;
        List<MinoUtil.MinoObject> parts = minoUtil.listObjects(partsPrefix);
        if (parts.isEmpty()) {
            // 单块直传(或尚未开始): 校验最终对象键即可
            if (!minoUtil.doesObjectExist(entity.getObject_key())) {
                throw new BizException(ResultCode.RC10503);
            }
            fileDao.updateUploadStatus(fileId, 1);
            log.info("单块文件上传确认完成: fileId={}", fileId);
            return;
        }

        // 分块数量/大小完整校验(名称 + 总大小 必须与 file_size 完全一致)
        parts.sort(Comparator.comparing(MinoUtil.MinoObject::objectName));
        long totalSize = parts.stream().mapToLong(MinoUtil.MinoObject::size).sum();
        long expectCount = (entity.getFile_size() + parts.get(0).size() - 1) / parts.get(0).size();
        if (totalSize != entity.getFile_size() || parts.size() != expectCount
                || parts.get(0).size() <= 0) {
            log.error("分块不完整: fileId={}, expectSize={}, totalSize={}, expectCount={}, actualCount={}",
                    fileId, entity.getFile_size(), totalSize, expectCount, parts.size());
            throw new BizException(ResultCode.RC10506, "分块不完整或大小不一致，请重新上传或中止后重试");
        }

        List<String> srcKeys = new ArrayList<>();
        for (MinoUtil.MinoObject part : parts) {
            srcKeys.add(part.objectName());
        }
        try {
            minoUtil.composeObjects(entity.getObject_key(), srcKeys);
            // 合并成功即清理临时分块
            minoUtil.deleteObjectsByPrefix(partsPrefix);
        } catch (Exception e) {
            log.error("分块合并失败: fileId={}", fileId, e);
            throw new BizException(ResultCode.RC10506, "分块合并失败: " + e.getMessage());
        }

        if (!minoUtil.doesObjectExist(entity.getObject_key())) {
            throw new BizException(ResultCode.RC10503);
        }
        fileDao.updateUploadStatus(fileId, 1);
        log.info("分块文件上传确认完成: fileId={}, parts={}", fileId, parts.size());
    }

    /**
     * 分块上传 - 中止/清理: 删除已上传的临时分块 + DB 记录。
     * 仅允许操作未完成(status=0)的记录；已完成文件请走 realDeleteFile。
     */
    public void abortChunkUpload(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            throw new BizException(ResultCode.RC10501);
        }
        if (entity.getUpload_status() == 1) {
            throw new BizException(ResultCode.RC10502, "文件已完成上传，请使用删除功能");
        }
        // 清理 {fileId}/ 前缀下所有对象(含分块临时对象与可能的单块残留)
        try {
            minoUtil.deleteObjectsByPrefix(fileId + "/");
        } catch (Exception e) {
            log.error("清理分块对象失败: fileId={}", fileId, e);
            throw new BizException(ResultCode.RC10508);
        }
        fileDao.deleteById(fileId);
        log.info("分块上传已中止并清理: fileId={}", fileId);
    }

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
     * 彻底删除文件: MinIO 删除对象 + DB 物理删除记录
     * 说明:
     * 1. MinIO removeObject 幂等，对象不存在(未上传完成/已被删过)不会报错，故不区分 upload_status 统一删除；
     * 2. 先删 MinIO 再删 DB：若 MinIO 删除失败则抛异常中止，DB 记录保留，可重试；
     * 3. 删除后 DB 无残留记录，同 MD5 文件可再次正常上传(秒传以 status!=2 的有效记录为准)。
     */
    public void realDeleteFile(String fileId) {
        TFileInfo entity = fileDao.queryById(fileId);
        if (entity == null) {
            throw new BizException(ResultCode.RC10501);
        }
        try {
            minoUtil.deleteObject(entity.getObject_key());
        } catch (Exception e) {
            log.error("MinIO删除文件失败: fileId={}, objectKey={}", fileId, entity.getObject_key(), e);
            throw new BizException(ResultCode.RC10508);
        }
        fileDao.deleteById(fileId);
        log.info("文件彻底删除完成: fileId={}, objectKey={}", fileId, entity.getObject_key());
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
