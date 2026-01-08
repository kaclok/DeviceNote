package com.smlj.nfcpatrol.core.network;

public class Message<T> extends Msg<T> {
    public long timestamp = 0;
    public int zoneOffset = 0;

    public Message(int code, T data) {
        super(code, data);
    }
}
