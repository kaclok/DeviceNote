package com.smlj.singledevice_note.core.o.dto.file;

import lombok.Data;

@Data
public class FileUploadResultDTO {
    private String file_id;
    private String upload_url;
    private String download_url;
    private Boolean is_exist; // 是否已存在（秒传）
}
