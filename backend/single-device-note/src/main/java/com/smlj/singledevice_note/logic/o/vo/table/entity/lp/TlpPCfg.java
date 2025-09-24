package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;

@Component
@Data
@Accessors(chain = true)
@NoArgsConstructor
public class TlpPCfg implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    private String id; // 这个字段名不一定非要是id

    private List<Integer> workflows;
}
