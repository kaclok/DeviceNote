package com.smlj.singledevice_note.logic.o.vo.table.entity;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * (TMsg)表实体类
 *
 * @author Cui
 * @since 2025-01-17 16:31:26
 */
@NoArgsConstructor
@Data
public class TDeviceRecord implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private Integer id;
    private String gy_id;
    private String device_id;
    private Date record_time;
    private String c_person;
    private Date c_trouble_time;
    private String c_trouble_xx;
    private String c_trouble_yy;
    private String c_fix_xm;
    private String c_bjxh;
    private String c_fix_data;
    private String c_fix_jl;
    private String c_fix_hs;
    private String c_comment;

    public TDeviceRecord(String json) {
        TDeviceRecord r = JSONUtil.toBean(json, TDeviceRecord.class);
        id = r.id;
        gy_id = r.gy_id;
        device_id = r.device_id;
        record_time = r.record_time;
        c_person = r.c_person;
        c_trouble_time = r.c_trouble_time;
        c_trouble_xx = r.c_trouble_xx;
        c_trouble_yy = r.c_trouble_yy;
        c_fix_xm = r.c_fix_xm;
        c_bjxh = r.c_bjxh;
        c_fix_data = r.c_fix_data;
        c_fix_jl = r.c_fix_jl;
        c_fix_hs = r.c_fix_hs;
        c_comment = r.c_comment;
    }
}

