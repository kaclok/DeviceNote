package com.smlj.singledevice_note.core.o.to;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Objects;

// 前后端交互数据标准
@Getter
@Setter
@Accessors(chain = true) // 链式
@NoArgsConstructor  // private和protected的字段必须被lombok暴露出来，才能被json序列化
public class Result<T> extends Message<T> {
    @Schema(description = "业务逻辑提示信息")
    protected String message = null;

    @JsonIgnore
    protected boolean needRefreshAT;

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
        return new Result<>(resultCode.getCode(), null, resultCode.getMessage());
    }

    public static <T> Result<T> fail() {
        return fail("操作失败", null);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message) {
        return fail(message, null);
    }

    public static <T> Result<T> fail(int code, String message) {
        return fail(code, message, null);
    }

    /**
     * 指定结果码 + 自定义提示文案。
     * <p>
     * 注意：第二参是 <b>message</b>，不是 data。历史上有 {@code fail(ResultCode, T data)} 重载，
     * 使 {@code fail(RC10101, "承包商不存在")} 静默解析成「data=字符串、message=枚举默认文案」，
     * 前端只显示「请求参数为空或不合法」，真实原因被吞。该重载已删除，需要带 data 请用
     * {@link #failWithData(ResultCode, Object)}。
     */
    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return fail(resultCode.getCode(), message, null);
    }

    // 自定义失败信息, fail返回值其实会自动推导，因为T没有传递下去
    public static <T> Result<T> fail(String message, T data) {
        return fail(ResultCode.RC500.getCode(), message, data);
    }

    public static <T> Result<T> fail(int code, String message, T data) {
        return new Result<>(code, data, message);
    }

    /** 失败并携带数据（原 fail(ResultCode, T) 的语义，改名避免与「结果码+文案」混淆） */
    public static <T> Result<T> failWithData(ResultCode resultCode, T data) {
        return fail(resultCode.getCode(), resultCode.getMessage(), data);
    }

    // 成功/失败都处理
    public static <T> Result<T> sf(boolean r) {
        if (r) {
            return success();
        } else {
            return fail();
        }
    }

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
