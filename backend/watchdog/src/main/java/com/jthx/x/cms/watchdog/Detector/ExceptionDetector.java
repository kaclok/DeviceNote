package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.dao.mapper.SMDSBranchInfoMapper;
import com.jthx.x.cms.watchdog.pojo.ExceptionInfo;
import com.jthx.x.cms.watchdog.pojo.IndicatorInfo;
import com.jthx.x.cms.watchdog.pojo.response.IndicatorResponseInfo;
import com.jthx.x.cms.watchdog.service.SMDSRequestService;
import com.jthx.x.cms.watchdog.service.WebSocketPushService;
import com.jthx.x.cms.watchdog.util.SMDSSafeAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Service
@Slf4j
public class ExceptionDetector {
    @Autowired
    private SMDSRequestService requestService;
    @Autowired
    private SMDSBranchInfoMapper branchInfoMapper;

    // 指标实时数据列表
    private IndicatorResponseInfo indicatorSnapshotInfo;

    private Thread thread = null;

    // 用来记录每个指标当前的运行状态
    private Map<String, DetectorExceptionStatus> deviceStatus = new HashMap<>();
    // 存储每个指标的最近一次正常的基准值，指标发生异常后，可以依据该值判断指标是否恢复正常
    private Map<String, Double> lastNormalValue = new HashMap<>();
    // 记录当前指标持续发生异常的次数
    private Map<String, Integer> exceptionCount = new HashMap<>();
    //    private Map<String, Boolean> indicatorOpen = new HashMap<>();
    // 记录需要异常工况检测的指标
    private List<IndicatorInfo> indicatorInfoList = new ArrayList<>();

    // 异常列表
    private List<IndicatorInfo> exceptionList = new ArrayList<>();
    // 每个指标对应的数据处理类
    private Map<String, DataHandler> dataHandlerMap = new HashMap<>();

    private void prepareForDetect() {
        this.prepareForMybatis();
        for (IndicatorInfo indicatorInfo : indicatorInfoList) {
            DataHandler dataHandler = new DataHandler();
            dataHandler.setWindowSize(10);
            dataHandler.setThreshold(indicatorInfo.getTrendThreshold());
            dataHandler.setIndicatorId(indicatorInfo.getIndicatorId());
            dataHandler.setIndicatorName(indicatorInfo.getIndicatorName());
            dataHandler.setBranchId(indicatorInfo.getBranchId());
            dataHandler.setDeviceId(indicatorInfo.getDeviceId());
            dataHandler.setBranchInfoMapper(branchInfoMapper);

            String key = this.indicatorKeyWithIndicatorInfo(indicatorInfo);

            dataHandlerMap.put(key, dataHandler);
        }
    }

    private boolean prepareForMybatis() {
        if (!indicatorInfoList.isEmpty()) {
            return true;
        }

        indicatorInfoList = branchInfoMapper.getAllIndicatorInfo();

        System.out.println("indicatorInfoList获取成功");
        return !indicatorInfoList.isEmpty();
    }

    public void stopMonitoring() {
        log.info("stopMonitoring");

        if (thread != null) {
            thread.interrupt();
        }
    }

