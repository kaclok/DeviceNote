package com.jthx.x.cms.watchdog.controller;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.jthx.x.cms.watchdog.pojo.Point;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class wzReader extends AnalysisEventListener<Point> {
    /*private int BATCH_COUNT = 1000;*/
    public List<Point> arr = new ArrayList<Point>();

    public wzReader(List<Point> arr) {
        this.arr = arr;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    one row value. Is is same as {@link AnalysisContext#readRowHolder()}
     * @param context
     */
    @Override
    public void invoke(Point data, AnalysisContext context) {
        if (data != null) {
            arr.add(data);
        }

        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        /*if (cachedDataList.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            cachedDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
        }*/
    }

    /**
     * 所有数据解析完成了 都会来调用
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
    }

    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        log.error("解析失败:{}", exception.getMessage());
        // https://easyexcel.opensource.alibaba.com/docs/current/quickstart/read
        if (exception instanceof ExcelDataConvertException edce) {
            var msg = String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
            log.error("详细:{}", msg);
            throw edce;
        }

        throw new Exception(exception.getMessage());
    }
}
