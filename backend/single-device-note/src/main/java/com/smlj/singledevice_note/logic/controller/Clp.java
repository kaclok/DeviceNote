package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.setting.mongodb.MongoSetting;
import com.smlj.singledevice_note.logic.controller.lp.EPtype;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TlpDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.lp.TlpBase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    public Result<?> storeUser(@RequestParam(name = "id") String id, @RequestParam(name = "name") String name) {
        var r = tlpDao.storeUser(id, name);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/findUser")
    public Result<?> findUser(@RequestParam(name = "id") String id) {
        var r = tlpDao.findUser(id);
        return Result.success(r);
    }

    @Transactional
    @PostMapping(value = "/getWorkflows")
    // formType = 0 表示所有  1是作业票  2是操作票
    public Result<?> getWorkflows(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType) {
        List<Integer> wfs = tlpDao.getPs(EPtype.fromValue(formType));
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
    public Result<?> getList(@RequestParam(name = "formType", required = false, defaultValue = "0") int formType, @RequestParam(name = "beginTime") Date beginTime, @RequestParam(name = "endTime") Date endTime, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        var ps = tlpDao.getPs(beginTime, endTime, formType, pageNum, pageSize);
        return Result.success(ps);
    }

    @Transactional
    @PostMapping(value = "/getOne")
    public Result<TlpBase> getOne(@RequestParam(name = "requestId") String requestId, @RequestParam(name = "workflowId") Integer workflowId) {
        var lp = tlpDao.getOne(requestId, workflowId);
        return Result.success(lp);
    }

    @Transactional
    @PostMapping(value = "/storeRecord")
    public Result<?> storeRecord(@RequestParam("request_id") String requestId,
                                 @RequestParam("workflow_id") Integer workflowId,
                                 @RequestParam("create_user_id") String createrId,
                                 @RequestParam("submit_time") Long submitTimeMS,
                                 @RequestParam(value = "archive_time", required = false) Long archiveTimeMS) {
        var lp = tlpDao.getOne(requestId, workflowId);
        if (lp == null) { // 第一次进入
            String createrName = null;
            var user = tlpDao.findUser(createrId);
            if (user != null) {
                createrName = user.getName();
            }

            Class<? extends TlpBase> targetClass = MongoSetting.WORKFLOW_CLS.getOrDefault(workflowId, TlpBase.class);
            TlpBase instance = BeanUtils.instantiateClass(targetClass);
            instance.setRequest_id(requestId)
                    .setWorkflow_id(workflowId)
                    .setCreate_user_id(createrId)
                    .setCreate_user_name(createrName);

            if (submitTimeMS != null) {
                instance.setSubmit_time(submitTimeMS);
            }
            if (archiveTimeMS != null) {
                instance.setArchive_time(archiveTimeMS);
            }

            var r = tlpDao.storeRecord(instance);
            return Result.success(r);
        } else {
            lp.setRequest_id(requestId);
            if (lp.getArchive_time() == null && archiveTimeMS != null) {
                lp.setArchive_time(archiveTimeMS);
            }

            var r = tlpDao.storeRecord(lp);
            return Result.success(r);
        }
    }
}
