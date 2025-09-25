package com.smlj.singledevice_note.logic.controller.lp;

/*
@Data
@Component
@RequiredArgsConstructor
@Accessors(chain = true)
public class LPReadTypeConverter implements MongoDocReadConverter<TlpBase> {
    private final ObjectProvider<MongoTemplate> mongoTemplateProvider;

    public static Map<Integer, Class<? extends TlpBase>> WORKFLOW_CLS = Map.ofEntries(
            Map.entry(2085547, TlpBase.class),
            Map.entry(2085501, Tlp_gzp_2648.class),
    );

    @Override
    public TlpBase convert(Document source) {
        if (source.containsKey("workflow_id")) {
            var workflow_id = source.getInteger("workflow_id");
            Class<? extends TlpBase> cls = WORKFLOW_CLS.getOrDefault(workflow_id, TlpBase.class);
            MongoTemplate mongoTemplate = mongoTemplateProvider.getIfAvailable();
            if (mongoTemplate != null) {
                // 会有不识别Tlp_czp_1等子类的问题
                var r = mongoTemplate.getConverter().read(cls, source);
                return r;
            }
        }

        return null;
    }
}
*/
