package com.smlj.singledevice_note.logic.o.vo.converter;

import cn.hutool.json.JSONUtil;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TDeviceRecord;
import org.springframework.core.convert.converter.Converter;

public class StringToTDeviceRecord implements Converter<String, TDeviceRecord> {
    @Override
    public TDeviceRecord convert(String source) {
        return JSONUtil.toBean(source, TDeviceRecord.class);
    }
}
