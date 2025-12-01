package com.smlj.lpjtlj.logic.o.vo.table.entity.lp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Data
@Accessors(chain = true)
@NoArgsConstructor
@Document(collection = "workflow_cfg")
public class TlpWorkflowCfg implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    private String _id; // 这个字段名不一定非要是id

    private String cls;
}
