package shared.json;

import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ISOFormatBase<T> extends StdDeserializer<T> {
    protected ISOFormatBase(Class<?> vc) {
        super(vc);
    }

    private static final Map<Integer, ChronoUnit> ordinalMap = Arrays.stream(ChronoUnit.values()).collect(Collectors.toMap(Enum::ordinal, v -> v));

    // format [d'.']hh':'mm':'ss['.'fffffff]
    protected Duration fromString(String str, ChronoUnit leastSigFig) {
        var spl = str.split(":");

        var isNeg = str.startsWith("-");

        Duration duration = Duration.ZERO;

        var unit = leastSigFig;

        for(int i = spl.length - 1; i >= 0; i--) {
            var component = spl[i];

            if(component.startsWith("-"))
                component = component.substring(1);

            if(i == 0 && spl.length == 3) {
                var dh = component.split("\\.");

                duration = duration.plus(Long.parseLong(dh[0]), ChronoUnit.DAYS);

                if(!dh[1].equals("00")) {
                    duration = duration.plus(Long.parseLong(dh[1]), ChronoUnit.HOURS);
                }

                continue;
            }

            duration = addFractionalComponent(
                    duration,
                    unit,
                    new BigDecimal(component)
            );

            unit = ordinalMap.get(unit.ordinal() + 1);
        }

        if(isNeg)
            duration = duration.negated();

        return duration;
    }

    protected Duration addFractionalComponent(Duration dur, ChronoUnit unit, BigDecimal d) {
        for(int i = unit.ordinal(); i >= ChronoUnit.MICROS.ordinal(); i--) {
            var dist = unit.ordinal() - i;
            var exp = Math.pow(1000, dist);

            var iterationOrdinal = ordinalMap.get(i);

            // grab the non-fractional part if distance (ordinal - i) == 0; otherwise, multiply 'd' by
            // 1000 to the power of ordinal distance, then grab the lowest 3 digit integer
            // f(x) = (x % (ordinal - i) * 1000^(ordinal - i)) % 1000
            var f = (
                    (
                            dist == 0
                                    ? d
                                    : d.remainder(BigDecimal.valueOf(dist))
                    )
                            .multiply(BigDecimal.valueOf(exp))
            )
                    .remainder(BigDecimal.valueOf(1000))
                    .longValue();

            if(f == 0) {
                return dur;
            }

            // convert the fractional component 'f' to the target unit, since time units can
            // be non-base 10, ex 1 hour is 60 minutes, divide the current ordinal duration by the last
            // ordinal duration, then multiply it by the resulting fractional.
            // the 'durationFraction' can be 1 if the current unit is the supplied 'unit' parameter,
            // in this case, this second step has the same effect as:
            // 1 * (f / 1), which just returns 'f'.
            // in cases where the conversion is base 10, this second step also returns out 'f'
            // because of our 'exp' exponent.
            var durationFraction = iterationOrdinal.getDuration().dividedBy(dist == 0 ? unit.getDuration() : ordinalMap.get(i - 1).getDuration());
            var g = durationFraction * (f / exp);

            dur = dur.plus((long)Math.floor(g), iterationOrdinal);
        }

        return dur;
    }
}
