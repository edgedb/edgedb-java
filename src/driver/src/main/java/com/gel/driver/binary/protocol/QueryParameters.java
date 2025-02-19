package com.gel.driver.binary.protocol;

import com.gel.driver.Capabilities;
import com.gel.driver.binary.builders.CodecBuilder;
import com.gel.driver.binary.protocol.common.Cardinality;
import com.gel.driver.binary.protocol.common.IOFormat;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Map;

public final class QueryParameters {
    public final String query;
    public final @Nullable Map<String, @Nullable Object> arguments;
    public final EnumSet<Capabilities> capabilities;
    public final Cardinality cardinality;
    public final IOFormat format;
    public final boolean implicitTypeNames;

    public QueryParameters(
            String query,
            @Nullable Map<String, @Nullable Object> arguments,
            EnumSet<Capabilities> capabilities,
            Cardinality cardinality,
            IOFormat format,
            boolean implicitTypeNames
    ) {
        this.query = query;
        this.arguments = arguments;
        this.capabilities = capabilities;
        this.cardinality = cardinality;
        this.format = format;
        this.implicitTypeNames = implicitTypeNames;
    }

    public long getCacheKey() {
        return CodecBuilder.getCacheKey(query, cardinality, format);
    }
}
