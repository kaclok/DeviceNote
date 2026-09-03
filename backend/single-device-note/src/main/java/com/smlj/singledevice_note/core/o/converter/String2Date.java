package com.smlj.singledevice_note.core.o.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;

public class String2Date implements Converter<String, Date> {
    // 定义所有支持的日期格式,数组内元素顺序不可调整，会影响for的parse
    private static final String[] SUPPORTED_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy.MM.dd HH:mm:ss",
            "yyyyMMdd HH:mm:ss",

            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy.MM.dd HH:mm",
            "yyyyMMdd HH:mm",

            "yyyy-MM-dd HH",
            "yyyy/MM/dd HH",
            "yyyy.MM.dd HH",
            "yyyyMMdd HH",

            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyy.MM.dd",
            "yyyyMMdd",
    };

    @Nullable
    @Override
    public Date convert(@NonNull String source) {
        if (source.trim().isEmpty()) {
            return null;
        }

        String trimmedText = source.trim();
        for (String pattern : SUPPORTED_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false); // 严格模式，确保日期有效
                // sdf.setTimeZone(DEFAULT_TIMEZONE);
                return sdf.parse(trimmedText);
            } catch (Exception e) {
                // 当前格式解析失败，继续尝试下一个
            }
        }
        throw new IllegalArgumentException("无法解析日期: " + source);
    }
}
