package shared.json;

import com.edgedb.driver.state.Config;
import com.edgedb.driver.state.Session;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.assertj.core.util.Streams;
import shared.ResultTypeBuilder;
import shared.models.ResultNode;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

public class SessionDeserializer extends StdDeserializer<Session> {
    protected SessionDeserializer() {
        super(Session.class);
    }

    @Override
    public Session deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
        var node = parser.getCodec().readTree(parser);

        if(!(node instanceof ObjectNode)) {
            throw new RuntimeException("Expected object node");
        }

        var module = ((TextNode)node.get("module")).asText();
        var aliases = Streams.stream(((ObjectNode)node.get("aliases")).fields())
                .collect(Collectors.toMap(Map.Entry::getKey, v -> ((TextNode)v).asText()));
        var config = readConfig(node.get("config"), context);
        var globals = Streams.stream(((ObjectNode)node.get("globals")).fields())
                .collect(Collectors.toMap(Map.Entry::getKey, v -> {
                    try {
                        return ResultTypeBuilder.toObject((ResultNode) ResultNodeDeserializer.INSTANCE.deserialize(v.getValue().traverse(parser.getCodec()), context));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));

        return new Session(
                module,
                aliases,
                config,
                globals
        );

    }

    private Config readConfig(TreeNode node, DeserializationContext context) throws IOException {
        return Config.builder()
                .applyAccessPolicies(
                        node.path("apply_access_policies").isMissingNode()
                                ? null
                                : ((BooleanNode)node.get("apply_access_policies")).asBoolean()
                )
                .withIdleTransactionTimeout(
                        node.path("idle_transaction_timeout").isMissingNode()
                                ? null
                                : context.readTreeAsValue((JsonNode) node.get("idle_transaction_timeout"), Duration.class)
                )
                .allowBareDDL(
                        node.path("ddl_policy").isMissingNode()
                                ? null
                                : ((IntNode)node.get("ddl_policy")).asInt() == 0
                )
                .allowDMLInFunctions(node.path("allow_dml_in_functions").isMissingNode()
                        ? null
                        : ((BooleanNode)node.get("allow_dml_in_functions")).asBoolean()
                )
                .withQueryExecutionTimeout(node.path("query_execution_timeout").isMissingNode()
                        ? null
                        : context.readTreeAsValue((JsonNode) node.get("query_execution_timeout"), Duration.class)
                )
                .build();
    }
}
