package com.smlj.singledevice_note.core.o.converter;

import cn.hutool.json.JSONUtil;
import org.springframework.core.convert.converter.Converter;

public class StringTo<T> implements Converter<String, T> {
    private final Class<T> clazz;

    public StringTo(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T convert(String source) {
        var r = JSONUtil.toBean(source, clazz);
        return r;
    }
}
