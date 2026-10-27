package com.smlj.singledevice_note.gb.controller;

import com.smlj.singledevice_note.core.o.to.Result;
import com.smlj.singledevice_note.gb.o.GbLogLine;
import com.smlj.singledevice_note.gb.o.GbTimeline;
import com.smlj.singledevice_note.gb.service.GbService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 过磅日志监控器（gb-watcher）上报接口 + 日志/过磅事件查询。
 *
 * ⚠️ 路径刻意挂在 /api/gb 下：TokenInterceptor / SignInterceptor 只拦 /cghtz/**，
 * 监控器（磅房电脑上的 exe）不带 JWT/签名，这里保持免认证直写。
 * 若日后要收紧，可改为固定 token 请求头校验，不要把该 controller 挪进 /cghtz/**。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gb")
@Tag(name = "CGbLog", description = "过磅日志上报与查询（gb_record 库）")
public class CGbLog {
    private final GbService gbService;

    /** 监控器上报入口：POST {server}/api/gb/log，请求体为日志行数组 */
    @PostMapping("/log")
    public Result<Map<String, Object>> log(@RequestBody List<GbLogLine> lines) {
        try {
            return Result.success(gbService.receive(lines));
        } catch (Exception e) {
            log.error("过磅日志上报落库失败", e);
            return Result.fail("过磅日志落库失败：" + e.getMessage());
        }
    }

    /** 原始日志行分页查询 */
    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String source,
                                            @RequestParam(required = false) String begin,
                                            @RequestParam(required = false) String end,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "100") int size) {
        try {
            LocalDateTime beginTime = GbService.parseRange(begin, false);
            LocalDateTime endTime = GbService.parseRange(end, true);
            return Result.success(gbService.listLines(source, beginTime, endTime, keyword, page, size));
        } catch (Exception e) {
            log.error("过磅日志查询失败", e);
            return Result.fail("过磅日志查询失败：" + e.getMessage());
        }
    }

    /** 过磅事件（单号+车牌）分页查询 */
    @GetMapping("/events")
    public Result<Map<String, Object>> events(@RequestParam(required = false) String source,
                                              @RequestParam(required = false) String orderNo,
                                              @RequestParam(required = false) String vehicleNo,
                                              @RequestParam(required = false) String begin,
                                              @RequestParam(required = false) String end,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "100") int size) {
        try {
            LocalDateTime beginTime = GbService.parseRange(begin, false);
            LocalDateTime endTime = GbService.parseRange(end, true);
            return Result.success(gbService.listEvents(source, orderNo, vehicleNo,
                    beginTime, endTime, page, size));
        } catch (Exception e) {
            log.error("过磅事件查询失败", e);
            return Result.fail("过磅事件查询失败：" + e.getMessage());
        }
    }

    /** 单号聚合视图：进/出磅时间（首次=进场，最后一次=出场） */
    @GetMapping("/timeline")
    public Result<List<GbTimeline>> timeline(@RequestParam String orderNo) {
        try {
            if (orderNo == null || orderNo.isBlank()) return Result.fail("单号不能为空");
            return Result.success(gbService.timeline(orderNo.trim()));
        } catch (Exception e) {
            log.error("过磅时间线查询失败 orderNo={}", orderNo, e);
            return Result.fail("过磅时间线查询失败：" + e.getMessage());
        }
    }
}
