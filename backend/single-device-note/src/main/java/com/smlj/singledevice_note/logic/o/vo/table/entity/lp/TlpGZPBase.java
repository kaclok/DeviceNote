package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

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
    private String jhgzsj;
    private String gzddhdd1;
    private String gznr1;
}

