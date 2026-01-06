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
import com.smlj.singledevice_note.logic.o.vo.table.entity.TNFCPatrolRecord;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

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

    @Transactional
    @PostMapping(value = "/deleteLine")
    public Result<?> deleteLine(@RequestParam(name = "id") int id) {
        int r = lineDao.delete(id);
        return Result.sf(r > 0);
    }

    @Transactional
    private void connectLinePoint(Integer lineid, String[] pointids, boolean incrOrUpdate) {
        if (incrOrUpdate) {
            for (int i = 0; i < pointids.length; i++) {
                pointDao.updateLineId(pointids[i], lineid);
            }
        } else {
            var line = lineDao.queryById(lineid);
            if (line == null) {
                return;
            }

            // 清空旧数据
            if (line.getPointids() == null || line.getPointids().length == 0) {
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

        connectLinePoint(id, pointids, false);
        int r = lineDao.updateBase(line);
        return Result.sf(r > 0);
    }

    @Transactional
    @PostMapping(value = "/queryRecords")
    public Result<?> queryRecords(@RequestParam(name = "queryByDeptId", required = false) String queryByDeptId,
                                  @RequestParam(name = "queryByStatus", required = false) Integer queryByStatus,
                                  @RequestParam(name = "queryBegin", required = false) String queryBegin,
                                  @RequestParam(name = "queryEnd", required = false) String queryEnd,
                                  @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        Date beginDt, endDt;
        if (StrUtil.isEmpty(queryBegin)) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, -1);  // 减去1天
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            queryBegin = new SimpleDateFormat("yyyy-MM-dd HH").format(calendar.getTime());
        }
        beginDt = DateUtil.parse(queryBegin, "yyyy-MM-dd HH");

        if (StrUtil.isEmpty(queryEnd)) {
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
            calendar.setTime(new Date());
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            queryEnd = new SimpleDateFormat("yyyy-MM-dd HH").format(calendar.getTime());
        }
        endDt = DateUtil.parse(queryEnd, "yyyy-MM-dd HH");
        endDt.setTime(endDt.getTime() + 24 * 3600 * 1000 - 1);

        if (beginDt.getTime() >= endDt.getTime()) {
            var ls = new ArrayList<>();
            return Result.success(new PageSerializable<>(ls));
        }

        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var begin = sdf.format(beginDt);
        var end = sdf.format(endDt);

        PageHelper.startPage(pageNum, pageSize, true, true, true);
        var ls = recordDao.querySeries(queryByDeptId, queryByStatus, begin, end);
        return Result.success(new PageSerializable<>(ls));
    }

    @Transactional
    @PostMapping(value = "/queryLineRecords")
    public Result<?> queryLineRecords(@RequestParam(name = "lineid") int lineid,
                                  @RequestParam(name = "queryBegin", required = false) String queryBegin,
                                  @RequestParam(name = "pageNum", required = false, defaultValue = "0") Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false, defaultValue = "0") Integer pageSize) {
        var line = lineDao.queryById(lineid);
        if(line != null) {
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var begin = sdf.format(queryBegin);
            return Result.success(new PageSerializable<>());
        }
        return Result.fail("不存在该路线");
    }

    @Data
    @NoArgsConstructor
    public static class RecordInfo {
        private TNFCPatrolPoint point;
        private TNFCPatrolRecord record;
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @NoArgsConstructor
    public static class LineInfo {
        private TNFCPatrolLine line;
        private Date time;
        private int finishCnt;
    }

    /*@Transactional
    @PostMapping(value = "/queryPointInfo")
    public Result<?> queryPoints(@RequestParam(name = "rfid") String rfid, @RequestParam(name = "includeLine", required = false) boolean includeLine) {
        var point = pointDao.queryById(rfid);
        if (point == null) {
            return Result.fail("不存在rfid为:" + rfid + "的巡检点");
        }

        PointInfo pi = new PointInfo();
        pi.setPoint(point);

        if (includeLine) {
            TNFCPatrolLine line = null;
            var lineId = point.getLineid();
            if (lineId != null) {
                line = lineDao.queryById(lineId);
            }
            pi.setLine(line);
        }

        return Result.success(pi);
    }

    private Pair<Map<String, Integer>, Integer> _getLineErrorInfo(TNFCPatrolLine line, Date beginDt, Date endDt) {
        int cnt = 0;
        Map<String, Integer> info = new HashMap<>();
        for (var pointId : line.getPointids()) {
            var records = recordDao.queryAll(null, pointId, beginDt, endDt);
            if (records != null && !records.isEmpty()) {
                // 因为结果是order by time desc, 所以第一个是最新的
                info.put(pointId, records.get(0).getErrornum());
            } else {
                info.put(pointId, 0);
            }
        }

        return Pair.of(info, cnt);
    }*/

    @Transactional
    @PostMapping(value = "/queryLinesByDept")
    public Result<?> queryLinesByDept(@RequestParam(name = "deptid") String deptid) {
        ArrayList<LineInfo> ls = new ArrayList<>();

        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var now = new Date();
        var lines = lineDao.queryByDeptId(deptid);
        if (lines != null && !lines.isEmpty()) {
            for (var line : lines) {
                LineInfo one = new LineInfo();
                ls.add(one);

                one.setLine(line);
                var preDt = _getPreDt(line, now);
                one.setTime(preDt);
                var begin = sdf.format(preDt);
                var end = sdf.format(now);
                one.setFinishCnt(recordDao.queryPointRecordCntOfLine(line.getId(), begin, end));
            }
        }

        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/queryPointsInfoByLine")
    public Result<?> queryPointsInfoByLine(@RequestParam(name = "lineid") int lineid,
                                           @RequestParam(name = "queryBegin") String queryBegin) {
        ArrayList<RecordInfo> ls = new ArrayList<>();

        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var now = new Date();
        Date beginDt = DateUtil.parse(queryBegin, "yyyy-MM-dd HH:mm:ss");
        var line = lineDao.queryById(lineid);
        if (line != null && line.getPointids() != null) {
            for (var pointId : line.getPointids()) {
                RecordInfo one = new RecordInfo();
                ls.add(one);

                TNFCPatrolPoint point = pointDao.queryById(pointId);
                one.setPoint(point);

                TNFCPatrolRecord record = null;
                var begin = sdf.format(beginDt);
                var end = sdf.format(now);
                var records = recordDao.queryAll(null, pointId, begin, end);
                if (records != null && !records.isEmpty()) {
                    record = records.get(0);
                }
                one.setRecord(record);
            }
        }

        return Result.success(ls);
    }

    @Transactional
    @PostMapping(value = "/addRecord")
    public Result<?> addRecord(@RequestParam(name = "rfid") String rfid,
                               @RequestParam(name = "person") String person,
                               @RequestParam(name = "content") String content,
                               @RequestParam(name = "errornum") int errornum) {
        if (pointDao.exist(rfid) <= 0) {
            return Result.fail("添加失败，该巡检点id不存在");
        }

        TNFCPatrolRecord record = new TNFCPatrolRecord();
        record.setRfid(rfid);
        record.setPerson(person);
        record.setContent(content);
        record.setErrornum(errornum);
        record.setDotime(new Date());

        int r = recordDao.insert(record);
        return Result.sf(r > 0);
    }

    private Date _getPreDt(TNFCPatrolLine line, Date targetTime) {
        var diff = targetTime.getTime() - line.getBegintime().getTime();
        if (diff > 0) {
            long seconds = diff / 1000;
            long hours = seconds / 3600;
            int cycle = (int) (line.getCycle());
            long cycleSegment = hours / cycle;
            return new Date(line.getBegintime().getTime() + cycleSegment * cycle * 3600 * 1000);
        }
        return null;
    }
}
