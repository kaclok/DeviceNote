package com.jthx.x.cms.watchdog.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenRequestInfo {
    private String username;
    private String password;
}

