package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TCGHTUser implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String account;
    private String username;
    private String pwd;
    private String role_code;
    private TCGHTRole role;
}

