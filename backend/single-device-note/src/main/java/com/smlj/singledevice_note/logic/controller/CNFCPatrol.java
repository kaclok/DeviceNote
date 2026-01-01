package com.smlj.singledevice_note.logic.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageSerializable;
import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TNFCPatrolDeptDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TNFCPatrolLineDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TNFCPatrolPointDao;
import com.smlj.singledevice_note.logic.o.vo.table.dao.TNFCPatrolRecordDao;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolLine;
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolPoint;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/nfcPatrol")
@Tag(name = "CNFCPatrol", description = "巡检系统")
public class CNFCPatrol {
    private final TNFCPatrolPointDao pointDao;
    private final TNFCPatrolLineDao lineDao;
    private final TNFCPatrolRecordDao recordDao;
    private final TNFCPatrolDeptDao deptDao;

    @Transactional
    @GetMapping(value = "/queryDepts")
    public Result<?> queryDepts() {
        var ls = deptDao.queryAll();
        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/queryUnusedPoints")
    public Result<?> queryUnusedPoints(@RequestParam(name = "queryByNum", required = false) String queryByNum,
                                       @RequestParam(name = "queryByName", required = false) String queryByName,
                                       @RequestParam(name = "queryByRfId", required = false) String queryByRfId,
                                       @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                       @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = pointDao.queryUnused(queryByNum, queryByName, queryByRfId);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/queryRelativedPoints")
    public Result<?> queryRelativedPoints(@RequestParam(name = "id", required = false) int id,
                                          @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                          @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = lineDao.queryRelativedPoints(id);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/queryPoints")
    public Result<?> queryPoints(@RequestParam(name = "queryByNum", required = false) String queryByNum,
                                 @RequestParam(name = "queryByName", required = false) String queryByName,
                                 @RequestParam(name = "queryByRfId", required = false) String queryByRfId,
                                 @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                 @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = pointDao.queryAll(queryByNum, queryByName, queryByRfId);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/deletePoint")
    public Result<?> deletePoint(@RequestParam(name = "rfid") String rfid) {
        int r = pointDao.delete(rfid);
        return Result.sf(r > 0);
    }

    @Transactional
    @PostMapping(value = "/addPoint")
    public Result<?> addPoint(@RequestParam(name = "rfid") String rfid,
                              @RequestParam(name = "pointname") String pointname,
                              @RequestParam(name = "pointnum") String pointnum,
                              @RequestParam(name = "pointaddr") String pointaddr) {

        if (pointDao.exist(rfid) > 0) {
            return Result.fail("添加失败，已存在rfid为:" + rfid + "的巡检点");
        }

        var point = new TNFCPatrolPoint();
        point.setRfid(rfid);
        point.setPointname(pointname);
        point.setPointnum(pointnum);
        point.setPointaddr(pointaddr);
        point.setCreatetime(new Date());
        int r = pointDao.insert(point);
        if (r > 0) {
            return Result.success();
        }
        return Result.fail("添加失败，已存在rfid为:" + rfid + "的巡检点");
    }

    @Transactional
    @PostMapping(value = "/updatePoint")
    public Result<?> updatePoint(@RequestParam(name = "rfid") String rfid,
                                 @RequestParam(name = "pointname") String pointname,
                                 @RequestParam(name = "pointnum") String pointnum,
                                 @RequestParam(name = "pointaddr") String pointaddr) {
        var point = new TNFCPatrolPoint();
        point.setRfid(rfid);
        point.setPointname(pointname);
        point.setPointnum(pointnum);
        point.setPointaddr(pointaddr);
        int r = pointDao.updateBase(point);
        if (r > 0) {
            return Result.success();
        }
        return Result.fail("编辑失败，rfId不可修改");
    }

    @Transactional
    @PostMapping(value = "/queryLines")
    public Result<?> queryLines(@RequestParam(name = "queryByNum", required = false) String queryByNum,
                                @RequestParam(name = "queryByName", required = false) String queryByName,
                                @RequestParam(name = "queryByDeptId", required = false) String queryByDeptId,
                                @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = lineDao.queryAll(queryByNum, queryByName, queryByDeptId);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/queryPointsByLine")
    public Result<?> queryPointsByLine(@RequestParam(name = "id", required = false) int id,
                                       @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                       @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = lineDao.queryPointsByLine(id);
        return Result.success(new PageSerializable<>(ls));
    }

    private ArrayList<TNFCPatrolPoint> _queryPointsByLine(int id) {
        var ls = new ArrayList<TNFCPatrolPoint>();
        var line = lineDao.queryById(id);
        if(line != null && line.getPointids() != null && line.getPointids().length > 0) {
            for(var pointid : line.getPointids()) {
                var point = pointDao.queryById(pointid);
                ls.add(point);
            }
        }
        return ls;
    }

    @Transactional
    @PostMapping(value = "/deleteLine")
    public Result<?> deleteLine(@RequestParam(name = "id") int id) {
        int r = lineDao.delete(id);
        return Result.sf(r > 0);
    }

    @Transactional
    private void connectLinePoint(Integer lineid, String[] pointids, boolean incrOrUpdate) {
        if(incrOrUpdate) {
            for (int i = 0; i < pointids.length; i++) {
                pointDao.updateLineId(pointids[i], lineid);
            }
        }
        else {
            var line = lineDao.queryById(lineid);
            if (line == null) {
                return;
            }

            // 清空旧数据
            if(line.getPointids() == null ||  line.getPointids().length == 0) {
                return;
            }
            var ls = line.getPointids();
            for (int i = 0; i < ls.length; i++) {
                pointDao.updateLineId(ls[i], null);
            }

            // 更新新数据
            ls = pointids;
            for (int i = 0; i < ls.length; i++) {
                pointDao.updateLineId(ls[i], lineid);
            }
        }
    }

    @Transactional
    @PostMapping(value = "/addLine")
    public Result<?> addLine(@RequestParam(name = "linenum") String linenum,
                             @RequestParam(name = "linename") String linename,
                             @RequestParam(name = "deptid") String deptid,
                             @RequestParam(name = "cycle") Float cycle,
                             @RequestParam(name = "begintime") String begintime,
                             @RequestParam(name = "pointids") String[] pointids) {
        var line = new TNFCPatrolLine();
        line.setLinename(linename);
        line.setLinenum(linenum);
        line.setDeptid(deptid);
        line.setCycle(cycle);
        line.setPointids(pointids);
        line.setCreatetime(new Date());

        var dt = DateUtil.parse(begintime, "yyyy-MM-dd HH");
        line.setBegintime(dt);

        int r = lineDao.insert(line);
        if (r > 0) {
            connectLinePoint(line.getId(), pointids, true);
        }
        return Result.sf(r > 0);
    }

    @Transactional
    @PostMapping(value = "/updateLine")
    public Result<?> updateLine(@RequestParam(name = "id") int id,
                                @RequestParam(name = "linenum") String linenum,
                                @RequestParam(name = "linename") String linename,
                                @RequestParam(name = "deptid") String deptid,
                                @RequestParam(name = "cycle") Float cycle,
                                @RequestParam(name = "begintime") String begintime,
                                @RequestParam(name = "pointids") String[] pointids) {
        var line = new TNFCPatrolLine();
        line.setId(id);
        line.setLinename(linename);
        line.setLinenum(linenum);
        line.setDeptid(deptid);
        line.setCycle(cycle);
        line.setPointids(pointids);
        var dt = DateUtil.parse(begintime, "yyyy-MM-dd HH");
        line.setBegintime(dt);

        int r = lineDao.updateBase(line);
        if (r > 0) {
            connectLinePoint(id, pointids, false);
        }
        return Result.sf(r > 0);
    }

    @Transactional
    @PostMapping(value = "/queryRecords")
    public Result<?> queryRecords(@RequestParam(name = "queryByDeptId", required = false) String queryByDeptId,
                                  @RequestParam(name = "queryByPerson", required = false) String queryByPerson,
                                  @RequestParam(name = "queryBegin", required = false) String queryBegin,
                                  @RequestParam(name = "queryEnd", required = false) String queryEnd,
                                  @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        Date beginDt, endDt;
        if(StrUtil.isEmpty(queryBegin)) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -2);  // 减去2天
            queryBegin = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        }
        beginDt = DateUtil.parse(queryBegin, "yyyy-MM-dd");

        if(StrUtil.isEmpty(queryEnd)) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            calendar.setTime(new Date());
            queryEnd = new SimpleDateFormat("yyyy-MM-dd").format(calendar.getTime());
        }
        endDt = DateUtil.parse(queryEnd, "yyyy-MM-dd");
        endDt.setTime(endDt.getTime() + 24 * 3600 * 1000 - 1);

        if (beginDt.getTime() >= endDt.getTime()) {
            var ls = new ArrayList<>();
            return Result.success(new PageSerializable<>(ls));
        }

        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = recordDao.queryAll(queryByDeptId, queryByPerson, beginDt, endDt);
        return Result.success(new PageSerializable<>(ls));
    }
}
