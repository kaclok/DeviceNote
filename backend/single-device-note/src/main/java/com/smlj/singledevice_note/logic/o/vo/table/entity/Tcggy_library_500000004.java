package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
public class Tcggy_library_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @ExcelProperty(index = 7)
    private String id;
    @ExcelProperty(index = 1)
    private String goods_name;
    @ExcelProperty(index = 2)
    private String goods_level;
    @ExcelProperty(index = 3)
    private String cy_position;

    @DateTimeFormat("yyyy-MM-dd HH:mm")
    @ExcelProperty(index = 4)
    private Date xy_dt;
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty(index = 5)
    private Date fx_dt;

    @ExcelProperty(index = 6)
    private int fq;

    // comment是postgre数据库的关键字，所有不能自定义,故用c_commecnt
    // 不知道为什么根据“备注”总是读取失败为null, 所以用index读取
    // 可能是因为列顺序不是一一对应导致的
    @ExcelProperty(index = 8)
    private String c_commecnt;

    private boolean is_ds = false; // 是否是：神木电石公司
    private boolean is_handled = false; // 是否进行过表格对比
}

