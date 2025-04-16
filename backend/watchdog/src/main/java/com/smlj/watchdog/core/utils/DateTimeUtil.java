package com.smlj.watchdog.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

@Slf4j
public final class DateTimeUtil {
    // 时间戳转换为当天0点的时间戳
    public static long convertToMidnightTimestamp(long timestamp) {
        var calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));

        // 设置时间为给定的时间戳
        calendar.setTimeInMillis(timestamp * 1000);

        // calendar.get(Calendar.MONTH)获取的月份比实际少1
        // log.info("{}, {}, {}", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));

        // 设置时间为当天0点
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 返回当天0点的时间戳
        return calendar.getTimeInMillis() / 1000;
    }

    public static int getZoneOffset() {
        ZoneId currentZone = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(currentZone);
        return now.getOffset().getTotalSeconds();
    }

    public static long plusDay(long timestamp, int days) {
        return timestamp + (long) days * 24 * 60 * 60;
    }

    public static long nowTimestamp(boolean useSeconds) {
        if(useSeconds) {
            return System.currentTimeMillis() / 1000;
        }
        return System.currentTimeMillis();
    }

    public static int getYear() {
        var calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        return calendar.get(Calendar.YEAR);
    }
}
