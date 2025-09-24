package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.smlj.singledevice_note.core.setting.mongodb.MongoSetting;
import com.smlj.singledevice_note.logic.controller.lp.EPtype;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpPCfg;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpBase;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpUser;
import lombok.Data;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Data
public class TlpDao {
    private static final String COLLECTION_NAME = "reqs";
    private static final String P_COLLECTION_NAME = "p_cfg";
    private static final String WORKFLOW_COLLECTION_NAME = "workflow_cfg";
    private static final String USER_COLLECTION_NAME = "user";

    private MongoTemplate mongoTemplate;

    private List<Integer> gzpList = new ArrayList<>();
    private List<Integer> czpList = new ArrayList<>();
    private List<Integer> allList = new ArrayList<>();

    public TlpDao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;

        this.gzpList = getPs(EPtype.GZP);
        this.czpList = getPs(EPtype.CZP);

        this.allList.addAll(this.gzpList);
        this.allList.addAll(this.czpList);
    }

    public List<Integer> getPs(EPtype type) {
        String id = "all";
        if (type == EPtype.GZP) {
            id = "gzp";
        } else if (type == EPtype.CZP) {
            id = "czp";
        }

        TlpPCfg cfg = mongoTemplate.findById(id, TlpPCfg.class, P_COLLECTION_NAME);
        List<Integer> arr = new ArrayList<>();
        if (cfg != null) {
            arr = cfg.getWorkflows();
        }
        return arr;
    }

    private Query buildBaseQuery(Date begin, Date end, int pType) {
        return new Query().addCriteria(Criteria.where("submit_time")
                .gte(begin.getTime())
                .lte(end.getTime()));
    }

    public Map<String, Long> getCount(Date begin, Date end, int pType) {
        Map<String, Long> r = new HashMap<>();
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.gzpList));
            Long count = Long.valueOf(mongoTemplate.count(query, COLLECTION_NAME));
            r.put("gzp", count);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.czpList));
            Long count = Long.valueOf(mongoTemplate.count(query, COLLECTION_NAME));
            r.put("czp", count);
        }

        return r;
    }

    // todo 如何进行分页查询？
    public Map<String, List<TlpBase>> getPs(Date begin, Date end, int pType, int pageNum, int pageSize) {
        Map<String, List<TlpBase>> r = new HashMap<>();
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.gzpList));
            List<TlpBase> ls = mongoTemplate.find(query, TlpBase.class, COLLECTION_NAME);
            r.put("gzp", ls);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.czpList));
            List<TlpBase> ls = mongoTemplate.find(query, TlpBase.class, COLLECTION_NAME);
            r.put("czp", ls);
        }
        return r;
    }

    // 根据workflowId获取具体类型，从而反序列化
    public TlpBase getOne(String requestId, Integer workflowId) {
        var cls = MongoSetting.WORKFLOW_CLS.get(workflowId);
        return mongoTemplate.findById(requestId, cls, COLLECTION_NAME);
    }

    public <T extends TlpBase> T storeRecord(T lp) {
        return mongoTemplate.save(lp, COLLECTION_NAME);
    }

    public TlpUser storeUser(String id, String name) {
        TlpUser tlpUser = new TlpUser()
                .setId(id)
                .setName(name);
        return mongoTemplate.save(tlpUser, USER_COLLECTION_NAME);
    }

    public TlpUser findUser(String id) {
        return mongoTemplate.findById(id, TlpUser.class, USER_COLLECTION_NAME);
    }
}
