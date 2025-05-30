package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.network.SMDSRestTemplate;
import com.jthx.x.cms.watchdog.pojo.IndicatorInfo;
import com.jthx.x.cms.watchdog.pojo.request.IndicatorRequestInfo;
import com.jthx.x.cms.watchdog.pojo.request.IndicatorRequestItem;
import com.jthx.x.cms.watchdog.pojo.response.IndicatorResponseInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SMDSRequestService {
    private static SMDSRestTemplate restTemplate = new SMDSRestTemplate();

    /**
     * 通过这个接口获取实时数据，这个接口里包含访问网络获取实时数据的所有业务信息，包括请求体的构建，最终调用SMDSRestTemplate进行最终的接口信息访问
     */
    public IndicatorResponseInfo requestSnapshotInfo(List<IndicatorInfo> indicatorJoinInfoList) {
        if (indicatorJoinInfoList == null || indicatorJoinInfoList.isEmpty()) {
            // 在这里输入请排查数据库的log，便于维护人员维护
            return null;
        }
        IndicatorRequestInfo requestInfo = new IndicatorRequestInfo();
        List<IndicatorRequestItem> tags = new ArrayList<>();
        requestInfo.setTags(tags);

        // 根据传入的model，构建请求体并model化
        for (IndicatorInfo indicatorInfo : indicatorJoinInfoList) {
            IndicatorRequestItem item = new IndicatorRequestItem();
            item.setNamespace(indicatorInfo.getNamespace());
            item.setTag(indicatorInfo.getTag());
            item.setItems(null);

            tags.add(item);
        }

        IndicatorResponseInfo responseInfo = restTemplate.getIndicatorInfo(requestInfo);
        responseInfo.tryFillSnapshotMap();
        return responseInfo;
    }
}
