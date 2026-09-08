package com.smlj.singledevice_note.core.o.vo.table.entity;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 文件元数据实体
 * 对应表: core.t_file_info
 */
@Data
public class TFileInfo {
    /** 主键 UUID */
    private String id;
    /** 原始文件名 */
    private String original_name;
    /** MinIO 存储路径 */
    private String object_key;
    /** 文件大小(字节) */
    private Long file_size;
    /** 文件 MD5 哈希 */
    private String file_md5;
    /** MIME 类型 */
    private String content_type;
    /** 桶名 */
    private String bucket_name;
    /** 上传状态: 0=待上传 1=已上传 2=已删除 */
    private Integer upload_status;
    /** 创建时间 */
    private Timestamp create_time;
    /** 更新时间 */
    private Timestamp update_time;
}
