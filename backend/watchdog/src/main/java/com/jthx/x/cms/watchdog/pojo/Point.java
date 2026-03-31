package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class Point {
    // 指标id
    private int id;
    // 指标名
    private String indicatorName;

    // 指标对应设备id
    private int deviceId;
    // 指标对应设备对应的分厂id
    private int branchId;

    // 异常发生上限后报警
    private int exceptionCount;
    private int cdMills;
    // 指标域
    private String namespace;
    // 指标字段
    private String tag;

    // 滑动窗口大小
    private int windowSize;

    private boolean useCd;
    private boolean useSpikeDetect = true;
    private boolean useCUSUMDetect = true;
    private boolean useZScoreDetect = false;

    // CUSUM
    private double k = 0.01; // 容忍误差k
    private double cusumThreshold = 0.2;

    // 突变阈值
    private double spikeThreshold = 0.3;

    // zScore
    private double zScoreThreshold = 3;

    private String dw;
}
