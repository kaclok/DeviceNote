package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

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
