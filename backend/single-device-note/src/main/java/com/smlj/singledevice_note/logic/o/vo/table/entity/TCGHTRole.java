package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TCGHTRole implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private int id;
    private String role_name;
    private String role_code;
    private int data_scope;
    private String[] perms;
    private String description;
}

