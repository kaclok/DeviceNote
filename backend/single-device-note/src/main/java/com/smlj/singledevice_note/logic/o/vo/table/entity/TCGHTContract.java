package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
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
    private String title;
    private float amount;
    private Date date_sign;
    private String sign_person;
    private int sign_type;
    private String supplier;
    private float paycycle_dh;
    private float paycycle_zb;
    private float rate_yfk;
    private float rate_dhk;
    private float rate_zbj;
    private Date date_yfk;
    private Date date_dhk;
    private Date date_zbj;
    private Date date_rk;
    private String bz;

    private String pay_type;
    private float settle_amount;
    private int hq;
    private String date_htyj;
    private String date_fpyj;
    private String date_actual_dh;
    private String date_ruzlyj;

}

