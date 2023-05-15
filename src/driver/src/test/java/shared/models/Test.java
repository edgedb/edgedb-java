package shared.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import shared.json.ResultNodeDeserializer;

import java.util.List;

public class Test {
    public String name;
    public List<QueryExecutionArguments> queries;

    @JsonDeserialize(using = ResultNodeDeserializer.class)
    public Object result;
}
