package com.edgedb.driver.util;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class JsonUtils {

    public static class AsStringDeserializer extends StdDeserializer<String> {
        public AsStringDeserializer() {
            this(null);
        }

        public AsStringDeserializer(final Class<?> vc) {
            super(vc);
        }

        @Override
        public String deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
        ) throws IOException {
            JsonToken token = jsonParser.currentToken();

            if (token == JsonToken.VALUE_NUMBER_INT
                || token == JsonToken.VALUE_NUMBER_FLOAT
                || token == JsonToken.VALUE_STRING
            ) {
                return jsonParser.getText();
            }
            else {
                throw new IOException(String.format(
                    "Invalid %s token: \"%s\", expected NUMBER or STRING.",
                    token,
                    jsonParser.getText()
                ));
            }
        }
    }
}
