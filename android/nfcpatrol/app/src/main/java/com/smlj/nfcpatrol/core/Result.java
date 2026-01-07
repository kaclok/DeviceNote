package com.smlj.nfcpatrol.core;


// 前后端交互数据标准
public class Result<T> extends Message<T> {
    public String message = null;

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
        return success(data, "成功");
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<>(200, data, message);
    }

    public static <T> Result<T> fail() {
        return fail("失败", null);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message) {
        return fail(message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return fail(code, message, null);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message, T data) {
        return fail(500, message, data);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, data, message);
    }

    // 成功/失败都处理
    public static <T> Result<T> sf(boolean r) {
        if (r) {
            return success();
        } else {
            return fail();
        }
    }
}
