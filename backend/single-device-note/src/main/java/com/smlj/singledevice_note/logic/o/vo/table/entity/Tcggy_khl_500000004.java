package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
public class Tcggy_khl_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    // 要求单元格格式为：日期类型
    @DateTimeFormat("yyyy/m/d")
    @ExcelProperty(value = "入库日期"/*, converter = String2DateConverter.class*/)
    private Date enter_d;

    // 要求单元格格式为：日期类型
    @DateTimeFormat("h:mm:ss")
    @ExcelProperty(value = "时间"/*, converter = String2TimeConverter.class*/)
    private Date enter_t;

    @ExcelProperty("车号")
    private String car_no;

    @ExcelProperty("重量/t")
    private float diff_weight;

    @ExcelProperty("扣灰量/t")
    private float khl;

    @ExcelIgnore
    private boolean is_matched = false; // 是否两个表的数据行匹配过
}

