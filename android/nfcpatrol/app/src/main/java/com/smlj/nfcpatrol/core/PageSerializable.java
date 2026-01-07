package com.smlj.nfcpatrol.core;

import java.io.Serializable;
import java.util.List;

public class PageSerializable<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    public long total;
    public List<? extends T> list;

    public PageSerializable() {
    }

    public PageSerializable(List<? extends T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public long getTotal() {
        return this.total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<? extends T> getList() {
        return this.list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public String toString() {
        return "PageSerializable{total=" + this.total + ", list=" + this.list + '}';
    }
}

