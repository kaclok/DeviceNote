package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.pojo.Point;
import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;

@Data
public class TrendJumpDetector {
    private int windowSize = 30;
    private Queue<Double> window = new LinkedList<>();

    private int cusumWindowSize = 20;
    private Queue<Double> incrPosHistory = new LinkedList<>();
    private Queue<Double> incrNegHistory = new LinkedList<>();

    // 10个点里6个异常
    private Queue<Boolean> anomalyWindow = new LinkedList<>();
    private int anomalyWindowSize = 10;

    private double sum = 0;
    private double sumSquare = 0;
    private double prevValue = Double.NaN;

    // CUSUM
    private double cusumPos = 0;
    private double cusumNeg = 0;
    private double cusumK = 0.01; // 容忍误差k
    private double cusumPosThreshold = 0.2;
    private double cusumNegThreshold = 0.2;

    private double windowAverage = 0;

    // 突变阈值
    private double spikeThreshold = 0.3;

    // zScore
    private double zScoreThreshold = 3;

    // ---------- 报警控制 ----------
    private int anomalyCount = 3;

    // ---------- 每次报警间隔多久 ----------
    private long cdMillis = 3 * 60 * 1000;  // 冷却时间（毫秒）
    private long lastAlarmTime = 0;  // 上次报警时间

    private int useSpikeDetect;
    private int useCUSUMDetect;
    private int useZScoreDetect;
    private int useCd;

    private int failReason = -1;

    public boolean isWindowFull() {
        return window.size() >= windowSize;
    }

    public boolean isOverCD() {
        long now = System.currentTimeMillis();
        if (now - lastAlarmTime > cdMillis) {
            return true;
        }
        return false;
    }

    public TrendJumpDetector(Point point) {


        this.windowSize = point.getWindowSize();

        this.useSpikeDetect = point.getUseSpikeDetect();
        this.spikeThreshold = point.getSpikeThreshold();

        this.useCUSUMDetect = point.getUseCUSUMDetect();
        this.cusumK = point.getCusumK();
        this.cusumNegThreshold = this.cusumPosThreshold = point.getCusumThreshold();
        this.cusumWindowSize = point.getCusumWindowSize();

        this.anomalyWindowSize = point.getAnomalyWindowSize();
        this.anomalyCount = point.getAnomalyCount();

        this.useZScoreDetect = point.getUseZScoreDetect();
        this.zScoreThreshold = point.getZScoreThreshold();

        this.useCd = point.getUseCd();
        this.cdMillis = point.getCdMills();
    }

    // ---------- 更新窗口 ----------
    private void addTail(double value) {
        window.add(value);
        sum += value;
        sumSquare += value * value;
    }

    // 超过窗口，则去除首个元素
    private void removeHead() {
        double removed = window.poll();
        sum -= removed;
        sumSquare -= removed * removed;
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

        // 当前增量
        // ---------- 上升趋势 ----------
        double posIncrement = Math.max(0, diff - cusumK);
        // ---------- 下降趋势 ----------
        double negIncrement = Math.max(0, -diff - cusumK);

        // 入队
        incrPosHistory.add(posIncrement);
        incrNegHistory.add(negIncrement);

        // 累加
        cusumPos += posIncrement;
        cusumNeg += negIncrement;

        // 🔥 控制窗口（关键）
        // 累加和-头部的incr
        if (incrPosHistory.size() > cusumWindowSize) {
            cusumPos -= incrPosHistory.poll();
        }
        if (incrNegHistory.size() > cusumWindowSize) {
            cusumNeg -= incrNegHistory.poll();
        }

        // 检测
        if (cusumPos > cusumPosThreshold || cusumNeg > cusumNegThreshold) {
            // reset， 否则异常之后的全部都是异常
            cusumPos = 0;
            cusumNeg = 0;

            incrPosHistory.clear();
            incrNegHistory.clear();

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

    private boolean detectAnomalyWindow(boolean anomaly) {
        anomalyWindow.add(anomaly);
        if (anomalyWindow.size() > anomalyWindowSize) {
            anomalyWindow.poll();
        }

        int count = 0;
        for (boolean a : anomalyWindow) {
            if (a) {
                count++;
            }
        }

        return count >= anomalyCount;
    }

    public boolean detect(double value) {
        // 窗口未满时，直接更新并返回
        if (window.size() < windowSize) {
            addTail(value);
            prevValue = value;
            return true;
        }

        // 窗口已满
        // 先用当前窗口数据检测
        windowAverage = sum / window.size();  // 不含当前 value
        double variance = (sumSquare / window.size()) - windowAverage * windowAverage;
        double std = Math.sqrt(Math.max(variance, 0));

        // 执行各种检测（使用 average 和 std）
        boolean spike = (useSpikeDetect != 0) && detectSpike(value);
        boolean trend = (useCUSUMDetect != 0) && detectCUSUM(value, windowAverage);
        boolean zScore = (useZScoreDetect != 0) && detectZScore(value, windowAverage, std);

        boolean anomaly = spike || trend || zScore;
        if (anomaly) {
            if (spike) failReason = 1;
            else if (trend) failReason = 2;
            else if (zScore) failReason = 3;
        }

        // 检测完成后，再更新窗口
        removeHead();
        addTail(value);

        prevValue = value;

        // 超过异常上限
        if (detectAnomalyWindow(anomaly)) {
            if (useCd != 0) {
                if (!isOverCD()) {
                    // 冷却期内：清空异常窗口，避免累积
                    anomalyWindow.clear();
                    return true;
                }
                lastAlarmTime = System.currentTimeMillis();
            }

            anomalyWindow.clear();
            return false;
        }

        return true;
    }
}
