package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.Date;

@Component
@Data
public class Tcggy_library_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @ExcelProperty("流水号")
    private String id;
    @ExcelProperty("样品名称")
    private Integer goods_name;
    @ExcelProperty("样品等级")
    private String goods_level;
    @ExcelProperty("采样位置")
    private String cy_position;

    @ExcelProperty("下样时间")
    private Date xy_dt;
    @ExcelProperty("分析时间")
    private Date fx_dt;

    @ExcelProperty("发气量\nL/kg")
    private int fq;
    @ExcelProperty("备注")
    private String comment;
   
    private Date create_time;
}

