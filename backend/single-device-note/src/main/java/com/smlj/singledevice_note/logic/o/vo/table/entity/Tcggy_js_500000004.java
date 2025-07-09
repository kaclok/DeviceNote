package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Data
@NoArgsConstructor
public class Tcggy_js_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String wlcc_id;
    private String library_id;

    private LocalDate date;
    private String goods_supplier;
    private String car_no;
    private float ash_weight;
    private float weight;
    private float final_weight;

    private int fq;

    private float base_price;
    private float price;
    private float total_price;

    private boolean is_filtered; // 是否两个表格可以匹配
    private boolean is_js; // 是否已经被结算过

    private LocalDateTime xy_dt;
    private String c_comment;
    private String goods_level;

    private LocalDateTime modify_time;

    public Tcggy_js_500000004(String wlcc_id, String goods_supplier, String car_no, float weight, boolean is_filtered) {
        this.wlcc_id = wlcc_id;
        this.goods_supplier = goods_supplier;
        this.car_no = car_no;
        this.weight = weight;
        this.is_filtered = is_filtered;
    }
}

