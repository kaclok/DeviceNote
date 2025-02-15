package com.smlj.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class FloatUtil {
    public static Float get(Float f, int x) {
        BigDecimal bigDecimal = new BigDecimal(f);
        // 设置保留的小数位数为2，采用四舍五入的舍入模式
        bigDecimal = bigDecimal.setScale(x, RoundingMode.HALF_UP);
        return bigDecimal.floatValue();
    }
}
