package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@Component
@Data
public class TNFCPatrolLine implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private int id; // 自增id
    private String linenum;
    private String linename;
    private Date createtime;
    private String[] pointids;

    private String deptid; // 部门
    private float cycle; // 巡检周期
    private Date begintime; // 该路线巡检开始时间
}

