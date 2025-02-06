package com.smlj.devicenote.logic.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EExamType {
    ET0(0, "不考评"),
    ET1(1, "试卷考核"),
    ET2(2, "心得报告"),
    ET4(4, "行为观察"),
    ET8(8, "现场实操"),
    ET16(16, "现场提问");

    // 自定义状态码
    private final int code;
    // 自定义描述
    private final String message;

    public static EExamType fromValue(int value) {
        for (var flag : EExamType.values()) {
            if ((flag.code ^ value) == 0) {
                return flag;
            }
        }
        return ET0;
    }

    public static boolean contains(int value, EExamType type) {
        var c = type.getCode();
        return (value & c) == c;
    }
}
