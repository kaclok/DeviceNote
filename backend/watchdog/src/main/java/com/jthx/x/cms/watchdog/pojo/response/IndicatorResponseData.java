package com.jthx.x.cms.watchdog.pojo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorResponseData {
    @JsonProperty("n")
    private String n;
    @JsonProperty("ns")
    private String ns;
    @JsonProperty("t")
    private Long t;
    @JsonProperty("v")
    private IndicatorResponseItem v;
}
