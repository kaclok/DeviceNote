package com.smlj.singledevice_note.core.o.dto.file;

import lombok.Data;

import java.util.List;

/**
 * 分块上传 - 开始上传结果
 * is_exist=true 时表示 MD5 秒传命中，parts 为空，无需再上传。
 * 否则 parts 长度 = chunk_count；当 chunk_count==1 时(文件 <= 分块大小)，
 * 该 url 直接指向最终对象键，等价于普通单块直传。
 */
@Data
public class FileChunkBeginResultDTO {
    /** 文件记录 id */
    private String file_id;
    /** 实际分块大小(字节)，与请求一致 */
    private Long chunk_size;
    /** 分块数量(>=1) */
    private Long chunk_count;
    /** 是否秒传命中(文件已存在且上传完成) */
    private Boolean is_exist;
    /** 各分块预签名 PUT 直链(顺序即合并顺序) */
    private List<FileChunkPartDTO> parts;
}
