package com.jthx.x.cms.watchdog.service;

import com.jthx.x.cms.watchdog.pojo.MetricTask;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class MetricScheduler {
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);

    public void registerMetric(MetricTask metricTask) {
        executor.scheduleAtFixedRate(
                metricTask.getTask(),
                0,
                metricTask.getPeriod(),
                TimeUnit.SECONDS
        );
    }
}
