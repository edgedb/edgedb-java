package shared.json;

import com.edgedb.driver.datatypes.RelativeDuration;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;

import java.io.IOException;
import java.time.temporal.ChronoUnit;

public class CRelativeDurationDeserializer extends CFormatBase<RelativeDuration> {
    public CRelativeDurationDeserializer() {
        super(RelativeDuration.class);
    }

    @Override
    public RelativeDuration deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        return new RelativeDuration(super.fromString(parser.getCodec().readValue(parser, String.class), ChronoUnit.SECONDS));
    }
}
