package com.smlj.lpjtlj.core.o.to;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true) // 链式
public class Msg<T> {
    protected int code = 0;
    protected T data = null;

    public Msg(int code, T data) {
        this.code = code;
        this.data = data;
    }
}
