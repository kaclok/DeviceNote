package com.smlj.devicenote.core;

import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ESwitch {
    ENABLE(0, "开启"),
    DISABLE(1, "关闭");

    public static final int[] ARRAYS = Arrays.stream(values()).mapToInt(ESwitch::getStatus).toArray();

    private final int status;
    private final String name;

    public int[] array() {
        return ARRAYS;
    }

    public static boolean isEnable(int status) {
        return ObjUtil.equal(ENABLE.status, status);
    }

    public static boolean isDisable(int status) {
        return ObjUtil.equal(DISABLE.status, status);
    }
}
