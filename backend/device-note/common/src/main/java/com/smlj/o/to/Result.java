package com.smlj.o.to;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

// 前后端交互数据标准
@Getter
@Setter
@NoArgsConstructor  // private和protected的字段必须被lombok暴露出来，才能被json序列化
public class Result<T> extends Message<T> {
    @Schema(description = "业务逻辑提示信息")
    protected String message = null;

    public Result(int code, T data) {
        super(code, data);
    }

    public Result(int code, T data, String message) {
        super(code, data);
        this.message = message;
    }

    /*public boolean isSuccess() {
        return isSuccess(code);
    }

    public boolean isError() {
        return !isSuccess();
    }*/

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        return success(data, ResultCode.RC200.getMessage());
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(ResultCode.RC200.getCode(), data, message);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return fail(resultCode, null);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message) {
        return fail(message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return fail(code, message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, T data) {
        return fail(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message, T data) {
        return fail(ResultCode.RC500.getCode(), message, data);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, data, message);
    }

    // 成功/失败都处理
    public static <T> Result<T> sf(ResultCode resultCode, T data) {
        if (isSuccess(resultCode.getCode())) {
            return success(data);
        } else {
            return fail(resultCode.getCode(), resultCode.getMessage(), data);
        }
    }

    public static boolean isSuccess(int code) {
        return Objects.equals(code, ResultCode.RC200.getCode());
    }
}