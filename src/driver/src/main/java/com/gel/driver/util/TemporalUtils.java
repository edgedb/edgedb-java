package com.gel.driver.util;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.function.Function;

public final class TemporalUtils {
    public static final ZonedDateTime EDGEDB_EPOC = ZonedDateTime.of(
            2000,
            1,
            1,
            0,
            0,
            0,
            0,
            ZoneId.of("UTC"));

    public static final LocalDateTime EDGEDB_EPOC_LOCAL = LocalDateTime.of(
            2000,
            1,
            1,
            0,
            0,
            0,
            0
    );

    // date_duration -> Period
    // datetime -> OffsetDateTime|ZonedDateTime
    // duration -> Duration
    // local_date -> LocalDate
    // local_datetime -> LocalDateTime
    // local_time -> LocalTime
    // relative_duration -> Duration|Period

    public static long toMicrosecondsSinceEpoc(Temporal temporal) {
        return ChronoUnit.MICROS.between(EDGEDB_EPOC, temporal);
    }

    public static <T extends Temporal> T fromMicrosecondsSinceEpoc(long micros, @NotNull Function<ZonedDateTime, T> mapper) {
        return mapper.apply(EDGEDB_EPOC.plus(micros, ChronoUnit.MICROS));
    }
}
