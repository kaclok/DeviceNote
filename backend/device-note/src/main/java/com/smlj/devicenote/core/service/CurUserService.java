package com.smlj.devicenote.core.service;

import com.smlj.devicenote.logic.o.dto.to.AccountUsername;

public class CurUserService {
    private static final ThreadLocal<AccountUsername> l_user = new ThreadLocal<>();

    public static void set(AccountUsername user) {
        l_user.set(user);
    }

    public static void remove() {
        l_user.remove();
    }

    public static AccountUsername get() {
        return l_user.get();
    }
}
