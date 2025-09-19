package com.smlj.singledevice_note.logic.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TlpDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TlpBase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/lp")
@Tag(name = "Clp", description = "两票系统")
public class Clp {
    private final TlpDao tlpDao;

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
    public Result<TlpBase> getOne(@RequestParam(name = "requestId") Integer requestId, @RequestParam(name = "workflowId") Integer workflowId) {
        var lp = tlpDao.getOne(requestId, workflowId);
        return Result.success(lp);
    }
}
