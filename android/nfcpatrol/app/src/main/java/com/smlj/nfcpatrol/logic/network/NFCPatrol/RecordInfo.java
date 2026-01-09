package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.io.Serializable;

import lombok.Data;

@Data
public class RecordInfo implements Serializable {
    private TNFCPatrolPoint point;
    private TNFCPatrolRecord record;
}
