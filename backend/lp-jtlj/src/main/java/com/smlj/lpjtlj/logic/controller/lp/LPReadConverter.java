package com.smlj.lpjtlj.logic.controller.lp;

/*@Data
@Component
@RequiredArgsConstructor
@Accessors(chain = true)
public class LPReadConverter implements MongoDocReadConverter<TlpBase> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public TlpBase convert(Document source) {
        if (source.containsKey("workflow_id")) {
            var workflowId = source.getInteger("workflow_id");
            Class<? extends TlpBase> targetClass = MongoSetting.WORKFLOW_CLS.getOrDefault(workflowId, TlpBase.class);
            var t = mapper.convertValue(source, targetClass);
            return t;
        }
        return null;
    }
}*/

