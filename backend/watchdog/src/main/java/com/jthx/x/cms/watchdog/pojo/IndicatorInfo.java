package com.jthx.x.cms.watchdog.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Component
public class IndicatorInfo {
    /**
     * 指标正常区间，指标小于
     */
    private double normalMin;
    private double normalMax;
    // 异常增长率
    private double abnormalIncrRate;
    // 异常衰减率
    private double abnormalDecrRate;
    // 指标id
    private int indicatorId;
    // 指标名
    private String indicatorName;
    // 指标对应设备id
    private int deviceId;
    // 指标对应设备对应的分厂id
    private int branchId;
    // 异常发生上限后报警
    private int exceptionCount;
    // 趋势阈值
    private double trendThreshold;
    // 指标域
    private String namespace;
    // 指标字段
    private String tag;

    public Boolean infoValid() {
        return this.abnormalIncrRate > 0 &&
                this.indicatorId > 0 &&
                this.indicatorName != null &&
                this.deviceId > 0 &&
                this.branchId > 0 &&
                this.exceptionCount > 0 &&
                this.trendThreshold > 0 &&
                this.namespace != null &&
                this.tag != null;
    }
}
