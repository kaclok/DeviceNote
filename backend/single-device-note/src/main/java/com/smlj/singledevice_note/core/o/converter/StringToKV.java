package com.smlj.singledevice_note.core.o.converter;

import cn.hutool.json.JSONUtil;
import org.springframework.core.convert.converter.Converter;
import com.smlj.singledevice_note.core.o.dto.KV;

public class StringToKV<K, V> implements Converter<String, KV<K, V>> {
    @Override
    public KV<K, V> convert(String source) {
        var r = JSONUtil.toBean(source, KV.class);
        return r;
    }
}
