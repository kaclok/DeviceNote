package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.setting.mongodb.MongoSetting;
import com.smlj.singledevice_note.logic.controller.lp.EPtype;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TlpDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpBase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
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
    public Result<?> getCount(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType, @RequestParam(name = "beginTime") Date beginTime, @RequestParam(name = "endTime") Date endTime) {
        Map<String, Long> count = tlpDao.getCount(beginTime, endTime, formType);
        return Result.success(count);
    }

    @Transactional
    @PostMapping(value = "/getList")
    // formType = 0 表示所有， 1是作业票  2是操作票
    public Result<?> getList(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType, @RequestParam(name = "beginTime") Date beginTime, @RequestParam(name = "endTime") Date endTime, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize) {
        var ps = tlpDao.getPs(beginTime, endTime, formType, pageNum, pageSize);
        return Result.success(ps);
    }

    @Transactional
    @PostMapping(value = "/storeRecordArchiveTime")
    public Result<?> storeRecordArchiveTime(@RequestParam("_id") String requestId,
                                            @RequestParam("workflow_id") String workflowId,
                                            @RequestParam(value = "archive_time") Long archiveTimeMS) {
        var lp = tlpDao.getOneRecord(requestId, workflowId);
        if (lp != null && lp.getWorkflow_id().equals(workflowId)) {
            lp.setArchive_time(archiveTimeMS);
            tlpDao.storeRecord(lp);
            return Result.success(lp);
        }
        return Result.fail("不存在_id：" + requestId + "  workflow_id:" + workflowId + " 对应的文档");
    }

    @Transactional
    @PostMapping(value = "/getOneRecord")
    public Result<TlpBase> getOneRecord(@RequestParam(name = "_id") String requestId, @RequestParam(name = "workflow_id") String workflowId) {
        var lp = tlpDao.getOneRecord(requestId, workflowId);
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
    public Result<?> storeRecord(@RequestParam("workflow_id") String workflowId,
                                 @RequestParam Map<String, Object> map) {
        String requestId = map.get("_id").toString();

        var lp = tlpDao.getOneRecord(requestId, workflowId);
        if (lp == null) { // 第一次进入
            Class<? extends TlpBase> cls = MongoSetting.WORKFLOW_CLS.getOrDefault(workflowId, TlpBase.class);
            TlpBase t = BeanUtil.mapToBean(map, cls, false, null);

            String createrId = t.getCreate_user_id();
            String createrName = null;
            var user = tlpDao.findUser(createrId);
            if (user != null) {
                createrName = user.getName();
            }

            t.setCreate_user_name(createrName);
            var r = tlpDao.storeRecord(t);
            return Result.success(r);
        }

        return Result.fail("已存在_id：" + requestId + "  workflow_id:" + workflowId + " 对应的文档");
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
        private boolean hasArchiveTime;
    }

    @Transactional
    @PostMapping(value = "/getInfo")
    public Result<?> getInfo(@RequestParam(name = "_id") String requestId,
                             @RequestParam(name = "workflow_id") String workflowId) {
        EPtype ep = tlpDao.getCzpList().contains(workflowId) ? EPtype.CZP : EPtype.GZP;
        TlpBase lp = tlpDao.getOneRecord(requestId, workflowId);
        boolean hasArchiveTime = false;
        if (lp != null) {
            hasArchiveTime = lp.getArchive_time() != null;
        }

        Tp tp = new Tp()
                .setWorkflowId(workflowId)
                .setExists(tlpDao.getAllList().contains(workflowId))
                .setEptype(ep.getType())
                .setHasRecord(lp != null)
                .setHasArchiveTime(hasArchiveTime);
        return Result.success(tp);
    }
}
