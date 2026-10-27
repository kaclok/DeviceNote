package com.smlj.singledevice_note.gb.service;

import com.smlj.singledevice_note.gb.dao.GbDao;
import com.smlj.singledevice_note.gb.o.GbLogLine;
import com.smlj.singledevice_note.gb.o.GbTimeline;
import com.smlj.singledevice_note.gb.o.GbWeighEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class GbService {
    private final GbDao gbDao;

    /** 过磅软件日志行的「单号/车牌」提取规则：自动数据保存成功！单号[x],车牌号[y] */
    private static final Pattern EVENT_RE =
            Pattern.compile("单号\\[([^\\]]+)\\]\\s*,\\s*车牌号\\[([^\\]]+)\\]");
    /** 监控器上报的时间格式：月/日/小时不补零 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss");

    /**
     * 接收监控器批量上报的日志行：原始行全量落库（fp 幂等去重），
     * 能解析出单号+车牌的行额外写一条过磅事件。
     * ⚠️ 不加 @Transactional：本服务走 @DS("gb") 独立库，事务内切库不生效。
     */
    public Map<String, Object> receive(List<GbLogLine> lines) {
        List<GbLogLine> valid = new ArrayList<>();
        List<GbWeighEvent> events = new ArrayList<>();
        if (lines != null) {
            for (GbLogLine it : lines) {
                if (it == null || it.getNo() == null || it.getFp() == null || it.getFp().isBlank()) continue;
                // 限制极端长行，避免异常内容撑爆
                if (it.getContent() != null && it.getContent().length() > 4000) {
                    it.setContent(it.getContent().substring(0, 4000));
                }
                it.setLogTime(parseTime(it.getTime()));
                valid.add(it);

                Matcher m = EVENT_RE.matcher(it.getContent() == null ? "" : it.getContent());
                if (m.find()) {
                    GbWeighEvent ev = new GbWeighEvent();
                    ev.setSource(it.getNo());
                    ev.setFp(it.getFp());
                    ev.setOrderNo(m.group(1).trim());
                    ev.setVehicleNo(m.group(2).trim());
                    ev.setEventTime(it.getLogTime() != null ? it.getLogTime()
                            : LocalDateTime.of(1970, 1, 1, 0, 0));
                    events.add(ev);
                }
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("received", lines == null ? 0 : lines.size());
        r.put("saved", 0);
        r.put("events", 0);
        if (!valid.isEmpty()) r.put("saved", gbDao.insertLines(valid));
        if (!events.isEmpty()) r.put("events", gbDao.insertEvents(events));
        return r;
    }

    public Map<String, Object> listLines(String source, LocalDateTime beginTime, LocalDateTime endTime,
                                         String keyword, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 500);
        int safePage = Math.max(page, 1);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", gbDao.countLines(source, beginTime, endTime, keyword));
        r.put("page", safePage);
        r.put("size", safeSize);
        r.put("list", gbDao.selectLines(source, beginTime, endTime, keyword,
                (safePage - 1) * safeSize, safeSize));
        return r;
    }

    public Map<String, Object> listEvents(String source, String orderNo, String vehicleNo,
                                          LocalDateTime beginTime, LocalDateTime endTime, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 500);
        int safePage = Math.max(page, 1);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", gbDao.countEvents(source, orderNo, vehicleNo, beginTime, endTime));
        r.put("page", safePage);
        r.put("size", safeSize);
        r.put("list", gbDao.selectEvents(source, orderNo, vehicleNo, beginTime, endTime,
                (safePage - 1) * safeSize, safeSize));
        return r;
    }

    public List<GbTimeline> timeline(String orderNo) {
        return gbDao.selectTimeline(orderNo);
    }

    /** 把前端/调用方的 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss 宽松解析成 LocalDateTime；解析失败返回 null */
    public static LocalDateTime parseRange(String s, boolean endOfDay) {
        if (s == null || s.isBlank()) return null;
        s = s.trim();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignore) {
        }
        try {
            LocalDate d = LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return endOfDay ? d.plusDays(1).atStartOfDay() : d.atStartOfDay();
        } catch (Exception ignore) {
        }
        return null;
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim(), TIME_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
