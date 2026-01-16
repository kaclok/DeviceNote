package com.smlj.nfcpatrol.core.network;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ApiException extends RuntimeException {
    protected int code;

    public ApiException(int code, String message) {
        super(message);
        this.code = code;
    }
}
