package com.smlj.common.service;

import com.smlj.common.o.to.AccountUser;

public class CurUserService {
    // 定义一个 ThreadLocal 变量，用于存储当前线程的用户信息
    private static final ThreadLocal<AccountUser> lc = new ThreadLocal<>();

    // 设置当前线程的用户信息
    public static void set(AccountUser user) {
        lc.set(user);
    }

    // 移除当前线程的用户信息
    public static void remove() {
        lc.remove();
    }

    // 获取当前线程的用户信息
    public static AccountUser get() {
        return lc.get();
    }
}
