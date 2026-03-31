package com.jthx.x.cms.watchdog.pojo;

import lombok.Data;

@Data
public class MetricTask {
    private Point point;

    // 轮询周期（秒）
    private long period;

    // 获取指标逻辑
    private Runnable task;
}
