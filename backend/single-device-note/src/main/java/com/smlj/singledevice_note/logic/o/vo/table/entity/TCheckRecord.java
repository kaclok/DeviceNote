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
public class TCheckRecord implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private Integer id;
    private String bj_id;
    private Date time;
    private String c_name;
    private String c_desc;
    private String c_progress;
    private String c_result;
    private String c_person;
    private String c_summary;
    private Boolean c_finish;
    private String c_comment;

    public TCheckRecord(String json) {
        TCheckRecord r = JSONUtil.toBean(json, TCheckRecord.class);
        id = r.id;
        bj_id = r.bj_id;
        time = r.time;
        c_name = r.c_name;
        c_person = r.c_person;
        c_desc = r.c_desc;
        c_progress = r.c_progress;
        c_result = r.c_result;
        c_person = r.c_person;
        c_summary = r.c_summary;
        c_finish = r.c_finish;
        c_comment = r.c_comment;
    }
}

