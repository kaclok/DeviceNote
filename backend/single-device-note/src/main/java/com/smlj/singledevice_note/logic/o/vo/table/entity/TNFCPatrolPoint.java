package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

@Component
@Data
@NoArgsConstructor
public class TNFCPatrolPoint implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String rfid;
    private String pointname;
    private String pointnum;
    private String pointaddr;

    private Date createtime;

    // 冗余设计
    private String lineid; // 路线id
}

