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
    private String dw;

    // 指标对应设备对应的分厂id
    private int branchId;

    // 指标域
    private String namespace;
    // 指标字段
    private String tag;

    // 滑动窗口大小
    private int windowSize;

    // 突变
    private int useSpikeDetect;
    private double spikeThreshold = 0.3;

    // CUSUM
    private int useCUSUMDetect;
    private double cusumK = 0.01; // 容忍误差k
    private double cusumThreshold = 0.2;
    private int cusumWindowSize = 20;

    // 异常发生上限后报警
    private int anomalyWindowSize;
    private int anomalyCount;

    private int useCd;
    private int cdMills;

    private int groupId;

    // zScore
    private int useZScoreDetect;
    private double zScoreThreshold = 3;
}
