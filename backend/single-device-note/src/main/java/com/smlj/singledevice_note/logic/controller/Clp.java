package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.controller.lp.EPtype;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TlpDao;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/lp")
@Tag(name = "Clp", description = "两票系统")
public class Clp {
    private final TlpDao tlpDao;

    @Transactional
    @PostMapping(value = "/storeUser")
    public Result<?> storeUser(@RequestParam(name = "_id") String id, @RequestParam(name = "name") String name) {
        var r = tlpDao.storeUser(id, name);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/findUser")
    public Result<?> findUser(@RequestParam(name = "_id") String id) {
        var r = tlpDao.findUser(id);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/getType")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> getType(@RequestParam(name = "workflow_id") String workflowId) {
        EPtype ep = tlpDao.getCzpList().contains(workflowId) ? EPtype.CZP : EPtype.GZP;
        return Result.success(ep.getType());
    }

    @Transactional
    @PostMapping(value = "/getWorkflows")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> getWorkflows(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType) {
        List<String> wfs = tlpDao.getPs(EPtype.fromValue(formType));
        return Result.success(wfs);
    }

    @Transactional
    @PostMapping(value = "/getCount")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> getCount(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType, @RequestParam(name = "beginTime") String beginTimeStr, @RequestParam(name = "endTime") String endTimeStr) {
        Date beginTime, endTime;
        if (StrUtil.isEmpty(beginTimeStr) || StrUtil.isEmpty(endTimeStr)) {
            beginTime = DateUtil.parse("1970/01/01", "yyyy/MM/dd");
            endTime = DateUtil.parse("2099/01/01", "yyyy/MM/dd");
        } else {
            beginTime = DateUtil.parse(beginTimeStr, "yyyy/MM/dd");
            endTime = DateUtil.parse(endTimeStr, "yyyy/MM/dd");
        }

        Map<String, Long> count = tlpDao.getCount(beginTime, endTime, formType);
        return Result.success(count);
    }

    @Transactional
    @PostMapping(value = "/getList")
    // formType = 0 表示所有， 1是作业票  2是操作票
    public Result<?> getList(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType, @RequestParam(name = "beginTime") String beginTimeStr, @RequestParam(name = "endTime") String endTimeStr, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        Date beginTime, endTime;
        if (StrUtil.isEmpty(beginTimeStr) || StrUtil.isEmpty(endTimeStr)) {
            beginTime = DateUtil.parse("1970/01/01", "yyyy/MM/dd");
            endTime = DateUtil.parse("2099/01/01", "yyyy/MM/dd");
        } else {
            beginTime = DateUtil.parse(beginTimeStr, "yyyy/MM/dd");
            endTime = DateUtil.parse(endTimeStr, "yyyy/MM/dd");
        }

        var ps = tlpDao.getPs(beginTime, endTime, formType, pageNum, pageSize);
        return Result.success(ps);
    }

    @Transactional
    @PostMapping(value = "/storeRecordArchiveTime")
    public Result<?> storeRecordArchiveTime(@RequestParam("_id") String requestId,
                                            @RequestParam(value = "archive_time") Long archiveTimeMS) {
        var doc = tlpDao.getDocRecord(requestId);
        if (doc != null) {
            if (!doc.containsKey("archive_time")) {
                doc.put("archive_time", archiveTimeMS);
            } else {
                doc.replace("archive_time", archiveTimeMS);
            }

            if (!doc.containsKey("status")) {
                doc.put("status", 2);
            } else {
                doc.replace("status", 2);
            }

            tlpDao.storeRecord(doc);
            return Result.success(doc);
        }
        return Result.fail("不存在_id：" + requestId + " 对应的文档");
    }

    @Transactional
    @PostMapping(value = "/getDocRecord")
    public Result<?> getDocRecord(@RequestParam(name = "_id") String requestId) {
        var lp = tlpDao.getDocRecord(requestId);
        return Result.success(lp);
    }

    @Transactional
    @PostMapping(value = "/deleteRecord")
    public Result<?> deleteRecord(@RequestParam("_id") String requestId) {
        var r = tlpDao.deleteRecord(requestId);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/storeRecord")
    public Result<?> storeRecord(@RequestParam Map<String, Object> map) {
        String requestId = map.get("_id").toString();
        var lp = tlpDao.getDocRecord(requestId);
        Long submitTime = Long.valueOf(map.get("submit_time").toString());
        if (lp != null && lp.get("submit_time") != null) {
            submitTime = Long.valueOf(lp.get("submit_time").toString());
        }

        Document doc = new Document(map);
        String createrId = doc.get("create_user_id").toString();
        String createrName = null;
        var user = tlpDao.findUser(createrId);
        if (user != null) {
            createrName = user.getName();
        }

        doc.put("create_user_name", createrName);
        doc.replace("submit_time", submitTime);
        doc.put("status", 1);

        var r = tlpDao.storeRecord(doc);
        return Result.success(r);
    }

    @Data
    @Accessors(chain = true)
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Tp {
        private String workflowId;
        private boolean exists;
        private int eptype;
        private boolean hasRecord;
        private boolean hasSubmitTime;
        private boolean hasArchiveTime;
    }

    @Transactional
    @PostMapping(value = "/getInfo")
    public Result<?> getInfo(@RequestParam(name = "_id") String requestId,
                             @RequestParam(name = "workflow_id") String workflowId) {
        EPtype ep = tlpDao.getCzpList().contains(workflowId) ? EPtype.CZP : EPtype.GZP;
        var doc = tlpDao.getDocRecord(requestId);
        boolean hasArchiveTime = (doc != null) && (doc.containsKey("archive_time"));
        boolean hasSubmitTime = (doc != null) && (doc.containsKey("submit_time"));
        Tp tp = new Tp()
                .setWorkflowId(workflowId)
                .setExists(tlpDao.getAllList().contains(workflowId))
                .setEptype(ep.getType())
                .setHasRecord(doc != null)
                .setHasSubmitTime(hasSubmitTime)
                .setHasArchiveTime(hasArchiveTime);
        return Result.success(tp);
    }
}
