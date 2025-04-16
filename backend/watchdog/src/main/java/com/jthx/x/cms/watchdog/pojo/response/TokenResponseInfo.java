package com.jthx.x.cms.watchdog.pojo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TokenResponseInfo {
    @JsonProperty("data")
    private Map<String, String> data;
    @JsonProperty("message")
    private String message;
    @JsonProperty("status")
    private Long status;
    @JsonProperty("timestamp")
    private Long timestamp;
}
