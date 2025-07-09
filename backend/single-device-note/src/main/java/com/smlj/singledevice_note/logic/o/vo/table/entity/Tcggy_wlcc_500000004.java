package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Component
@Data
public class Tcggy_wlcc_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @ExcelProperty("送货通知单号")
    private String wlcc_id;
    @ExcelProperty("采购订单号")
    private String cdd_id;
    @ExcelProperty("合同单号")
    private String ht_id;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("完成时间")
    private LocalDateTime finish_time;

    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("货物名称")
    private String goods_name;
    @ExcelProperty("车牌号")
    private String car_no;
    @ExcelProperty("送货单位")
    private String goods_supplier;

    @ExcelProperty("净重")
    private float diff_weight;
    @ExcelProperty("毛重")
    private float gross_weight;
    @ExcelProperty("皮重")
    private float tare_weight;
    @ExcelProperty("结算吨位")
    private float r_weight;

    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("派车时间")
    private LocalDateTime send_time;
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("签到时间")
    private LocalDateTime sign_time;
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("入场时间")
    private LocalDateTime enter_time;
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("过毛时间")
    private LocalDateTime gross_time;
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("过皮时间")
    private LocalDateTime tare_time;

    @ExcelProperty("司机")
    private String driver_name;
    @ExcelProperty("手机号")
    private String driver_phone;

    // 是否处理过，一般都是因为行数据不对导致程序没有处理，直接过滤掉了
    private boolean is_filtered = false;
}

