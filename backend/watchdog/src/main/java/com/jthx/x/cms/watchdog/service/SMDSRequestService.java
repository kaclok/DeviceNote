package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.network.SMDSRestTemplate;
import com.jthx.x.cms.watchdog.pojo.IndicatorInfo;
import com.jthx.x.cms.watchdog.pojo.request.IndicatorRequestInfo;
import com.jthx.x.cms.watchdog.pojo.request.IndicatorRequestItem;
import com.jthx.x.cms.watchdog.pojo.response.IndicatorResponseInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SMDSRequestService {
    private static SMDSRestTemplate restTemplate;

    {
        restTemplate = new SMDSRestTemplate();
    }

    /**
     * 通过这个接口获取实时数据，这个接口里包含访问网络获取实时数据的所有业务信息，包括请求体的构建，最终调用SMDSRestTemplate进行最终的接口信息访问
     */
    public IndicatorResponseInfo requestSnapshotInfo(List<IndicatorInfo> indicatorInfoList) {
        if (indicatorInfoList == null || indicatorInfoList.size() == 0) {
            // 在这里输入请排查数据库的log，便于维护人员维护
            return null;
        }
        IndicatorRequestInfo requestInfo = new IndicatorRequestInfo();
        List<IndicatorRequestItem> items = new ArrayList<>();
        requestInfo.setTags(items);

        // 根据传入的model，构建请求体并model化
        for (IndicatorInfo indicatorInfo : indicatorInfoList) {
            IndicatorRequestItem item = new IndicatorRequestItem();
            item.setTag(indicatorInfo.getTag());
            item.setItems(null);
            item.setNamespace(indicatorInfo.getNamespace());
            items.add(item);
        }

        IndicatorResponseInfo responseInfo = restTemplate.getIndicatorInfo(requestInfo);
        responseInfo.tryFillSnapshotMap();
        return responseInfo;
    }
}
