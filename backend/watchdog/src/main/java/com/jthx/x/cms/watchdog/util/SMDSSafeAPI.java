package com.jthx.x.cms.watchdog.util;

import java.util.List;
import java.util.Map;

public class SMDSSafeAPI {
    public static boolean isStringNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    // 判断数组是否非空
    public static <T> boolean isListNotEmpty(List<T> array) {
        return array != null && array.size() > 0;
    }

    // 判断 Map 是否非空
    public static <K, V> boolean isMapNotEmpty(Map<K, V> map) {
        return map != null && !map.isEmpty();
    }
}
