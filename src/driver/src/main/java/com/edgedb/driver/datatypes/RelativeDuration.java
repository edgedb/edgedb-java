package com.edgedb.driver.datatypes;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public final class RelativeDuration {
    private final long microseconds;
    private final int days;
    private final int months;

    public RelativeDuration(int months, int days, long microseconds) {
        this.microseconds = microseconds;
        this.days = days;
        this.months = months;
    }

    public RelativeDuration(Period period) {
        this((int)period.toTotalMonths(), period.getDays(), 0);
    }

    public RelativeDuration(Duration duration) {
        this(0, 0, Math.round(duration.toNanos() / 1000d));
    }

    public int getMonths() {
        return this.months;
    }

    public int getDays() {
        return this.days;
    }

    public long getMicroseconds() {
        return this.microseconds;
    }

    public Period toPeriod() {
        if(months == 0 && days == 0 && microseconds == 0) {
            return Period.ZERO;
        }

        return Period.of(0, months, days + (int)Duration.of(microseconds, ChronoUnit.MICROS).toDays());
    }

    public Duration toDuration() {
        if(months == 0 && days == 0 && microseconds == 0) {
            return Duration.ZERO;
        }

        var duration = microseconds == 0 ? Duration.ZERO : Duration.of(microseconds, ChronoUnit.MICROS);

        if(days != 0) {
            duration = duration.plus(days, ChronoUnit.DAYS);
        }

        if(months != 0) {
            duration = duration.plus(months, ChronoUnit.MONTHS);
        }

        return duration;
    }
}
