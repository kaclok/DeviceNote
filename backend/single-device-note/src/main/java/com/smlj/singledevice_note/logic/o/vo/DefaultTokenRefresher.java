package com.smlj.singledevice_note.logic.o.vo;

import com.smlj.singledevice_note.core.TokenRefresher;
import com.smlj.singledevice_note.core.o.dto.TokenPair;
import org.springframework.stereotype.Component;

@Component
public class DefaultTokenRefresher implements TokenRefresher {
    @Override
    public TokenPair refresh() {
        return null;
    }
}
