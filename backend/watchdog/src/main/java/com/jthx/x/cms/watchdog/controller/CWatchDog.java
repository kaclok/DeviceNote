package com.jthx.x.cms.watchdog.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.exception.ExcelDataConvertException;
import com.jthx.x.cms.watchdog.Detector.Checker;
import com.jthx.x.cms.watchdog.dao.mapper.SMDSBranchInfoMapper;
import com.jthx.x.cms.watchdog.pojo.Point;
import com.jthx.x.core.o.to.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/watchdog")
public class CWatchDog {
    @Autowired
    private Checker checker;
    private final SMDSBranchInfoMapper branchInfoMapper;

    @Transactional
    @GetMapping("/start")
    public void start() {
        if (checker != null) {
            checker.start();
        }
    }

    @Transactional
    @GetMapping("/stop")
    public void stop() {
        if (checker != null) {
            checker.stop();
        }
    }

    @Transactional
    @GetMapping("/mock")
    public void mock(boolean toMock) {
        if (checker != null) {
            checker.mock(toMock);
        }
    }

    @Transactional
    @GetMapping("/reloadDB")
    public Result<?> reloadDB() {
        if (checker != null) {
            if(checker.reloadDB()) {
                return Result.success();
            }
            return Result.fail("reloadDB failed");
        }
        return Result.fail("checker is null");
    }

    private String parse(MultipartFile file, List<Point> cs) {
        try {
            EasyExcel.read(file.getInputStream(), Point.class, new wzReader(cs)).ignoreEmptyRow(true).autoTrim(true).headRowNumber(1).doReadAll();
        } catch (ExcelDataConvertException edce) {
            return String.format("第%s行，第%s列解析异常", edce.getRowIndex() + 1, edce.getColumnIndex() + 1);
        } catch (IOException e) {
            return e.getMessage();
        }

        return null;
    }

    @Transactional
    @PostMapping("/importExcel")
    public Result<?> importExcel(MultipartFile file) {
        int batchSize = 500; // 每批次插入量

        List<Point> cs = new ArrayList<>();
        var r = parse(file, cs);
        if (r != null) {
            return Result.fail(r);
        }

        branchInfoMapper.ClearPoints();

        // 分批次插入db
        for (int start = 0; start < cs.size(); start += batchSize) {
            int end = start + batchSize;
            end = Math.min(end, cs.size());
            var subList = cs.subList(start, end);

            branchInfoMapper.InsertBatch(subList);
        }
        return Result.success();
    }
}
