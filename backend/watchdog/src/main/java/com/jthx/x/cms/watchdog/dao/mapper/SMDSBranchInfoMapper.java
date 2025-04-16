package com.jthx.x.cms.watchdog.dao.mapper;

import com.jthx.x.cms.watchdog.pojo.ExceptionInfo;
import com.jthx.x.cms.watchdog.pojo.IndicatorInfo;
import com.jthx.x.cms.watchdog.pojo.Snapshot;

import java.util.List;

public interface SMDSBranchInfoMapper {

    // 获取公司内所有分厂的所有设备的所有需要进行异常监测的指标的信息
    public List<IndicatorInfo> getAllIndicatorInfo();

    public int insertSnapshotInfo(Snapshot snapshot);

    public int insertExceptionInfo(ExceptionInfo exceptionInfo);
}
