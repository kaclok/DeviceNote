package com.smlj.train.logic.o.dto.from;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

// 提交的出题的题库
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FrTList<T, V, X> {
    public T key;
    public ArrayList<V> list = new ArrayList<>();
    public X value;
}
