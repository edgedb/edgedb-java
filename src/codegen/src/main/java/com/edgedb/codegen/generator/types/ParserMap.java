package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.driver.binary.codecs.Codec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ParserMap {
    @FunctionalInterface
    public interface CodecParser<T extends Codec<?>> {
        TypeName run(T codec, @Nullable GeneratorTargetInfo target, GeneratorContext context)
                throws Exception;
    }

    private final Map<Class<? extends Codec<?>>, CodecParser<?>> PARSER_MAP = new HashMap<>();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T extends Codec<?>> void define(Class<? extends Codec> cls, CodecParser<T> parser) {
        PARSER_MAP.put((Class<T>)cls, parser);
    }

    @SuppressWarnings("unchecked")
    public <T extends Codec<?>> @Nullable TypeName parse(T codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        var cls = codec.getClass();

        CodecParser<T> parser = null;

        if(PARSER_MAP.containsKey(cls)) {
            parser = (CodecParser<T>) PARSER_MAP.get(codec.getClass());
        } else {
            for(var entry : PARSER_MAP.entrySet()) {
                if(entry.getKey().isAssignableFrom(cls)) {
                    parser = (CodecParser<T>) entry.getValue();
                    break;
                }
            }

            if(parser == null) {
                context.generator.getLogger().warn("No parser found for type {}", cls);
                return null;
            }
        }


        try {
            return parser.run(codec, target, context);
        } catch (Exception e) {
            context.generator.getLogger().error("Failed to parse {}", codec, e);
            return null;
        }
    }
}
