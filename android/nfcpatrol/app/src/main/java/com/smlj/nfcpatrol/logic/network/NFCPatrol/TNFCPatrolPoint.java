package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.io.Serializable;
import java.util.Date;

public class TNFCPatrolPoint implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    public String rfid;
    public String pointname;
    public String pointnum;
    public String pointaddr;

    public Date createtime;

    // 冗余设计
    public Integer lineid; // 路线id
}