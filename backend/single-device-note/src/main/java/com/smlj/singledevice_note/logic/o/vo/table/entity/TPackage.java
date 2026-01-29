package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@NoArgsConstructor
public class TPackage implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;

    private String package_name;
    private String version_name;
    private String desc;
    private int version;
}

