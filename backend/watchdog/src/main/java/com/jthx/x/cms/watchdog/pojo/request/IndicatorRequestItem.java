package com.jthx.x.cms.watchdog.pojo.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IndicatorRequestItem {
    private List<String> items;
    private String namespace;
    private String tag;
}
