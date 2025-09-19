package com.smlj.singledevice_note.logic.o.vo.table.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@EqualsAndHashCode(callSuper = true)
@Component
@Data
@Accessors(chain = true)
public class Tlp_czp_1 extends TlpBase {
    private String a;
}

