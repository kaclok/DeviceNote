package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;

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
public class TlpBase implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    @Field("_id")
    private String request_id; // 这个字段名不一定非要是id

    private int workflow_id;

    private Long submit_time; // 提交时间
    private Long archive_time; // 归档时间

    private String create_user_id;
    private String create_user_name; // 申请人
}

