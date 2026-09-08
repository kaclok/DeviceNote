package com.smlj.singledevice_note.core.o.dto.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class FileUploadBeginDTO {
    @NotBlank(message = "文件名不能为空")
    private String original_name;

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long file_size;

    @NotBlank(message = "文件MD5不能为空")
    private String file_md5;
}
