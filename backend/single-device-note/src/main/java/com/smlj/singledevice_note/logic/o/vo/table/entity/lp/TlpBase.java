package com.smlj.singledevice_note.logic.o.vo.table.entity.lp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.stereotype.Component;

import java.io.Serializable;

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
public class TlpBase implements Serializable {
    // @Serial
    // private static final long serialVersionUID = 1;
    @MongoId
    @Field("_id")
    /*@JsonProperty("_id") 的作用是：
        序列化时：request_id 字段会输出成 JSON 的 _id
        反序列化时：
        JSON 的 _id 会赋值给 Java 的 request_id*/
    private String _id;

    private String workflow_id;

    private Integer group;

    private Long submit_time; // 提交时间
    private Long archive_time; // 归档时间

    private String create_user_id;
    private String create_user_name; // 申请人

    // 1 进行中
    // 2 已归档
    private int status;
}

