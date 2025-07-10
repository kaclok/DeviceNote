package com.smlj.singledevice_note.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@Slf4j
public final class DateTimeUtil {
    // 时间戳转换为当天0点的时间戳
    public static long convertToMidnightTimestamp(long timestamp) {
        var dt = convertTo(timestamp * 1000);
        dt = convertTo(dt, 0, 0, 0, 0);
        return convertTo(dt);
    }

    // 转换成from所在当天的某个时间
    public static Date convertTo(Date from, int hour, int minute, int second, int millisecond) {
        var calendar = convertToCalendar(from);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, second);
        calendar.set(Calendar.MILLISECOND, millisecond);
        return calendar.getTime();
    }

    public static Calendar convertToCalendar(Date from) {
        var calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTime(from);
        return calendar;
    }

    public static Calendar convertToCalendar(long millis) {
        var calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
        calendar.setTimeInMillis(millis);
        return calendar;
    }

    public static long convertTo(Date from) {
        var calendar = convertToCalendar(from);
        return calendar.getTimeInMillis();
    }

    public static Date convertTo(long millis) {
        var calendar = convertToCalendar(millis);
        return calendar.getTime();
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
        if (useSeconds) {
            return System.currentTimeMillis() / 1000;
        }
        return System.currentTimeMillis();
    }
}
