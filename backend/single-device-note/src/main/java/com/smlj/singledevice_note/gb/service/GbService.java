package com.smlj.singledevice_note.gb.service;

import com.smlj.singledevice_note.gb.dao.GbDao;
import com.smlj.singledevice_note.gb.o.GbCombine;
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
    /** 车牌提取规则 1：...单号[]:车牌号[陕KM8567]有其它未完成记录 */
    private static final Pattern PLATE_RE = Pattern.compile("车牌号\\[([^\\]]+)\\]");
    /** 车牌提取规则 2：1#[宁AD3107]无效：... */
    private static final Pattern PLATE_INVALID_RE = Pattern.compile("\\d+#\\[([^\\]]+)\\]无效");
    /** 单号提取规则：...单号[WH2024110900069]:...（空单号不算） */
    private static final Pattern ORDER_RE = Pattern.compile("单号\\[([^\\]]+)\\]");
    /** 监控器上报的时间格式：月/日/小时不补零 */
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss");
    /**
     * 关键异常识别规则（顺序即优先级，一行只记首个命中的分类）：
     * 关键字 -> 异常分类名（分类名与 gb_combine.abnormal_type / 前端标签一致）
     */
    private static final List<Map.Entry<String, String>> ABNORMAL_RULES = List.of(
            Map.entry("超出载重范围", "超出载重范围"),
            Map.entry("PRIMARY KEY", "违反PRIMARY KEY约束"),
            Map.entry("未完成记录", "有其它未完成记录"),
            Map.entry("身份证", "未维护身份证号"),
            Map.entry("不可重复称", "不可重复称[皮重]"),
            Map.entry("分钟内不可重复过一次磅", "分钟内不可重复过磅"),
            Map.entry("重量不可小于", "重量不可小于0"));
    /** 组合信息正文上限，防止异常长行撑表 */
    private static final int COMBINE_CONTENT_MAX = 1000;
    /** 数据保留天数：超过即被懒清理删除（gb_log 按 log_time / gb_weigh_event 按 event_time） */
    private static final int RETENTION_DAYS = 10;
    /** 上次保留期清理时间（懒触发：每小时最多跑一次） */
    private volatile LocalDateTime lastRetention = LocalDateTime.MIN;

    /**
     * 接收监控器批量上报的日志行：原始行全量落库（fp 幂等去重），
     * 能解析出单号+车牌的行额外写一条过磅事件，
     * 命中 7 类关键异常的行额外写一条组合记录（车牌/单号 + 异常分类 + 原文 -> gb_combine）。
     * ⚠️ 不加 @Transactional：写库走主数据源 main（gb_record 模式），保持与查询相同的轻量语义。
     */
    public Map<String, Object> receive(List<GbLogLine> lines) {
        List<GbLogLine> valid = new ArrayList<>();
        List<GbWeighEvent> events = new ArrayList<>();
        List<GbCombine> combines = new ArrayList<>();
        if (lines != null) {
            for (GbLogLine it : lines) {
                if (it == null || it.getNo() == null || it.getFp() == null || it.getFp().isBlank()) continue;
                // 限制极端长行，避免异常内容撑爆
                if (it.getContent() != null && it.getContent().length() > 4000) {
                    it.setContent(it.getContent().substring(0, 4000));
                }
                it.setLogTime(parseTime(it.getTime()));
                valid.add(it);

                String content = it.getContent() == null ? "" : it.getContent();
                Matcher m = EVENT_RE.matcher(content);
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

                GbCombine cb = matchAbnormal(it, content);
                if (cb != null) combines.add(cb);
            }
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("received", lines == null ? 0 : lines.size());
        r.put("saved", 0);
        r.put("events", 0);
        r.put("combines", 0);
        if (!valid.isEmpty()) r.put("saved", gbDao.insertLines(valid));
        if (!events.isEmpty()) r.put("events", gbDao.insertEvents(events));
        if (!combines.isEmpty()) r.put("combines", gbDao.insertCombines(combines));
        retention();
        return r;
    }

    /**
     * 关键异常识别：首个命中规则生效；提取车牌（无车牌取单号），组装 gb_combine 行。
     * ⚠️ 只认 [A] 自动判定行：身份证异常的 [M] 行是「过磅车辆判断信息」JSON 回显，
     * 与 [A] 行成对出现且无车牌，放进来会产生无车牌的重复组合记录。
     */
    private GbCombine matchAbnormal(GbLogLine line, String content) {
        if ("M".equals(line.getType())) return null;
        for (Map.Entry<String, String> rule : ABNORMAL_RULES) {
            if (!content.contains(rule.getKey())) continue;
            String plate = null;
            Matcher p = PLATE_RE.matcher(content);
            if (!p.find()) {
                p = PLATE_INVALID_RE.matcher(content);
                plate = p.find() ? p.group(1).trim() : null;
            } else {
                plate = p.group(1).trim();
            }
            String orderNo = null;
            Matcher o = ORDER_RE.matcher(content);
            if (o.find()) orderNo = o.group(1).trim();

            GbCombine cb = new GbCombine();
            cb.setSource(line.getNo());
            cb.setFp(line.getFp());
            cb.setVehicleNo(plate);
            cb.setOrderNo(orderNo);
            cb.setAbnormalType(rule.getValue());
            cb.setContent(content.length() > COMBINE_CONTENT_MAX
                    ? content.substring(0, COMBINE_CONTENT_MAX) : content);
            cb.setLogTime(line.getLogTime());
            return cb;
        }
        return null;
    }

    /** 保留期懒清理：随上报触发，每小时最多一次，删除超过保留期的数据 */
    private void retention() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(lastRetention.plusHours(1))) return;
        lastRetention = now;
        try {
            LocalDateTime before = now.minusDays(RETENTION_DAYS);
            int l = gbDao.deleteExpiredLines(before);
            int e = gbDao.deleteExpiredEvents(before);
            int c = gbDao.deleteExpiredCombines(before);
            if (l > 0 || e > 0 || c > 0) {
                log.info("过磅日志保留期清理(>{}天): 删除原始行 {} 条, 事件 {} 条, 组合记录 {} 条",
                        RETENTION_DAYS, l, e, c);
            }
        } catch (Exception ex) {
            log.warn("过磅日志保留期清理失败（不影响上报）", ex);
        }
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
