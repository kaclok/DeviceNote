package com.smlj.nfcpatrol.core.network;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

@Data
public class PageSerializable<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private long total;
    private List<? extends T> list;

    public PageSerializable() {
    }

    public PageSerializable(List<? extends T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public String toString() {
        return "PageSerializable{total=" + this.total + ", list=" + this.list + '}';
    }
}

