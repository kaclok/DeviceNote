package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
public class Twz implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @ExcelProperty("物资名称")
    private String c_name;
    @ExcelProperty("数量")
    private Integer c_number;
    @ExcelProperty("单位")
    private String c_unit;
    @ExcelProperty("规格型号")
    private String c_model;
    @ExcelProperty("申报单位")
    private String c_declarer;
    @ExcelProperty("采购合同编号")
    private String c_ht;
}

