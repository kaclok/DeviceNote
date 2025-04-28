package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.dao.mapper.SMDSBranchInfoMapper;
import com.jthx.x.cms.watchdog.pojo.Snapshot;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 该类负责从实时数据库读取数据，并采用滑动窗口的方式计算数据平均变化率
 */
@Component
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DataHandler {
    // 滑动窗口大小，注意在运行过程中不要修改窗口大小
    private int windowSize;

    // 趋势阈值
    private double threshold;
    // 用于存储滑动窗口中的数据
    private Queue<Double> window = new LinkedList<>();
    // 存储窗口中变化率的总和
    private double sumOfChangeRates;
    // 上一个数据点的值
    private double preValue = Double.NaN;
    private double snapshotValue = Double.NaN;
    private int num = 0;

    private Date date;
    private int indicatorId;
    private int deviceId;
    private int branchId;
    private String indicatorName;
    private SMDSBranchInfoMapper branchInfoMapper;
    private DecimalFormat df = new DecimalFormat("0.0000");

    /**
     * 使用滑动窗口，判断指标当前增长率是否超过了阈值
     *
     * @param currentValue
     * @return false表示发生了异常，true表示当前指标正常
     */
    public Boolean detectIndicator(double currentValue) {
        if (!Double.isNaN(preValue)) {
            System.out.println("currentValue: " + currentValue + "preValue: " + preValue);
            double rateOfChange = (currentValue - preValue) / preValue;

            if (window.size() >= windowSize) {
                Double removed = window.poll();
                if (removed != null) {
                    sumOfChangeRates -= removed;
                }
            }
            window.add(rateOfChange);
            sumOfChangeRates += rateOfChange;

            if (window.size() < windowSize) {
                return true;
            }

            double averageChange = sumOfChangeRates / window.size();

            num++;
            System.out.println("--第" + num + "次");
            System.out.println("窗口值为" + window);
            System.out.println("---窗口当前平均增长率-" + averageChange);
            if (!Double.isNaN(currentValue) && !Double.isNaN(averageChange)) {
                Snapshot snapshot = new Snapshot();
                snapshot.setIndicatorId(indicatorId);
                snapshot.setDeviceId(deviceId);
                snapshot.setBranchId(branchId);
                snapshot.setIndicatorName(indicatorName);
                snapshot.setRate(Double.valueOf(df.format(averageChange)));
                snapshot.setValue(currentValue);
                snapshot.setDate(LocalDateTime.now());

                branchInfoMapper.insertSnapshotInfo(snapshot);
            }

            if (Math.abs(averageChange) > threshold) {
                return false;
            } else {
                return true;
            }
        }
        preValue = currentValue;
        return true;
    }
}
