package com.jthx.x.cms.watchdog.controller;

import com.jthx.x.cms.watchdog.Detector.Checker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/x/watchdog")
public class CWatchDog {
    @Autowired
    private Checker checker;

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
    @GetMapping("/reloadDB")
    public void reloadDB() {
        if (checker != null) {
            checker.reloadDB();
        }
    }
}
