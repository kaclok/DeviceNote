package com.smlj.nfcpatrol.core.network;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true)
public class Message<T> extends Msg<T> {
    protected long timestamp = 0;
    protected int zoneOffset = 0;

    public Message(int code, T data) {
        super(code, data);
    }
}
