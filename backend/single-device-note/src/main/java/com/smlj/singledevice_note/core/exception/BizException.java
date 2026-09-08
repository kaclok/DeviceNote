package com.smlj.singledevice_note.core.exception;

import com.smlj.singledevice_note.core.o.to.ResultCode;
import lombok.Getter;

/**
 * 业务异常：Service 层抛出，携带 ResultCode，由 GlobalExceptionHandler 统一捕获并包装成 Result
 */
@Getter
public class BizException extends RuntimeException {
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
