package com.smlj.singledevice_note.core.o.vo.table.dao;

import com.smlj.singledevice_note.core.o.vo.table.entity.TFileInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文件元数据 DAO
 */
@Mapper
@Repository
public interface TFileInfoDao {
    /** 插入文件记录 */
    int insert(TFileInfo entity);

    /** 按 id 查询 */
    TFileInfo queryById(@Param("id") String id);

    /** 按 MD5 查询（秒传判断） */
    TFileInfo queryByMd5(@Param("file_md5") String fileMd5);

    /** 更新上传状态 */
    int updateUploadStatus(@Param("id") String id, @Param("upload_status") int uploadStatus);

    /** 软删除（标记为已删除） */
    int markDeleted(@Param("id") String id);

    /** 物理删除（彻底删除：DB 记录与 MinIO 对象一并移除） */
    int deleteById(@Param("id") String id);

    /** 查询所有已上传完成的有效文件(status=1)，用于预览列表 */
    List<TFileInfo> queryValidList();
}
