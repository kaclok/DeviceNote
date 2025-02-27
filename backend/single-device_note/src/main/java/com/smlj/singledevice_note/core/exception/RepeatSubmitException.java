package com.smlj.singledevice_note.core.exception;

public class RepeatSubmitException extends RuntimeException {
    private final String userId;
    private final String requestId;

    public RepeatSubmitException(String userId, String requestId) {
        this.userId = userId;
        this.requestId = requestId;
    }

    @Override
    public String getMessage() {
        return "用户：{" + userId + "} 请求协议：{" + requestId + "} 过于频繁";
    }
}
