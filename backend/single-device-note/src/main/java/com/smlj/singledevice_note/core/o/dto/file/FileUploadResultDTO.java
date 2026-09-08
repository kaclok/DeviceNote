package com.smlj.singledevice_note.core.o.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FileUploadResultDTO {
    private String fileId;
    private String uploadUrl;
    private String downloadUrl;
    private Boolean isExist; // 是否已存在（秒传）
}
