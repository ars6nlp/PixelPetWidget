package com.example.pixelpetwidget;

import java.util.Calendar;
import java.util.TimeZone;

/** Local calendar days and times of day, without java.time (minSdk 24). */
final class DayClock {

    static final long HOUR_MS = 60L * 60 * 1000;
    static final long DAY_MS = 24 * HOUR_MS;

    private DayClock() {}

    /** Number of the local calendar day; consecutive days differ by exactly 1. */
    static long dayNumber(long millis, TimeZone zone) {
        return Math.floorDiv(millis + zone.getOffset(millis), DAY_MS);
    }

    static long today() {
        return dayNumber(System.currentTimeMillis(), TimeZone.getDefault());
    }

    static int hourOfDay(long millis, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTimeInMillis(millis);
        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /** Month 1..12 and day of month of the given moment. */
    static int[] monthAndDay(long millis, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTimeInMillis(millis);
        return new int[] {calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH)};
    }

    /** The next moment strictly after {@code millis} when the local clock shows hour:00. */
    static long nextTimeAt(long millis, int hour, TimeZone zone) {
        Calendar calendar = Calendar.getInstance(zone);
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (calendar.getTimeInMillis() <= millis) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return calendar.getTimeInMillis();
    }
}
