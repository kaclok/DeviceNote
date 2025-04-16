package com.jthx.x.cms.watchdog.pojo.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jthx.x.cms.watchdog.util.SMDSSafeAPI;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorResponseInfo {
    @JsonProperty("data")
    private List<IndicatorResponseData> data;
    @JsonProperty("message")
    private String message;
    @JsonProperty("status")
    private Integer status;
    @JsonProperty("timestamp")
    private Long timestamp;
    private HashMap<String, Double> snapshotMap = new HashMap<String, Double>();


    public void tryFillSnapshotMap() {
        if (!SMDSSafeAPI.isListNotEmpty(data)) {
            return;
        }
        for (IndicatorResponseData item : data) {
            String n = item.getN();
            if (!SMDSSafeAPI.isStringNotEmpty(n)) { // 如果为空字符串则不填充数据
                continue;
            }
            double av = item.getV().getAV();
            snapshotMap.put(n, av);
        }
    }
    
    public double snapshotWithTag(String tag) {
        if (!snapshotMap.containsKey(tag)) {
            return 0.f;
        }
        Double av = snapshotMap.get(tag);
        return av == null ? 0.f : av;
    }
}
