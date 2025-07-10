package com.smlj.singledevice_note.logic.o.vo.table.entity;

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

    private Date date;
    private String goods_supplier;
    private String wlcc_id;
    private String car_no;
    private float final_weight;
    private float ash_weight;
    private float weight;
    private int fq;

    private float base_price;
    private float price;
    private float total_price;

    private String library_id;
    private Date xy_dt;
    private String c_comment;
    private String goods_level;

    private Date modify_dt;

    // jackson转换为json的时候， 会自动把is/get这种前缀去掉，这里保持字段名原样
    @JsonProperty("is_filtered")
    private boolean is_filtered = false; // 是否是数据不合理的行
    @JsonProperty("is_matched")
    private boolean is_matched = false; // 是否两个表的数据行匹配
    @JsonProperty("is_js")
    private boolean is_js = false; // 是否已经被结算过

    public Tcggy_js_500000004(String wlcc_id, String goods_supplier, String car_no, float weight, boolean is_filtered) {
        this.wlcc_id = wlcc_id;
        this.goods_supplier = goods_supplier;
        this.car_no = car_no;
        this.weight = weight;
        this.is_filtered = is_filtered;
    }
}

