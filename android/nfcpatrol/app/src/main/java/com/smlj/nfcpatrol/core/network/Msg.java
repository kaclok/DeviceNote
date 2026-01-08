package com.smlj.nfcpatrol.core.network;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class Msg<T> {
    protected int code = 0;

    protected T data = null;

    public Msg(int code, T data) {
        this.code = code;
        this.data = data;
    }
}
