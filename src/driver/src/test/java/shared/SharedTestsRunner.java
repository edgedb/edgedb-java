package shared;

import com.edgedb.driver.EdgeDBClient;
import com.edgedb.driver.EdgeDBClientConfig;
import com.edgedb.driver.binary.builders.ObjectBuilder;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.packets.receivable.Data;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import com.edgedb.driver.binary.packets.shared.IOFormat;
import com.edgedb.driver.clients.BaseEdgeDBClient;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.datatypes.RelativeDuration;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import shared.json.CDurationDeserializer;
import shared.json.CPeriodDeserializer;
import shared.json.CRelativeDurationDeserializer;
import shared.models.QueryExecutionArguments;
import shared.models.Test;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class SharedTestsRunner {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .registerModule(new JavaTimeModule())
            .registerModule(new SimpleModule(){{
                addDeserializer(RelativeDuration.class, new CRelativeDurationDeserializer());
                addDeserializer(Period.class, new CPeriodDeserializer());
                addDeserializer(Duration.class, new CDurationDeserializer());
            }});

    private static final EdgeDBClient CLIENT;

    static {
        try {
            CLIENT = new EdgeDBClient(EdgeDBClientConfig.builder()
                    .withMessageTimeout(1, TimeUnit.HOURS)
                    .withExplicitObjectIds(true)
                    .build()
            );
        } catch (IOException | EdgeDBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void Run(Path path) {
        // we're in './src/driver', so remove 2 levels of the dir
        var absPath = Path.of(System.getProperty("user.dir")).getParent().getParent().resolve(path);

        // since result type combinations are exponential, we set a hard coded limit as to not run out of memory.
        final int maxResultTypes = 15;

        Test testDefinition;

        try {
            var content = Files.readString(absPath);
            testDefinition = MAPPER.readValue(content, Test.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        EdgeDBBinaryClient clientHandle;
        try {
            clientHandle = (EdgeDBBinaryClient)getClientHandle().toCompletableFuture().get();
        } catch (InterruptedException | ExecutionException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<BinaryResult> results = new ArrayList<>();

        for(int i = 0; i != testDefinition.queries.size(); i++){
            var query = testDefinition.queries.get(i);

            clientHandle.withSession(query.session);

            BinaryResult result;

            try {
                result = executeQuery(clientHandle, query);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            if(!result.data.isEmpty()) {
                results.add(result);
            }
        }

        var resultTypes = ResultTypeBuilder.createResultTypes(testDefinition.result, maxResultTypes, !absPath.toString().contains("deep_nesting"));

        for(int i = 0; i != results.size(); i++) {
            var executionResult = results.get(i);

            for (int j = 0; j != resultTypes.size(); j++) {
                var resultType = resultTypes.get(j);

                Object value = null;
                try {
                    // store reader index before build
                    var readerIndexes = executionResult.data.stream()
                            .mapToInt(v -> v.payloadBuffer.readerIndex())
                            .toArray();

                    value = buildResult(clientHandle, resultType, executionResult);

                    // restore reader indexes post-build
                    IntStream.range(0, executionResult.data.size())
                            .mapToObj(k -> Map.entry(executionResult.data.get(k), readerIndexes[k]))
                            .forEach(v -> v.getKey().payloadBuffer.readerIndex(v.getValue()));
                } catch (EdgeDBException | OperationNotSupportedException e) {
                    throw new RuntimeException(e);
                }

                ResultAsserter.assertResult(testDefinition.result, value);
            }
        }

        try {
            clientHandle.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object buildResult(EdgeDBBinaryClient client, Class<?> type, BinaryResult result) throws EdgeDBException, OperationNotSupportedException {
        switch (result.cardinality) {
            case MANY:
                var arr = (Object[])Array.newInstance(type, result.data.size());

                for(int i = 0; i != result.data.size(); i++) {
                    var value = ObjectBuilder.buildResult(client, result.deserializer, result.data.get(i), type);
                    arr[i] = value;
                }

                return arr;
            case AT_MOST_ONE:
                if(result.data.size() == 0)
                    return null;

                return ObjectBuilder.buildResult(client, result.deserializer, result.data.get(0), type);
            case ONE:
                if(result.data.size() != 1)
                    throw new IllegalArgumentException("Missing data for result");

                return ObjectBuilder.buildResult(client, result.deserializer, result.data.get(0), type);
            default:
                if(result.data.size() > 0)
                    throw new IllegalArgumentException("Unknown cardinality path for remaining data");

                return null;
        }
    }

    private static Method getClientMethod;
    private static CompletionStage<BaseEdgeDBClient> getClientHandle() throws InvocationTargetException, IllegalAccessException {
        if(getClientMethod == null) {
            try {
                getClientMethod = EdgeDBClient.class.getDeclaredMethod("getClient");
                getClientMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }

        //noinspection unchecked
        return (CompletionStage<BaseEdgeDBClient>) getClientMethod.invoke(CLIENT);
    }

    private static final class BinaryResult {
        public final Codec<?> deserializer;
        public final List<Data> data;
        public final Cardinality cardinality;

        private BinaryResult(Codec<?> deserializer, List<Data> data, Cardinality cardinality) {
            this.deserializer = deserializer;
            this.data = data;
            this.cardinality = cardinality;
        }
    }

    private static BinaryResult executeQuery(EdgeDBBinaryClient client, QueryExecutionArguments query) throws ExecutionException, InterruptedException {
        Map<String, Object> args = null;

        if(query.arguments != null && query.arguments.size() > 0) {
            args = new HashMap<>();

            for (var argument : query.arguments) {
                args.put(argument.name, ResultTypeBuilder.toObject(argument.value));
            }
        }

        Cardinality queryCard;

        switch (query.getCardinality()) {
            case ONE:
            case AT_MOST_ONE:
                queryCard = Cardinality.AT_MOST_ONE;
                break;
            default:
                queryCard = Cardinality.MANY;
                break;
        }

        var executionArgs = client.new ExecutionArguments(
                query.value,
                args,
                queryCard,
                query.getCapabilities(),
                query.getCardinality() == Cardinality.NO_RESULT ? IOFormat.NONE : IOFormat.BINARY,
                false,
                false
        );

        client.executeQuery(executionArgs).toCompletableFuture().get();

        return new BinaryResult(executionArgs.codecs.outputCodec, executionArgs.data, executionArgs.cardinality);
    }

}
