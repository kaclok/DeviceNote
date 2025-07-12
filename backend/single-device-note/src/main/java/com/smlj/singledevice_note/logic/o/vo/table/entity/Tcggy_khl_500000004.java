package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;

@Component
@Data
public class Tcggy_khl_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @DateTimeFormat("yyyy-MM-dd")
    @ExcelProperty("入库日期")
    private Date enter_d;

    @DateTimeFormat("hh-mm-ss")
    @ExcelProperty("时间")
    private Time enter_t;

    @ExcelProperty("车号")
    private String car_no;

    @ExcelProperty("扣灰量/t")
    private float khl;
}

