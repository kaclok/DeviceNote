package com.smlj.nfcpatrol.core;

public class Msg<T> {
    public int code = 0;

    public T data = null;

    public Msg(int code, T data) {
        this.code = code;
        this.data = data;
    }
}
