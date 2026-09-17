package com.smlj.singledevice_note.core;

import com.smlj.singledevice_note.core.o.dto.TokenPair;

public interface  TokenRefresher {
    /**
     * 刷新 token 并返回，若无需刷新返回 null
     */
    TokenPair refresh();
}
