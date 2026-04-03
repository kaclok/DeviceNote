package com.jthx.x.cms.watchdog.pojo;

import com.jthx.x.cms.watchdog.Detector.TrendJumpDetector;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@NoArgsConstructor
@Data
@Component
public class ExceptionDetail {
    private int windowSize = 30;
    private ArrayList<Double> window = new ArrayList<>();

    private double sum = 0;
    private double sumSquare = 0;

    // CUSUM
    private double cusumPos = 0;
    private double cusumNeg = 0;
    private double k = 0.01; // 容忍误差k
    private double cusumPosThreshold = 0.2;
    private double cusumNegThreshold = 0.2;

    // 突变阈值
    private double spikeThreshold = 0.3;

    // zScore
    private double zScoreThreshold = 3;

    private int useCd;

    private int failReason = -1;
    private boolean isOverCount;
    private boolean isOverCD;

    private Date date;

    private Point point;
    private double av;

    public ExceptionDetail(TrendJumpDetector dataHandler, Point point, double av) {
        this.date = new Date();
        this.point = point;
        this.av = av;

        this.failReason = dataHandler.getFailReason();
        this.isOverCount = dataHandler.isOverCount();
        this.isOverCD = dataHandler.isOverCD();

        this.sum = dataHandler.getSum();
        this.sumSquare = dataHandler.getSumSquare();
        this.cusumPos = dataHandler.getCusumPos();
        this.cusumNeg = dataHandler.getCusumNeg();
        this.k = dataHandler.getK();
        this.cusumPosThreshold = dataHandler.getCusumPosThreshold();
        this.cusumNegThreshold = dataHandler.getCusumNegThreshold();
        this.spikeThreshold = dataHandler.getSpikeThreshold();
        this.zScoreThreshold = dataHandler.getZScoreThreshold();
        this.useCd = dataHandler.getUseCd();

        this.window.addAll(dataHandler.getWindow());
        this.windowSize = window.size();
    }
}
