package com.smlj.encrypt.core;

import lombok.AllArgsConstructor;
import lombok.Getter;

// https://github.com/kaclok/springboot-vue/blob/master/yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/exception/enums/GlobalErrorCodeConstants.java#L17
@Getter
@AllArgsConstructor
public enum ResultCode {
    RC1(1, "成功"),

    // 协议参数传输
    RC400(0, "加密失败");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;
}
