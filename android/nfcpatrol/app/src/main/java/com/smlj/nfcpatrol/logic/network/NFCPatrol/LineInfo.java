package com.smlj.nfcpatrol.logic.network.NFCPatrol;

import java.util.Date;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LineInfo {
    private TNFCPatrolLine line;
    private Date time;
    private int finishCnt;
}
