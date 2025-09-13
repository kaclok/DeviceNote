package com.smlj.singledevice_note.logic.controller.reader;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tcggy_js_500000004;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class _500000004_js_reader extends AnalysisEventListener<Tcggy_js_500000004> {
    /*private int BATCH_COUNT = 1000;*/
    public List<Tcggy_js_500000004> arr = new ArrayList<>();

    public _500000004_js_reader(List<Tcggy_js_500000004> arr) {
        this.arr = arr;
    }

    @Override
    public void invoke(Tcggy_js_500000004 data, AnalysisContext context) {
        if (data == null || data.getDt() == null || !data.isHas_js()) {
            return;
        }

        arr.add(data);
    }

    /*@Override
    public void invokeHead(Map<Integer, ReadCellData<?>> rowData, AnalysisContext context) {
    }*/

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
