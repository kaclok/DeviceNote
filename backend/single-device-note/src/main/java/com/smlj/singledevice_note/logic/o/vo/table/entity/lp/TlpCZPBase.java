package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

@Component
@Data
/*@Document(collection = "reqs")
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE, property = "workflow_id", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Tlp_czp_1.class, name = "Tlp_czp_1"),
        @JsonSubTypes.Type(value = Tlp_czp_2.class, name = "Tlp_czp_2")
})*/
@Accessors(chain = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TlpCZPBase extends TlpBase {
    private String czpz;
    private String flr;
    private String slr;
    private String flsj;
    private String czkssj;
    private String czjssj;

    private String czrw;
}

