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
    private ArrayList<Double> window = new ArrayList<>();
    private ArrayList<Double> cusumPosHistory = new ArrayList<>();
    private ArrayList<Double> cusumNegHistory = new ArrayList<>();

    private int failReason = -1;
    private boolean isOverCount;
    private boolean isOverCD;

    private Date date;

    private Point point;
    private double av;
    private double average;

    public ExceptionDetail(TrendJumpDetector dataHandler, Point point, double av) {
        this.date = new Date();
        this.point = point;
        this.av = av;
        this.average = dataHandler.getWindowAverage();

        this.failReason = dataHandler.getFailReason();
        this.isOverCount = dataHandler.isOverCount();
        this.isOverCD = dataHandler.isOverCD();

        this.window.addAll(dataHandler.getWindow());
        this.cusumPosHistory.addAll(dataHandler.getIncrPosHistory());
        this.cusumNegHistory.addAll(dataHandler.getIncrNegHistory());
    }
}
