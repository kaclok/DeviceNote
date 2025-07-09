package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.sql.Date;

@Component
@Data
public class Tcggy_js_500000004 implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String wlcc_id;
    private String library_id;

    private Date date;
    private String goods_supplier;
    private String car_no;
    private float ash_weight;
    private float weight;
    private float final_weight;

    private int fq;

    private float base_price;
    private float price;
    private float total_price;

    private boolean is_js; // 是否已经被结算过
}

