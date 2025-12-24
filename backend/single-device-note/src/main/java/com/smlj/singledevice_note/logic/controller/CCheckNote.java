package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.date.DateUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.core.utils.DateTimeUtil;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCheckBJDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TCheckRecordDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TCheckRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x")
@Tag(name = "CCheckNote", description = "仪表检修记录")
public class CCheckNote {
    private final TCheckRecordDao tCheckRecordDao;
    private final TCheckBJDao tCheckBJDao;

    @Transactional
    @GetMapping(value = "/getList")
    public Result<?> getList(String bgId, Long mills, @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum, @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(new Date(mills));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(calendar.getTime());
        String conds = "bj_id = '" + bgId + "' and '"
                + format +
                "' = (time::date)::timestamp";
        String orderBy = "time desc";

        var bjs = tCheckBJDao.doSelectSimple("t_check_bj", "*", "bj_id = '" + bgId + "'", null);
        if (bjs == null || bjs.isEmpty()) {
            return Result.fail("bgId：" + bgId + "不存在");
        }

        var bj = bjs.get(0);
        var tableName = bj.getTableName();

        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = tCheckRecordDao.doSelectSimple(tableName, "*", conds, orderBy);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @GetMapping(value = "/add")
    public Result<?> add(String bgId, String info) {
        var bjs = tCheckBJDao.doSelectSimple("t_check_bj", "*", "bj_id = '" + bgId + "'", null);
        if (bjs == null || bjs.isEmpty()) {
            return Result.fail("bgId：" + bgId + "不存在");
        }

        var bj = bjs.get(0);
        var tableName = bj.getTableName();

        var t = new TCheckRecord(info);
        t.setBj_id(bgId);

        var id = t.getId();
        if (id != null && tCheckRecordDao.exist(tableName, id) > 0) {
            tCheckRecordDao.update(tableName, t);
        } else {
            t.setTime(new Date());
            tCheckRecordDao.insert(tableName, t);
        }

        return Result.success();
    }

    @Transactional
    @GetMapping(value = "/del")
    public Result<?> del(String bgId, Integer id) {
        var bjs = tCheckBJDao.doSelectSimple("t_check_bj", "*", "bj_id = '" + bgId + "'", null);
        if (bjs == null || bjs.isEmpty()) {
            return Result.fail("bgId：" + bgId + "不存在");
        }
        var bj = bjs.get(0);
        var tableName = bj.getTableName();

        tCheckRecordDao.delete(tableName, id);
        return Result.success();
    }

    @Data
    public static class To_Excel<T> implements Serializable {
        private ArrayList<String> colNames = new ArrayList<String>();
        private ArrayList<ArrayList<String>> rows = new ArrayList<ArrayList<String>>();
    }

    @Transactional
    @GetMapping(value = "/find")
    public Result<?> find(String bgId,
                          String query,
                          Long beginDate,
                          Long endDate,
                          @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                          @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        var bjs = tCheckBJDao.doSelectSimple("t_check_bj", "*", "bj_id = '" + bgId + "'", null);
        if (bjs == null || bjs.isEmpty()) {
            return Result.fail("bgId：" + bgId + "不存在");
        }

        int currentYear = LocalDate.now().getYear();
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date beginTime = DateUtil.parse(currentYear + "/01/01", "yyyy/MM/dd");
        if (beginDate != null) {
            Calendar t = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            t.setTime(new Date(beginDate));
            t.set(Calendar.HOUR_OF_DAY, 0);
            t.set(Calendar.MINUTE, 0);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            beginTime = t.getTime();
        }

        Date endTime = new Date();
        if (endDate != null) {
            Calendar t = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            t.setTime(new Date(endDate));
            t.set(Calendar.HOUR_OF_DAY, 0);
            t.set(Calendar.MINUTE, 0);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            endTime = t.getTime();
        }

        var bj = bjs.get(0);
        var tableName = bj.getTableName();
        String beginFormat = sdf.format(beginTime.getTime());
        String endFormat = sdf.format(endTime.getTime());

        String conds = "'" + beginFormat + "' <= (time::date)::timestamp and (time::date)::timestamp <= '" + endFormat + "'";
        String orderBy = "time desc";

        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = tCheckRecordDao.find(tableName, query, bgId, conds, orderBy);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @GetMapping(value = "/export")
    public Result<?> export(String bgId, Long beginDate, Long endDate) {
        var bjs = tCheckBJDao.doSelectSimple("t_check_bj", "*", "bj_id = '" + bgId + "'", null);
        if (bjs == null || bjs.isEmpty()) {
            return Result.fail("bgId：" + bgId + "不存在");
        }

        var bj = bjs.get(0);
        var tableName = bj.getTableName();

        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar begin = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        begin.setTime(new Date(beginDate));
        begin.set(Calendar.HOUR_OF_DAY, 0);
        begin.set(Calendar.MINUTE, 0);
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);

        String beginFormat = sdf.format(begin.getTime());

        Calendar end = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        end.setTime(new Date(endDate));
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        String endFormat = sdf.format(end.getTime());

        String conds = "'" + beginFormat + "' <= (time::date)::timestamp and (time::date)::timestamp <= '" + endFormat + "'";
        String orderBy = "time desc";
        var ls = tCheckRecordDao.doSelectSimple(tableName, "*", conds, orderBy);
        To_Excel<TCheckRecord> rlt = new To_Excel<>();
        rlt.getColNames().add("id");
        rlt.getColNames().add("班级");
        rlt.getColNames().add("时间");
        rlt.getColNames().add("具体时间");
        rlt.getColNames().add("位号/名字");
        rlt.getColNames().add("故障描述");
        rlt.getColNames().add("维修过程");
        rlt.getColNames().add("维修结果");
        rlt.getColNames().add("作业人员");
        rlt.getColNames().add("技术小结");
        rlt.getColNames().add("是否完成");
        rlt.getColNames().add("批注");

        var sdfSimple = new SimpleDateFormat("yyyy-MM-dd");
        for (var one : ls) {
            ArrayList<String> arr = new ArrayList<>();
            rlt.getRows().add(arr);

            arr.add(one.getId().toString());
            arr.add(one.getBj_id());

            var t = sdfSimple.format(one.getTime());
            arr.add(t);
            t = sdf.format(one.getTime());
            arr.add(t);
            arr.add(one.getC_name());
            arr.add(one.getC_desc());
            arr.add(one.getC_progress());
            arr.add(one.getC_result());
            arr.add(one.getC_person());
            arr.add(one.getC_summary());
            arr.add(one.getC_finish().toString());
            arr.add(one.getC_comment());
        }

        return Result.success(rlt);
    }
}
