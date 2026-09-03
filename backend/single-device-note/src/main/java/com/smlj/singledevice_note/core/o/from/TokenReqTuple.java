package com.smlj.singledevice_note.core.o.from;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenReqTuple {
    private String account;
    private String password;
    private String captcha;
}