    public void startMonitoring() {
        log.info("startMonitoring");

        // 模拟监测过程
        stopMonitoring();
        thread = new Thread(() -> {
            // 查询数据库，查询需要进行监测的指标信息
            this.prepareForDetect();
            int num = 0;
            Long lastTime = System.currentTimeMillis();
            while (true) {
                try {
                    System.out.println("这是第" + ++num + "轮检测");
                    Thread.currentThread().sleep(5000);
                    System.out.println("距离上次检测过了" + (System.currentTimeMillis() - lastTime) + "秒");
                    lastTime = System.currentTimeMillis();
                    this.indicatorSnapshotInfo = requestService.requestSnapshotInfo(indicatorInfoList);
                    System.out.println("要进行检测的数据有以下" + this.indicatorSnapshotInfo.getSnapshotMap());

                    exceptionList.clear();
                    this.detectorException();
                    this.handleException();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        thread.start();
    }

    private void detectorException() {
        if (!SMDSSafeAPI.isListNotEmpty(indicatorInfoList)) {
            return;
        }
        if (!SMDSSafeAPI.isMapNotEmpty(dataHandlerMap)) {
            return;
        }

        for (IndicatorInfo indicatorInfo : indicatorInfoList) {
            String key = indicatorKeyWithIndicatorInfo(indicatorInfo);
            DataHandler dataHandler = dataHandlerMap.get(key);
            // 获取对应指标的实时值
            dataHandler.setSnapshotValue(this.indicatorSnapshotWithTag(indicatorInfo.getTag()));
            double snapshot = dataHandler.getSnapshotValue();

            System.out.println("-------------------------------------------");
            System.out.println("指标名称" + indicatorInfo.getIndicatorName());

            // 检测是否超过峰值
            DetectorExceptionStatus status = detectError(snapshot, indicatorInfo);
            if (status == DetectorExceptionStatus.DetectorOverError) {
                changeDetectorStatus(key, status);
                exceptionList.add(indicatorInfo);
                System.out.println("超出报警阈值" + indicatorInfo.getTrendThreshold() + " 当前实时值为 " + snapshot);
                continue;
            }

            DetectorExceptionStatus lastStatus = deviceStatus.get(key);
            Double lastNormal = lastNormalValue.get(key);
            Boolean isException = dataHandler.detectIndicator(snapshot);
            System.out.println(indicatorInfo);
            if (!isException.booleanValue()) {
                addExceptionCount(key);
            } else if (lastStatus != DetectorExceptionStatus.DetectorNormal && lastNormal != null && !Double.isNaN(lastNormal) && (snapshot - lastNormal) / snapshot < indicatorInfo.getTrendThreshold()) { // 如果这次判断中指标是正常的，那么把当前指标和上一次正常的指标做一次变化率计算，如果变化率符合正常，那么就表明当前指标是恢复正常的了
                changeDetectorStatus(key, DetectorExceptionStatus.DetectorNormal);
                exceptionCount.put(key, 0);
            }
            if (isException.booleanValue()) {
                lastNormalValue.put(key, snapshot);
                exceptionCount.put(key, 0);
                deviceStatus.put(key, DetectorExceptionStatus.DetectorNormal);
            }

            if (exceptionCount.get(key) >= indicatorInfo.getExceptionCount() && lastStatus != DetectorExceptionStatus.DetectorOverError) {
                exceptionList.add(indicatorInfo);
                System.out.println("-----发生异常------");
                changeDetectorStatus(key, DetectorExceptionStatus.DetectorOverError);
                continue;
            }
        }
    }

    private double indicatorSnapshotWithTag(String tag) {
        if (!SMDSSafeAPI.isStringNotEmpty(tag)) {
            return 0.f;
        }
        return indicatorSnapshotInfo.snapshotWithTag(tag);
    }

    private DetectorExceptionStatus detectError(double snapshot, IndicatorInfo indicatorInfo) {
        DetectorExceptionStatus status = DetectorExceptionStatus.DetectorNormal;
        if (snapshot >= indicatorInfo.getNormalMax()) {
            status = DetectorExceptionStatus.DetectorOverError;
        }
        return status;
    }

    private void addExceptionCount(String key) {
        int count = 0;
        if (exceptionCount.containsKey(key)) {
            count = exceptionCount.get(key);
        }

        exceptionCount.put(key, count + 1);
    }

    private void changeDetectorStatus(String key, DetectorExceptionStatus status) {
        deviceStatus.put(key, status);
    }

    private String indicatorKeyWithIndicatorInfo(IndicatorInfo indicatorInfo) {
        return indicatorInfo.getIndicatorName() + indicatorInfo.getIndicatorId();
    }

    /**
     * 处理异常发生相关事件
     */
    private void handleException() {
        for (IndicatorInfo exception : exceptionList) {
            insertExceptionInfo(exception);
            WebSocketPushService.sendExceptionMessage(exception.getIndicatorName() + "异常，请及时排查");
        }
    }

    /**
     * 上报异常信息
     *
     * @param indicatorInfo
     */
    private void insertExceptionInfo(IndicatorInfo indicatorInfo) {
        ExceptionInfo exceptionInfo = new ExceptionInfo();
        exceptionInfo.setIndicatorId(indicatorInfo.getIndicatorId());
        exceptionInfo.setIndicatorName(indicatorInfo.getIndicatorName());
        exceptionInfo.setDeviceId(indicatorInfo.getDeviceId());
        exceptionInfo.setBranchId(indicatorInfo.getBranchId());
        LocalDateTime dateTime = LocalDateTime.now();
        exceptionInfo.setDate(dateTime);

        branchInfoMapper.insertExceptionInfo(exceptionInfo);
    }
}