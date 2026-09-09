package com.smlj.singledevice_note.core.o.dto.file;

import lombok.Data;

/**
 * 分块上传 - 单个分块的预签名 PUT 直链
 * 前端拿 url 直接把该分块字节流 PUT 到 MinIO(不经后端转发)。
 * exists=true 表示该分块已完整存在于 MinIO(与期望大小一致)，
 * 前端可跳过直传(断点续传只补传缺失分块)。
 * 说明: MinIO 对象上传只支持 PUT/POST(form)，预签名采用 PUT，属 MinIO 强制协议。
 */
@Data
public class FileChunkPartDTO {
    /** 分块序号，从 0 开始，与临时对象 {fileId}/parts/xxxxx 一一对应 */
    private Integer index;
    /** 该分块直传 MinIO 的预签名 PUT URL */
    private String url;
    /** 该分块是否已完整存在于 MinIO(服务端按期望大小校验一致)。true → 前端跳过直传 */
    private Boolean exists;
}
