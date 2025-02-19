package shared.json;

import com.gel.driver.datatypes.Range;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.util.Streams;
import shared.ResultTypeBuilder;
import shared.models.CollectionResultNode;
import shared.models.ResultNode;
import shared.models.ResultNodeImpl;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class ResultNodeDeserializer extends StdDeserializer<Object> {
    public static final ResultNodeDeserializer INSTANCE = new ResultNodeDeserializer();
    private static final EnumSet<JsonToken> START_TOKENS = EnumSet.of(JsonToken.START_ARRAY, JsonToken.START_OBJECT);

    protected ResultNodeDeserializer() {
        super(ResultNode.class);
    }

    @Override
    public Object deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        while(parser.getCurrentToken() == null || !START_TOKENS.contains(parser.getCurrentToken())) {
            parser.nextToken();
        }

        if(parser.getCurrentToken() == JsonToken.START_OBJECT) {
            return readNode(parser.getCodec().readTree(parser), context);
        }

        var arr = (ArrayNode)parser.getCodec().readTree(parser);

        var resultArray = new ResultNode[arr.size()];

        for(int i = 0; i != resultArray.length; i++) {
            if(!(arr.get(i) instanceof ObjectNode))
                throw new RuntimeException("Inner node of root array is not object node");

            resultArray[i] = readNode(arr.get(i), context);
        }

        return resultArray;
    }

    private ResultNode readNode(TreeNode tree, DeserializationContext context) throws IOException {
        var nodeType = asText(tree.get("type"));

        switch (nodeType) {
            case "set":
            case "array":
                return new CollectionResultNode(
                        nodeType,
                        asText(tree.get("element_type")),
                        readNodeValue(tree.get("value"), context)
                );
            case "namedtuple":
            case "object":
                return new ResultNodeImpl(
                        nodeType,
                        Streams.stream(
                                ((ObjectNode)tree.get("value")).fields()
                        ).collect(
                                LinkedHashMap::new,
                                (m, v) -> {
                                    try {
                                        m.put(v.getKey(), readNodeValue(v.getValue(), context));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                },
                                HashMap::putAll
                        )
                );
            case "tuple":
                return new ResultNodeImpl(
                        nodeType,
                        readNodeValue(tree.get("value"), context)
                );
            case "range":
                var elementType = asText(tree.get("element_type"));

                Class<?> elementClass;

                switch (elementType) {
                    case "std::int32":
                        elementClass = Integer.class;
                        break;
                    case "std::int64":
                        elementClass = Long.class;
                        break;
                    case "std::float32":
                        elementClass = Float.class;
                        break;
                    case "std::float64":
                        elementClass = Double.class;
                        break;
                    case "std::decimal":
                        elementClass = BigDecimal.class;
                        break;
                    case "std::datetime":
                        elementClass = OffsetDateTime.class;
                        break;
                    case "cal::local_datetime":
                        elementClass = LocalDateTime.class;
                        break;
                    case "cal::local_date":
                        elementClass = LocalDate.class;
                        break;
                    default:
                        throw new RuntimeException("Unknown range element type " + elementType);
                }

                if(!(tree.get("value") instanceof ObjectNode))
                    throw new RuntimeException("Expected object node for range");

                var range = createRange(elementClass, (ObjectNode) tree.get("value"), context);

                return new ResultNodeImpl(
                        nodeType,
                        range
                );
            default:
                return new ResultNodeImpl(
                        nodeType,
                        context.readTreeAsValue((JsonNode) tree.get("value"), ResultTypeBuilder.getClassFromGelTypeName(nodeType))
                );
        }
    }

    private Object readNodeValue(TreeNode node, DeserializationContext context) throws IOException {
        if(node instanceof ArrayNode) {
            var arrayNode = (ArrayNode)node;

            var array = new ResultNode[arrayNode.size()];

            for(int i = 0; i != array.length; i++) {
                if(!(arrayNode.get(i) instanceof ObjectNode))
                    throw new RuntimeException("Expected object node, but found " + arrayNode.get(i).getClass().getName());

                array[i] = readNode(arrayNode.get(i), context);
            }

            return array;
        }
        else if (node instanceof ObjectNode) {
            return readNode(node, context);
        }

        throw new RuntimeException("Expected array or object node, but found " + node.getClass().getName());
    }

    private <T> Range<T> createRange(Class<T> elementType, ObjectNode rangeObject, DeserializationContext context) throws IOException {
        var lower = context.readTreeAsValue(rangeObject.get("lower"), elementType);
        var upper = context.readTreeAsValue(rangeObject.get("upper"), elementType);
        var incLower = rangeObject.get("inc_lower").asBoolean();
        var incUpper = rangeObject.get("inc_upper").asBoolean();

        return Range.create(elementType, lower, upper, incLower, incUpper);

    }

    private String asText(TreeNode node) {
        return ((TextNode)node).asText();
    }
}
