package com.smlj.devicenote.core.exception;

import com.smlj.devicenote.core.o.to.Result;
import com.smlj.train.logic.o.dto.to.common.ReturnCode;
import jakarta.validation.ConstraintViolationException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

// https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
@RestControllerAdvice(basePackages = "com.smlj.train")
public class GlobalExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return Result.fail(ReturnCode.RC10104, ex.getMessage());
    }

    @ExceptionHandler(RepeatSubmitException.class)
    public Result<?> handleRepeatSubmitException(RepeatSubmitException ex) {
        return Result.fail(ReturnCode.RC10108, ex.getMessage());
    }

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // {@code @RequestBody} 参数校验不通过时抛出的异常处理
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        StringBuilder sb = new StringBuilder("校验失败:");
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            sb.append(fieldError.getField()).append("：").append(fieldError.getDefaultMessage()).append(", ");
        }
        String msg = sb.toString();
        if (StringUtils.hasText(msg)) {
            return Result.fail(ReturnCode.RC400.getCode(), msg);
        }
        return Result.fail(ReturnCode.RC400);
    }

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // {@code @PathVariable} 和 {@code @RequestParam} 参数校验不通过时抛出的异常处理
    @ExceptionHandler({ConstraintViolationException.class})
    public Result<?> handleConstraintViolationException(ConstraintViolationException ex) {
        if (StringUtils.hasText(ex.getMessage())) {
            return Result.fail(ReturnCode.RC400.getCode(), ex.getMessage());
        }
        return Result.fail(ReturnCode.RC400);
    }

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // 顶级异常捕获并统一处理，当其他异常无法处理时候选择使用
    @ExceptionHandler({Exception.class})
    public Result<?> handle(Exception ex) {
        return Result.fail(ex.getMessage());
    }
}