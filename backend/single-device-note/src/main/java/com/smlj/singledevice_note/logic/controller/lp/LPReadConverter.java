package com.smlj.singledevice_note.logic.controller.lp;

import org.springframework.beans.factory.ObjectProvider;
import com.smlj.singledevice_note.core.o.converter.MongoDocReadConverter;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TlpBase;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tlp_czp_1;
import com.smlj.singledevice_note.logic.o.vo.table.entity.Tlp_czp_2;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@RequiredArgsConstructor
@Accessors(chain = true)
public class LPReadConverter implements MongoDocReadConverter<TlpBase> {
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    public static Map<Integer, Class<? extends TlpBase>> WORKFLOW_CLS = Map.ofEntries(
            Map.entry(2085547, TlpBase.class),
            Map.entry(2085501, Tlp_czp_1.class),
            Map.entry(2085502, Tlp_czp_2.class)
    );

    @Override
    public TlpBase convert(Document source) {
        if (source.containsKey("workflow_id")) {
            var workflow_id = source.getInteger("workflow_id");
            Class<? extends TlpBase> cls = WORKFLOW_CLS.getOrDefault(workflow_id, TlpBase.class);
            MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
            if (mongoTemplate != null) {
                var r = mongoTemplate.getConverter().read(cls, source);
                return r;
            }
        }

        return null;
    }
}
