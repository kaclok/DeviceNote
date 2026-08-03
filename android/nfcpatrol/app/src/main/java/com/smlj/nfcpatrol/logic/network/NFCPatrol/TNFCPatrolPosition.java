package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.io.Serializable;

import lombok.Data;

@Data
public class TNFCPatrolPosition implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id; // 自增id
    private String pos; // 位号
    private String rfid; // 所属巡检点位的rfid
}