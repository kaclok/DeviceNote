package com.smlj.singledevice_note.core.o.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 分块上传 - 开始上传请求
 * 与整文件上传(FileUploadBeginDTO)的区别: 额外携带前端切片大小 chunk_size，
 * 后端据此为每个分块签发独立预签名 PUT 直链，前端并发直传 MinIO。
 */
@Data
public class FileChunkBeginDTO {
    @NotBlank(message = "文件名不能为空")
    private String original_name;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long file_size;

    /** 分块大小(字节)，前端切片基准；须 >= 5MB 且 < 文件大小时才会真正分块 */
    @NotNull(message = "分块大小不能为空")
    @Positive(message = "分块大小必须大于0")
    private Long chunk_size;

    /** 文件整体 MD5，可空(为空则不走秒传/断点续传复用) */
    private String file_md5;
}
