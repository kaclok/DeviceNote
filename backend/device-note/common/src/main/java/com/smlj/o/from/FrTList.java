package com.smlj.o.from;

import com.smlj.o.dto.KV;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

// 提交的出题的题库
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrTList<K, V, X> {
    public KV<K, V> key;
    public ArrayList<X> list = new ArrayList<>();
}
