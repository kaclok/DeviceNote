package com.smlj.singledevice_note.logic.o.vo.table.dao;

import com.mongodb.client.result.DeleteResult;
import com.smlj.singledevice_note.core.setting.mongodb.MongoSetting;
import com.smlj.singledevice_note.logic.controller.lp.EPtype;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.*;
import lombok.Data;
import org.bson.Document;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public TlpDao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public List<String> getPs(EPtype type) {
        String id = "all";
        if (type == EPtype.GZP) {
            id = "gzp";
        } else if (type == EPtype.CZP) {
            id = "czp";
        }

        return _getPs(id);
    }

    private List<String> _getPs(String id) {
        TlpPCfg cfg = mongoTemplate.findById(id, TlpPCfg.class, P_COLLECTION_NAME);
        List<String> arr = new ArrayList<>();
        if (cfg != null) {
            arr = cfg.getWorkflows();
        }
        return arr;
    }

    public TGZPCfg getGZPCfg(String workflowId) {
        TGZPCfg cfg = mongoTemplate.findById(workflowId, TGZPCfg.class, WORKFLOW_COLLECTION_NAME);
        return cfg;
    }

    public TCZPCfg getCZPCfg(String workflowId) {
        TCZPCfg cfg = mongoTemplate.findById(workflowId, TCZPCfg.class, WORKFLOW_COLLECTION_NAME);
        return cfg;
    }

    private Query buildBaseQuery(Integer group, Date begin, Date end, int pType) {
        int gp = group == null ? 1 : group;
        return new Query()
                .addCriteria(Criteria.where("is_test").is("false"))
                .addCriteria(Criteria.where("group").is(gp))
                .addCriteria(Criteria.where("submit_time").gte(begin.getTime()).lt(end.getTime())
                );
    }

    public Map<String, Long> getCount(Integer group, Date begin, Date end, int pType) {
        Map<String, Long> r = new HashMap<>();
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(group, begin, end, pType).addCriteria(Criteria.where("workflow_id").in(getPs(EPtype.GZP)));
            Long count = mongoTemplate.count(query, COLLECTION_NAME);
            r.put("gzp", count);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(group, begin, end, pType).addCriteria(Criteria.where("workflow_id").in(getPs(EPtype.CZP)));
            Long count = mongoTemplate.count(query, COLLECTION_NAME);
            r.put("czp", count);
        }

        return r;
    }

    // todo 如何进行分页查询？
    public Map<String, PageImpl<? extends TlpBase>> getPs(Integer group, Date begin, Date end, int pType, int pageNum, int pageSize) {
        Map<String, PageImpl<? extends TlpBase>> r = new HashMap<>();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("submit_time").descending());
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(group, begin, end, pType).addCriteria(Criteria.where("workflow_id").in(getPs(EPtype.GZP)));
            long count = mongoTemplate.count(query, COLLECTION_NAME);

            query.with(pageable);
            List<TlpGZPBase> ls = mongoTemplate.find(query, TlpGZPBase.class, COLLECTION_NAME);

            var t = new PageImpl<>(ls, pageable, count);
            r.put("gzp", t);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(group, begin, end, pType).addCriteria(Criteria.where("workflow_id").in(getPs(EPtype.CZP)));
            long count = mongoTemplate.count(query, COLLECTION_NAME);

            query.with(pageable);
            List<TlpCZPBase> ls = mongoTemplate.find(query, TlpCZPBase.class, COLLECTION_NAME);

            var t = new PageImpl<>(ls, pageable, count);
            r.put("czp", t);
        }
        return r;
    }

    public Document getDocRecord(String requestId) {
        if (requestId == null) {
            return null;
        }

        return mongoTemplate.findById(requestId, Document.class, COLLECTION_NAME);
    }

    public boolean deleteRecord(String requestId) {
        Query query = new Query(
                Criteria.where("_id")
                        .is(requestId));
        DeleteResult result = mongoTemplate.remove(query, COLLECTION_NAME);
        return result.getDeletedCount() > 0;
    }

    public <T> T storeRecord(T lp) {
        return mongoTemplate.save(lp, COLLECTION_NAME);
    }

    public TlpUser storeUser(String id, String name) {
        TlpUser tlpUser = new TlpUser()
                .set_id(id)
                .setName(name);
        return mongoTemplate.save(tlpUser, USER_COLLECTION_NAME);
    }

    public TlpUser findUser(String id) {
        return mongoTemplate.findById(id, TlpUser.class, USER_COLLECTION_NAME);
    }
}
