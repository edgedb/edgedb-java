package com.edgedb.codegen.generator.types;

import com.edgedb.codegen.generator.GeneratorContext;
import com.edgedb.codegen.generator.GeneratorTargetInfo;
import com.edgedb.driver.binary.codecs.Codec;
import com.squareup.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

public class V2TypeGenerator implements TypeGenerator{
    @Override
    public TypeName getType(Codec<?> codec, @Nullable GeneratorTargetInfo target, GeneratorContext context) {
        return null;
    }

    @Override
    public void postProcess(GeneratorContext context) {

    }
}
