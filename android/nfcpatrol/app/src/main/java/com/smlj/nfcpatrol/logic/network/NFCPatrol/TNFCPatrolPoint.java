package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

@Data
public class TNFCPatrolPoint implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String rfid;
    private String pointname;
    private String pointnum;
    private String pointaddr;

    private Date createtime;

    // 冗余设计
    private Integer lineid; // 路线id
}