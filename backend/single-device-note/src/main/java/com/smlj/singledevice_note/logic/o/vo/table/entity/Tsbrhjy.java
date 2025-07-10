package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
public class Tsbrhjy implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private Integer id;
    private String name;
    private String weihao;
    private String size;
    private Date prev_fix_a_time;
    private Integer a_duration;
    private Date prev_fix_b_time;
    private Integer b_duration;
    private String zz_id;
    private String level_id;
}

