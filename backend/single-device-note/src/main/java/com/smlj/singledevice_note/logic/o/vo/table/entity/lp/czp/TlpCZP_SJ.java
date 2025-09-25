package com.smlj.singledevice_note.logic.o.vo.table.entity.lp.czp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpCZPBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Component
@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlpCZP_SJ extends TlpCZPBase {
    private String czpz;
    private String flr;
    private String slr;
    private String flsj;
    private String czkssj;
    private String czjssj;

    private String czrw;
}

