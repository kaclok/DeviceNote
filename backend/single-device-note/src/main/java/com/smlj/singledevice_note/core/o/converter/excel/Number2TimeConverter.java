package com.smlj.singledevice_note.core.o.converter.excel;

import com.alibaba.excel.converters.Converter;
import com.alibaba.excel.converters.ReadConverterContext;
import com.alibaba.excel.enums.CellDataTypeEnum;
import lombok.extern.slf4j.Slf4j;

import java.sql.Time;

@Slf4j
public class Number2TimeConverter implements Converter<Time> {
    @Override
    public Class<?> supportJavaTypeKey() {
        return Time.class;
    }

    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.NUMBER;
    }

    /**
     * 这里读的时候会调用
     *
     * @param context
     * @return
     */
    @Override
    public Time convertToJavaData(ReadConverterContext<?> context) {
        var d = context.getReadCellData().getNumberValue().doubleValue();
        return new Time((long) (d * 86400));
    }
}
