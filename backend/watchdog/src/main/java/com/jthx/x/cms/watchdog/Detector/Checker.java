package com.jthx.x.cms.watchdog.Detector;

import com.jthx.x.cms.watchdog.dao.mapper.SMDSBranchInfoMapper;
import com.jthx.x.cms.watchdog.pojo.ExceptionDetail;
import com.jthx.x.cms.watchdog.pojo.Point;
import com.jthx.x.cms.watchdog.pojo.response.IndicatorResponseInfo;
import com.jthx.x.cms.watchdog.service.SMDSRequestService;
import com.jthx.x.cms.watchdog.service.WebSocketPushService;
import jakarta.websocket.EncodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Service
@Slf4j
@RequiredArgsConstructor
public class Checker {
    private boolean canRuning = false;
    private final SMDSRequestService smdsRequestService;
    private final SMDSRequestService requestService;
    private final SMDSBranchInfoMapper branchInfoMapper;

    // 指标实时数据列表
    private IndicatorResponseInfo responseInfo;

    private int batchIndex = 0;

    // 记录需要异常工况检测的指标
    private List<Point> pointList = new ArrayList<>();
    // 每个指标对应的数据处理类
    private final Map<Integer, TrendJumpDetector> dataHandlerMap = new HashMap<>();

    private boolean prepareForMybatis() {
        if (!pointList.isEmpty()) {
            return false;
        }

        pointList = branchInfoMapper.getAllPoints();
        return !pointList.isEmpty();
    }

    private boolean prepareForDetect() {
        dataHandlerMap.clear();

        for (Point oneIndicator : pointList) {
            TrendJumpDetector dataHandler = new TrendJumpDetector(oneIndicator);
            Integer key = oneIndicator.getId();
            dataHandlerMap.put(key, dataHandler);
        }
        return true;
    }

    public void detect() throws EncodeException {
        for (Point oneIndicator : pointList) {
            TrendJumpDetector dataHandler = dataHandlerMap.get(oneIndicator.getId());

            // 获取对应指标的实时值
            var av = responseInfo.getVByTag(oneIndicator.getTag());
            boolean isNormal = dataHandler.detect(av);

            System.out.println("isNormal:" + isNormal + "  av:" + av);

            if (!isNormal) {
                ExceptionDetail detail = new ExceptionDetail(dataHandler, oneIndicator, av);
                var json = ExceptionDetailEncoder.toJson(detail);
                log.error("id:{} av:{} json:{}", oneIndicator.getId(), av, json);
                /*branchInfoMapper.insertSnap(detail);*/
                WebSocketPushService.sendMsgToAll(detail);
            }
        }
    }

    public void start() {
        log.info("start");
        canRuning = true;
    }

    public void stop() {
        log.info("stop");
        canRuning = false;
    }

    public boolean reloadDB() {
        log.info("reloadDB");
        stop();

        pointList.clear();
        if (!this.prepareForMybatis()) {
            return false;
        }

        this.prepareForDetect();
        this.start();
        this.batchIndex = 0;
        return true;
    }

    public void startMonitoring() {
        log.info("startMonitoring");

        if (!reloadDB()) {
            return;
        }

        while (true) {
            try {
                if (!canRuning) {
                    Thread.sleep(200);
                    continue;
                }

                System.out.println("-------------这是第" + (++batchIndex) + "轮检测-------------");

                // 获取本次的实时点表数据
                responseInfo = requestService.requestSnapshotInfoByPoints(pointList);
                detect();

                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (EncodeException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
