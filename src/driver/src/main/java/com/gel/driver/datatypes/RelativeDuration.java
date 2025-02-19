package com.gel.driver.datatypes;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;

/**
 * A class representing the {@code cal::relative_duration} type.
 */
public final class RelativeDuration implements Comparable<RelativeDuration> {
    private final long microseconds;
    private final int days;
    private final int months;

    /**
     * Constructs a new {@linkplain RelativeDuration}.
     * @param months The months component.
     * @param days The days component.
     * @param microseconds The microseconds component.
     */
    public RelativeDuration(int months, int days, long microseconds) {
        this.microseconds = microseconds;
        this.days = days;
        this.months = months;
    }

    /**
     * Constructs a new {@linkplain RelativeDuration}.
     * @param period The period which to convert to a {@linkplain RelativeDuration}.
     */
    public RelativeDuration(@NotNull Period period) {
        this((int)period.toTotalMonths(), period.getDays(), 0);
    }

    /**
     * Constructs a new {@linkplain RelativeDuration}.
     * @param duration The duration which to convert to a {@linkplain RelativeDuration}.
     */
    public RelativeDuration(@NotNull Duration duration) {
        this(0, 0, Math.round(duration.toNanos() / 1000d));
    }

    /**
     * Gets the months component of this relative duration.
     * @return The months component.
     */
    public int getMonths() {
        return this.months;
    }

    /**
     * Gets the days component of this relative duration.
     * @return The days component.
     */
    public int getDays() {
        return this.days;
    }

    /**
     * Gets the microseconds component of this relative duration.
     * @return The microseconds component.
     */
    public long getMicroseconds() {
        return this.microseconds;
    }

    /**
     * Converts this relative duration to a {@linkplain Period}.
     * @return A period equivalent to this relative duration.
     * @see Period
     */
    public Period toPeriod() {
        if(months == 0 && days == 0 && microseconds == 0) {
            return Period.ZERO;
        }

        return Period.of(0, months, days + (int)Duration.of(microseconds, ChronoUnit.MICROS).toDays());
    }

    /**
     * Converts this relative duration to a {@linkplain Duration}.
     * @return A duration equivalent to this relative duration.
     * @see Duration
     */
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

    @Override
    public int compareTo(@NotNull RelativeDuration o) {
        return toDuration().compareTo(o.toDuration());
    }
}
