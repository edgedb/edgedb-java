package com.edgedb.datatypes;

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
        return Period.of(0, months, days);
    }

    public Duration toDuration() {
        // TODO: check overflow
        long micros = this.microseconds + (this.days * 86400000000L) + (this.months * 2629800000000L);
        return Duration.of(micros, ChronoUnit.MICROS);
    }
}
