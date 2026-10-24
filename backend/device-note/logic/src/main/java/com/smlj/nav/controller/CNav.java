package com.smlj.nav.controller;

import com.smlj.nav.o.NavConfig;
import com.smlj.nav.service.NavService;
import com.smlj.o.to.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/nav")
@Tag(name = "CNav", description = "厂区寻路配置")
public class CNav {
    private final NavService navService;

    @GetMapping("/config")
    public Result<NavConfig> get(@RequestParam(value = "plantId", defaultValue = "default") String plantId) {
        try {
            NavConfig config = navService.getConfig(plantId);
            if (config == null) return Result.fail("厂区配置不存在：" + plantId);
            return Result.success(config);
        } catch (Exception e) {
            log.error("读取厂区寻路配置失败 plantId={}", plantId, e);
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/config")
    public Result<Void> save(@RequestBody NavConfig config) {
        try {
            navService.saveConfig(config);
            return Result.success();
        } catch (Exception e) {
            log.error("保存厂区寻路配置失败", e);
            return Result.fail(e.getMessage());
        }
    }
}
