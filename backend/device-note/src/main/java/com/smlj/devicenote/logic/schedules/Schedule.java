package com.smlj.train.logic.schedules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

// 1、定时从进行数据库数据同步 _syncUsers, _syncOrgs
@Slf4j
@EnableScheduling
public class Schedule {
    @Scheduled(fixedRate = 60000/*1000 * 3600 * 24 * 10*/)
    private void x() {
        log.info("-- 定时任务 --");
    }
}
