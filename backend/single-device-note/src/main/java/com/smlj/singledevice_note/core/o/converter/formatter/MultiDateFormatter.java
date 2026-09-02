package com.smlj.singledevice_note.core.o.converter.formatter;

import org.springframework.format.Formatter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

// 全局注册的 Formatter 会覆盖 Spring 默认的日期转换行为
// 如果同时使用了 @DateTimeFormat 注解和全局 Formatter，注解会优先生效
@Component
public class MultiDateFormatter implements Formatter<Date> {
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getTimeZone("Asia/Shanghai");

    // 定义所有支持的日期格式,数组内元素顺序不可调整，会影响for的parse
    private static final String[] SUPPORTED_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd HH",
            "yyyy/MM/dd HH",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
    };

    @Override
    public Date parse(@NonNull String text, @NonNull Locale locale) throws ParseException {
        if (text.trim().isEmpty()) {
            return null;
        }

        String trimmedText = text.trim();

        // 尝试所有支持的格式
        for (String format : SUPPORTED_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false); // 严格模式，确保日期有效
                var r = sdf.parse(trimmedText);
                ;
                return r;
            } catch (ParseException e) {
                // 当前格式解析失败，继续尝试下一个
            }
        }

        // 所有格式都失败，抛出异常
        throw new ParseException("无法解析日期: " + text +
                ", 支持的格式: " + String.join(", ", SUPPORTED_FORMATS), 0);
    }

    @Override
    public String print(@NonNull Date date, @NonNull Locale locale) {
        // 统一输出格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
}
