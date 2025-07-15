package com.smlj.singledevice_note.core.o.converter.excel;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.ReadConverterContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class String2DateConverter implements Converter<Date> {
    @Override
    public Class<?> supportJavaTypeKey() {
        return Date.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * 这里读的时候会调用
     *
     * @param context
     * @return
     */
    @Override
    public Date convertToJavaData(ReadConverterContext<?> context) throws ParseException {
        var timeStr = context.getReadCellData().getStringValue();
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        var arr = timeStr.trim().split("/");
        var l = arr.length;
        if (l < 3) {
            return null;
        }

        timeStr = String.format("%04d/%02d/00", Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(arr[2]));
        var sdf = new SimpleDateFormat("yyyy/MM/dd");
        return sdf.parse(timeStr);
    }
}
