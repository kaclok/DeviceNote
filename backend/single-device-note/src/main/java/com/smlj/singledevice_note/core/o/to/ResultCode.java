package com.smlj.singledevice_note.core.o.to;

import lombok.AllArgsConstructor;
import lombok.Getter;

// https://github.com/kaclok/springboot-vue/blob/master/yudao-framework/yudao-common/src/main/java/cn/iocoder/yudao/framework/common/exception/enums/GlobalErrorCodeConstants.java#L17
@Getter
@AllArgsConstructor
public enum ResultCode {
    RC199(199, "失败"),
    RC200(200, "成功"),

    // 协议参数传输
    RC400(400, "请求参数不正确"),
    RC402(401, "请求参数缺失"),
    RC403(403, "无访问权限"),
    RC404(404, "请求未找到"),

    // ========== 服务端错误段 ==========
    RC500(500, "系统异常"),
    RC501(501, "功能未实现/未开启"),
    RC502(502, "错误的配置项"),
    RC503(503, "token过期"),
    RC504(504, "token不正确"),

    RC10005(10005, "token校验不正确"),

    // 文件
    RC10104(10104, "文件超过限制大小(单个300MB,总共1000MB)"),

    // 验证码
    RC10201(10201, "验证码不正确"),

    // 用户
    RC10301(10301, "用户不存在"),
    RC10302(10302, "用户所在的部门不存在"),

    // 保持在最后
    RC_1(-1, "未知错误");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;
}
