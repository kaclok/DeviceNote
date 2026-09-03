package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
@NoArgsConstructor
public class TCGHTContract implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    /** 主键（自动生成），即时结算类唯一校验按 id，周期结算类允许 id 重复但 unique_id 不同 */
    private String unique_id;
    private String title;
    private float amount;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_sign;
    private String sign_person;
    private Integer sign_type;
    private String supplier;
    private float paycycle_dh;
    private float paycycle_zb;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_yfk;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_dhk;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_zbj;
    // @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date date_rk;
    private String bz;

    private String pay_type;
    /** 付款类型：1-即时结算类 2-周期结算类 */
    private Integer payment_type;
    private float settle_amount;
    /** 已付款金额（元） */
    private float has_amount;
    private int hq;
    private String date_htyj;
    private String date_fpyj;
    private String date_actual_dh;
    private String date_ruzlyj;

    /**
     * 财务环节进度：0-未完结 1-预付款 2-到货款 3-质保款(完结)
     */
    private Integer finish_step;
    private boolean open_status;
}

