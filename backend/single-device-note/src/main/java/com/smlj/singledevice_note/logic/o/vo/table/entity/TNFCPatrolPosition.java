package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;

@Component
@Data
public class TNFCPatrolPosition implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id; // 自增id
    private String pos; // 位号
    private String rfid; // 所属巡检点位的rfid
}
