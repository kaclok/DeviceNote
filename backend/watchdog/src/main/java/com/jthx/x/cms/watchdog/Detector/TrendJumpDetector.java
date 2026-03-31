package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.pojo.Point;
import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;

@Data
public class TrendJumpDetector {
    private int windowSize = 30;
    private Queue<Double> window = new LinkedList<>();

    private double sum = 0;
    private double sumSquare = 0;
    private double prevValue = Double.NaN;

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

    // ---------- 报警控制 ----------
    private int anomalyCounter = 0;
    private int alarmConfirmCount = 3;

    // ---------- 每次报警间隔多久 ----------
    private long cdMillis = 3 * 60 * 1000;  // 冷却时间（毫秒）
    private long lastAlarmTime = 0;  // 上次报警时间

    private boolean useSpikeDetect = true;
    private boolean useCUSUMDetect = true;
    private boolean useZScoreDetect = false;
    private boolean useCd = true;

    private int failReason = -1;

    public boolean isWindowFull() {
        return window.size() >= windowSize;
    }

    public boolean isOverCount() {
        return anomalyCounter >= alarmConfirmCount;
    }

    public boolean isOverCD() {
        long now = System.currentTimeMillis();
        if (now - lastAlarmTime > cdMillis) {
            return true;
        }
        return false;
    }

    public TrendJumpDetector(Point point) {
        this.useSpikeDetect = point.isUseSpikeDetect();
        this.useCUSUMDetect = point.isUseCUSUMDetect();
        this.useZScoreDetect = point.isUseZScoreDetect();
        this.useCd = point.isUseCd();

        this.windowSize = point.getWindowSize();

        this.spikeThreshold = point.getSpikeThreshold();
        this.k = point.getK();
        this.cusumNegThreshold = this.cusumPosThreshold = point.getCusumThreshold();

        this.zScoreThreshold = point.getZScoreThreshold();

        this.alarmConfirmCount = point.getExceptionCount();
        this.cdMillis = point.getCdMills();
    }

    // ---------- 更新窗口 ----------
    private void updateWindow(double value) {
        window.add(value);
        sum += value;
        sumSquare += value * value;

        // 超过窗口，则去除首个元素
        if (window.size() > windowSize) {
            double removed = window.poll();
            sum -= removed;
            sumSquare -= removed * removed;
        }
    }

    // ---------- 突刺检测 ----------
    private boolean detectSpike(double value) {
        if (Double.isNaN(prevValue)) {
            return false;
        }

        double delta = Math.abs(value - prevValue);
        return delta > spikeThreshold;
    }

    // ---------- CUSUM趋势检测 ----------
    private boolean detectCUSUM(double value, double average) {
        double diff = value - average;

        // ---------- 上升趋势 ----------
        cusumPos = Math.max(0, cusumPos + diff - k);

        // ---------- 下降趋势 ----------
        cusumNeg = Math.max(0, cusumNeg - diff - k);

        if (cusumPos > cusumPosThreshold || cusumNeg > cusumNegThreshold) {
            // reset， 否则异常之后的全部都是异常
            cusumPos = 0;
            cusumNeg = 0;

            return true;
        }

        return false;
    }

    // ---------- ZScore检测 ----------
    private boolean detectZScore(double value, double average, double std) {
        if (std <= 0) {
            return false;
        }

        double z = Math.abs(value - average) / std;
        return z > zScoreThreshold;
    }

    public boolean detect(double value) {
        // ---------- 更新滑动窗口 ----------
        updateWindow(value);

        // 窗口不足则直接返回
        if (window.size() < windowSize) {
            prevValue = value;
            return true;
        }

        // 窗口满的情况

        // 窗口内所有值的平均
        double average = sum / window.size();

        // ---------- 突然跳变检测 ----------
        boolean spike = false;
        if (useSpikeDetect) {
            spike = detectSpike(value);
        }

        // ---------- CUSUM ----------
        // 持续统计“当前值比平均值高多少”
        boolean trend = false;
        if (useCUSUMDetect) {
            trend = detectCUSUM(value, average);
        }

        // ---------- ZScore ----------
        boolean zScore = false;
        if (useZScoreDetect) {
            double variance = (sumSquare / window.size()) - average * average;
            double std = Math.sqrt(Math.max(variance, 0));
            zScore = detectZScore(value, average, std);
        }

        boolean anomaly = false;
        if (spike) {
            anomaly = true;
            failReason = 1;
        }
        if (trend) {
            anomaly = true;
            failReason = 2;
        }
        if (zScore) {
            anomaly = true;
            failReason = 3;
        }

        // 必须连续 N 次异常才报警
        if (anomaly) {
            anomalyCounter++;
        } else {
            anomalyCounter = 0;
        }

        prevValue = value;

        if (isOverCount()) {
            anomalyCounter = 0;

            if (useCd) {
                if (!isOverCD()) {
                    return true;
                }
                lastAlarmTime = System.currentTimeMillis();
            }

            return false;
        }

        return true;
    }
}
