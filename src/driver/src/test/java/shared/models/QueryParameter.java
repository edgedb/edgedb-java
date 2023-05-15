package shared.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import shared.json.ResultNodeDeserializer;

public class QueryParameter {
    public String name;
    public String edgedbTypename;

    @JsonDeserialize(using = ResultNodeDeserializer.class)
    public ResultNode value;
}
