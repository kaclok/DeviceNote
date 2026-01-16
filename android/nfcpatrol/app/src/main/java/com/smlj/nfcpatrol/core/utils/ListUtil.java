package com.smlj.nfcpatrol.core.utils;

import java.util.List;
import java.util.function.BiFunction;

public class ListUtil {
    public static <T> int finalIdx(T aimId, List<T> list) {
        if (list == null || list.isEmpty()) {
            return -1;
        }

        var length = list.size();
        for (int i = 0; i < length; i++) {
            var one = list.get(i);
            if (one.equals(aimId)) {
                return i;
            }
        }
        return 0;
    }

    public static <T, Q> int finalIdx(T aimId, List<Q> list, BiFunction<T, Q, Boolean> func) {
        if (list == null || list.isEmpty()) {
            return -1;
        }

        var length = list.size();
        for (int i = 0; i < length; i++) {
            var one = list.get(i);
            if (func.apply(aimId, one)) {
                return i;
            }
        }
        return 0;
    }
}
