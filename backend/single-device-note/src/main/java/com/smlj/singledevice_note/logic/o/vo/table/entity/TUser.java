package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
public class TUser implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String account;
    private String password;
    private Integer auth;
    private Boolean deleted;
}

