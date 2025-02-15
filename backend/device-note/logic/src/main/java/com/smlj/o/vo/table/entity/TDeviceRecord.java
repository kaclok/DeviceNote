package com.smlj.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

/**
 * (TMsg)表实体类
 *
 * @author Cui
 * @since 2025-01-17 16:31:26
 */
@Data
public class TDeviceRecord implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id;
    private String gy_id;
    private String device_id;
    private Date record_time;
    private String c_person;
    private String c_trouble_time;
    private String c_trouble_xx;
    private String c_trouble_yy;
    private String c_fix_xm;
    private String c_bjxh;
    private String c_fix_jl;
    private String c_fix_hs;
    private String c_comment;
}

