package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class Snapshot {

    // 实时
    private double value;
    // 时间
    private LocalDateTime date;
    // 变化率
    private double rate;
    // 指标id
    private int indicatorId;
    // 指标名称
    private String indicatorName;
    // 设备id
    private int deviceId;
    // 分厂id
    private int branchId;
}
