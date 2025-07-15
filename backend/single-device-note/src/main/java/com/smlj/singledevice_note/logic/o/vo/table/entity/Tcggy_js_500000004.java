package com.smlj.singledevice_note.logic.o.vo.table.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
@NoArgsConstructor
public class Tcggy_js_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    @DateTimeFormat("yyyy/MM/dd HH:mm:ss")
    @ExcelProperty("日期")
    private Date dt;
    @ExcelProperty("供货单位")
    private String goods_supplier;
    @ExcelProperty("批号")
    private String wlcc_id;
    @ExcelProperty("车号")
    private String car_no;
    @ExcelProperty("结算数量(吨)")
    private float final_weight;
    @ExcelProperty("扣灰量(吨)")
    private float ash_weight;
    @ExcelProperty("净重(吨)")
    private float weight;
    @ExcelProperty("发气量(升/千克)")
    private int fq;

    @ExcelProperty("基准单价(元/吨)")
    private float base_price;
    @ExcelProperty("考核单价(元/吨)")
    private float price;
    @ExcelProperty("结算总价")
    private float total_price;

    @ExcelIgnore
    private Date gross_dt;
    @ExcelIgnore
    private String library_id;
    @ExcelIgnore
    private Date xy_dt;
    @ExcelIgnore
    private String c_comment;
    @ExcelIgnore
    private String goods_level;

    @ExcelIgnore
    private Date modify_dt;

    // jackson转换为json的时候， 会自动把is/get这种前缀去掉，这里保持字段名原样
    @ExcelIgnore
    @JsonProperty("has_filtered")
    private boolean has_filtered = false; // 是否是数据不合理的行

    @ExcelIgnore
    @JsonProperty("has_matched_lib")
    private boolean has_matched_lib = false; // 是否两个表的数据行匹配

    @ExcelIgnore
    @JsonProperty("has_matched_khl")
    private boolean has_matched_khl = false; // 是否两个表的数据行匹配

    @ExcelProperty("附加-是否结算过")
    @JsonProperty("has_js")
    private boolean has_js = false; // 是否已经被结算过

    public Tcggy_js_500000004(String wlcc_id, String goods_supplier, String car_no, float weight, boolean has_filtered) {
        this.wlcc_id = wlcc_id;
        this.goods_supplier = goods_supplier;
        this.car_no = car_no;
        this.weight = weight;
        this.has_filtered = has_filtered;
    }
}

