package com.jthx.x.cms.watchdog.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorRequestInfo {
    private List<IndicatorRequestItem> tags;
}
