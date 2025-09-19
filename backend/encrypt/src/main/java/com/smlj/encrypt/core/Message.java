package com.smlj.encrypt.core;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Setter
@Getter
@NoArgsConstructor
@Accessors(chain = true) // 链式
public class Message<T> extends Msg<T> {
    protected long timestamp = 0;

    public Message(int code, T data) {
        super(code, data);
        this.setTime();
    }

    public void setTime() {
        this.timestamp =System.currentTimeMillis();
    }
}
