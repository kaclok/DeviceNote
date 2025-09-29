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

    private List<String> gzpList = new ArrayList<>();
    private List<String> czpList = new ArrayList<>();
    private List<String> allList = new ArrayList<>();

    public TlpDao(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;

        this.gzpList = getPs(EPtype.GZP);
        this.czpList = getPs(EPtype.CZP);

        this.allList.addAll(this.gzpList);
        this.allList.addAll(this.czpList);
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

    private Query buildBaseQuery(Date begin, Date end, int pType) {
        return new Query().addCriteria(Criteria.where("submit_time")
                .gte(begin.getTime())
                .lt(end.getTime()));
    }

    public Map<String, Long> getCount(Date begin, Date end, int pType) {
        Map<String, Long> r = new HashMap<>();
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.gzpList));
            Long count = mongoTemplate.count(query, COLLECTION_NAME);
            r.put("gzp", count);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.czpList));
            Long count = mongoTemplate.count(query, COLLECTION_NAME);
            r.put("czp", count);
        }

        return r;
    }

    // todo 如何进行分页查询？
    public Map<String, PageImpl<? extends TlpBase>> getPs(Date begin, Date end, int pType, int pageNum, int pageSize) {
        Map<String, PageImpl<? extends TlpBase>> r = new HashMap<>();
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by("submit_time").descending());
        if (pType == EPtype.GZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.gzpList));
            query.with(pageable);

            List<TlpGZPBase> ls = mongoTemplate.find(query, TlpGZPBase.class, COLLECTION_NAME);
            long totalCount = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TlpGZPBase.class);
            var t = new PageImpl<>(ls, pageable, totalCount);
            r.put("gzp", t);
        }
        if (pType == EPtype.CZP.getType() || pType == EPtype.ALL.getType()) {
            var query = buildBaseQuery(begin, end, pType).addCriteria(Criteria.where("workflow_id").in(this.czpList));
            query.with(pageable);

            List<TlpCZPBase> ls = mongoTemplate.find(query, TlpCZPBase.class, COLLECTION_NAME);
            long totalCount = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TlpCZPBase.class);
            var t = new PageImpl<>(ls, pageable, totalCount);
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
