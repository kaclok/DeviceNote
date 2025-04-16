package com.jthx.x.cms.watchdog.pojo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorResponseItem {
    @JsonProperty("AV")
    private double AV;
    @JsonProperty("qos")
    private double qos;
}
