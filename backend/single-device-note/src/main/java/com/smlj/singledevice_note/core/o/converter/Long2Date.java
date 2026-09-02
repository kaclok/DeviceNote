package com.smlj.singledevice_note.core.o.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;

import java.util.Date;

// 时间戳转换为Date
public class Long2Date implements Converter<Long, Date> {
    @Override
    public Date convert(@NonNull Long source) {
        // 直接使用毫秒数创建 Date
        return new Date(source);
    }
}
