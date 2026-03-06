package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.dao.mapper.SMDSBranchInfoMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.DecimalFormat;
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

    // 用于存储滑动窗口中的数据
    private Queue<Double> window = new LinkedList<>();
    // 存储窗口中变化率的总和
    private double sumOfChangeRates;
    // 上一个数据点的值
    private double preValue = Double.NaN;
    private double snapshotValue = Double.NaN;
    private int num = 0;

    // 趋势阈值
    private double threshold;
    private Date date;
    private int indicatorId;
    private int deviceId;
    private int branchId;
    private String indicatorName;

    /**
     * 使用滑动窗口，判断指标当前增长率是否超过了阈值
     *
     * @param currentValue
     * @return false表示发生了异常，true表示当前指标正常
     */
    public Boolean detectIndicator(double currentValue) {
        if (!Double.isNaN(preValue)) {
            // 如果是非第一次进来
            System.out.println("currentValue: " + currentValue + "preValue: " + preValue);
            // todo preValue为0怎么处理？
            if(preValue == 0) {

            }
            double rateOfChange = (currentValue - preValue) / preValue;

            // 去头
            if (window.size() >= windowSize) {
                var removed = window.poll();
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
            return Math.abs(averageChange) <= threshold;
        } else {
            // 第一次进来
            preValue = currentValue;
            return true;
        }
    }
}
