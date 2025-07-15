package com.smlj.singledevice_note.core.o.converter.excel;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.ReadConverterContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;

@Slf4j
public class String2TimeConverter implements Converter<Time> {
    @Override
    public Class<?> supportJavaTypeKey() {
        return Time.class;
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
    public Time convertToJavaData(ReadConverterContext<?> context) {
        var timeStr = context.getReadCellData().getStringValue();
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        var arr = timeStr.trim().split(":");
        var l = arr.length;
        if (l < 2) {
            return null;
        }
        String sec = "0";
        String formater = null;
        if (l == 2) {
            formater = "%02d:%02d:00";
        } else {
            formater = "%02d:%02d:%02d";
            sec = arr[2];
        }

        timeStr = String.format(formater, Integer.parseInt(arr[0]), Integer.parseInt(arr[1]), Integer.parseInt(sec));
        return Time.valueOf(timeStr);
    }
}
