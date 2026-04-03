package com.jthx.x.cms.watchdog.dao.mapper;

import com.jthx.x.cms.watchdog.pojo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SMDSBranchInfoMapper {
    // 获取公司内所有分厂的所有设备的所有需要进行异常监测的指标的信息
    public List<IndicatorInfo> getAllIndicatorInfo();

    public List<Point> getAllPoints();

    public int insertSnapshotInfo(Snapshot snapshot);

    public int insertExceptionInfo(ExceptionInfo exceptionInfo);

    void ClearPoints();

    public int InsertBatch(List<Point> list);
}
