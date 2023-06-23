package shared.json;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class CDurationDeserializer extends CFormatBase<Duration> {
    public CDurationDeserializer() {
        super(Duration.class);
    }

    @Override
    public Duration deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return fromString(parser.getCodec().readValue(parser, String.class), ChronoUnit.SECONDS);
    }
}
