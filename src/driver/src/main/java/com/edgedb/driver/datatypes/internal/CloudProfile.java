package com.edgedb.driver.datatypes.internal;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudProfile {
    @JsonProperty("secret_key")
    public String secretKey;
}
