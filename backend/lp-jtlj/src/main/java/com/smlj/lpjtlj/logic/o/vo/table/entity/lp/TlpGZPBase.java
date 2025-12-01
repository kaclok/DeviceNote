package com.smlj.lpjtlj.logic.o.vo.table.entity.lp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Component
@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlpGZPBase extends TlpBase {
    private String dw;
    private String gzfzrjhr;

    private String jhkssj;
    private String jhjssj;

    private String bz;
    private String gzddhdd1;
    private String gznr1;
    private String gzddhdd2;
    private String gznr2;
    private String gzddhdd3;
    private String gznr3;
}

