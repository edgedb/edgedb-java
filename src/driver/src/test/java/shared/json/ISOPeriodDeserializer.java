package shared.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public class ISOPeriodDeserializer extends ISOFormatBase<Period> {
    public ISOPeriodDeserializer() {
        super(Period.class);
    }

    @Override
    public Period deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        return Period.ofDays((int)super.fromString(parser.getCodec().readValue(parser, String.class), ChronoUnit.SECONDS).toDays());
    }
}
