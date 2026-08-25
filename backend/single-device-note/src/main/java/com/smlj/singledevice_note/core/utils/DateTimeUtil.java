package com.smlj.singledevice_note.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;

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

    /**
     * 在 begin 和 end 之间随机取一个时间（包含边界）
     *
     * @param begin 开始时间
     * @param end   结束时间
     * @return 随机时间
     */
    public static Date randomDateBetween(Date begin, Date end, boolean includeBegin, boolean includeEnd) {
        if (begin == null || end == null) {
            throw new IllegalArgumentException("begin 和 end 不能为空");
        }
        if (begin.after(end)) {
            throw new IllegalArgumentException("begin 不能晚于 end");
        }

        long beginMillis = begin.getTime();
        long endMillis = end.getTime();

        // 如果时间相同，直接返回
        if (beginMillis == endMillis) {
            return new Date(beginMillis);
        }

        // 生成随机毫秒数
        long randomMillis = ThreadLocalRandom.current().nextLong(includeBegin ? beginMillis : beginMillis + 1, includeEnd ? endMillis + 1 : endMillis);
        return new Date(randomMillis);
    }
}
