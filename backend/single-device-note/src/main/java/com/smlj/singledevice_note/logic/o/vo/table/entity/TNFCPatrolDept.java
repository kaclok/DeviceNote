package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

@Component
@Data
public class TNFCPatrolDept implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String id; // 自增id
    private String name;
}

