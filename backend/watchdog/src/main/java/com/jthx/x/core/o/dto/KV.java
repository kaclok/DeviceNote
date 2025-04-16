package com.jthx.x.core.o.dto;

import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KV<T, V> implements Serializable {
    public T key;
    public V value;

    public KV(String json) {
        KV<T, V> r = JSONUtil.toBean(json, KV.class);
        key = r.key;
        value = r.value;
    }
}
