package com.smlj.singledevice_note.core.exception;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.o.to.ResultCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

// https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
@RestControllerAdvice(basePackages = "com.smlj.train")
public class GlobalExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return Result.fail(ResultCode.RC10104, ex.getMessage());
    }

    /*@ExceptionHandler(RepeatSubmitException.class)
    public Result<?> handleRepeatSubmitException(RepeatSubmitException ex) {
        return Result.fail(ReturnCode.RC10108, ex.getMessage());
    }*/

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // {@code @RequestBody} 参数校验不通过时抛出的异常处理
    // https://mp.weixin.qq.com/s/slsETQsBjMJ4qKRCKYeGGg
    // https://blog.csdn.net/qq_42402854/article/details/137344029
    // 处理 @Valid 校验异常
    // @RequestBody + @Valid → MethodArgumentNotValidException
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        // 自动处理所有校验异常
        List<String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        return Result.fail(400, "参数校验失败", errors);
    }

    /**
     * 处理 @ModelAttribute + @Valid 校验异常
     * 适用场景：GET 请求使用 @ModelAttribute 绑定对象参数时
     */
    // @ModelAttribute + @Valid → BindException
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<?> handleBindException(BindException e) {
        // 收集所有字段错误信息
        List<String> errors = e.getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        // 返回统一错误格式
        return Result.fail(400, "参数绑定失败", errors);
    }

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // {@code @PathVariable} 和 {@code @RequestParam} 参数校验不通过时抛出的异常处理
    // 处理 @RequestParam + @Validated 校验异常
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        List<String> errors = e.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.toList());
        return Result.fail(400, "参数校验失败", errors);
    }

    // 处理 HTTP 消息不可读（如 JSON 格式错误）
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return Result.fail("请求格式错误", ex.getMessage());
    }

    // https://mp.weixin.qq.com/s/vVBmqCbhmLXYjW1w8gsk3Q
    // 顶级异常捕获并统一处理，当其他异常无法处理时候选择使用
    @ExceptionHandler({Exception.class})
    public Result<?> handle(Exception ex) {
        return Result.fail(ex.getMessage());
    }
}
