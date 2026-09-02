package com.smlj.singledevice_note.core.o.from.param;

import com.smlj.singledevice_note.core.o.dto.Operator;
import lombok.Data;

@Data
public class FilterParam {
    private String field;              // 字段名
    private Operator operator;         // 操作符
    private Object left;               // 值
    private Object right;              // 第二个值（用于BETWEEN）
}
