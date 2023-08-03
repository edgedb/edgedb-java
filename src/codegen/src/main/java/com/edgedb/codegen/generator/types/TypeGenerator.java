package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.driver.binary.codecs.Codec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface TypeGenerator {
    TypeName getType(Codec<?> codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) throws IOException;

    void postProcess(GeneratorContext context);


}
