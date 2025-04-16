package com.jthx.x.cms.watchdog.Detector;

public enum DetectorExceptionStatus {
    // 表示当前正常
    DetectorNormal,
    // 表示当前该指标已经出现过指标过高异常
    DetectorOverWarning,
    // 表示当前该指标已确定发生指标过高异常工况
    DetectorOverError,
}
