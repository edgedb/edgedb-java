package com.edgedb.driver.binary.codecs.visitors;

import com.edgedb.driver.binary.builders.types.TypeDeserializerInfo;
import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.clients.GelBinaryClient;
import com.edgedb.driver.exceptions.GelException;
import com.edgedb.driver.util.TypeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("rawtypes")
public final class TypeVisitor implements CodecVisitor {
    private final @NotNull Stack<TypeResultContextFrame> frames;
    private final @NotNull FrameHandle handle;
    private final GelBinaryClient client;

    public TypeVisitor(GelBinaryClient client) {
        this.frames = new Stack<>();
        this.client = client;
        this.handle = new FrameHandle(this.frames::pop);
    }

    public void setTargetType(Class<?> type) {
        this.frames.push(new TypeResultContextFrame(type, false));
    }

    public void reset(){
        this.frames.clear();
    }

    @Override
    public Codec<?> visit(@NotNull Codec<?> codec) throws GelException {
        if (getContext().type.equals(Void.class)) {
            return codec;
        }

        var type = codec.getClass();

        if(TupleCodec.class.isAssignableFrom(type)) {
            return visitTupleCodec(this, (TupleCodec) codec);
        } else if(ObjectCodec.class.isAssignableFrom(type)) {
            return visitObjectCodec(this, (ObjectCodec) codec);
        } else if (CompilableCodec.class.isAssignableFrom(type)) {
            return visitCompilableCodec(this, (CompilableCodec) codec);
        } else if (ComplexCodec.class.isAssignableFrom(type)) {
            return visitComplexCodec(this, (ComplexCodec<?>) codec);
        } else if (RuntimeCodec.class.isAssignableFrom(type)) {
            return visitRuntimeCodec(this, (RuntimeCodec<?>) codec);
        }

        return codec;
    }

    private static @NotNull Codec<?> visitTupleCodec(@NotNull TypeVisitor visitor, @NotNull TupleCodec codec) throws GelException {
        for(int i = 0; i != codec.innerCodecs.length; i++) {
            var innerCodec = codec.innerCodecs[i];
            try(var ignored = visitor.enterNewContext(c -> {
                Class<?> type;

                if(c.isDynamicType() || c.isRealType) {
                    type = c.type;
                } else if (innerCodec instanceof CompilableCodec) {
                    type = ((CompilableCodec)innerCodec).getCompilableType();
                } else {
                    type = innerCodec.getConvertingClass();
                }

                c.type = type;
            })) {
                codec.innerCodecs[i] = visitor.visit(innerCodec);
            }
        }

        return codec;
    }

    public static @NotNull Codec<?> visitObjectCodec(@NotNull TypeVisitor visitor, ObjectCodec codec) throws GelException {
        ObjectCodec.TypeInitializedObjectCodec typeCodec;

        if(codec instanceof ObjectCodec.TypeInitializedObjectCodec) {
            typeCodec = (ObjectCodec.TypeInitializedObjectCodec)codec;

            if(!typeCodec.getTarget().equals(visitor.getContext().type)) {
                typeCodec = typeCodec.getParent().getOrCreateTypeCodec(visitor.getContext().type);
            }
        } else {
            typeCodec = codec.getOrCreateTypeCodec(visitor.getContext().type);
        }

        if(typeCodec.getDeserializer() == null) {
            throw new GelException("Could not find a valid deserialization strategy for " + visitor.getContext().type);
        }

        var map = typeCodec.getDeserializer().getFieldMap(visitor.client.getConfig().getNamingStrategy());

        for(int i = 0; i != typeCodec.elements.length; i++) {
            var element = typeCodec.elements[i];

            TypeDeserializerInfo.FieldInfo field = null;
            Class<?> type;

            if(map.contains(element.name) && (field = map.get(element.name)) != null) {
                type = field.getType(element.cardinality);
            }
            else {
                type = element.codec instanceof CompilableCodec
                        ? ((CompilableCodec)element.codec).getInnerType()
                        : element.codec.getConvertingClass();
            }

            final var isReal = field != null;

            try(var ignored = visitor.enterNewContext(v -> {
                v.type = type;
                v.isRealType = isReal;
            })) {
                element.codec = visitor.visit(element.codec);
            }
        }

        return typeCodec;
    }

    public static Codec<?> visitCompilableCodec(@NotNull TypeVisitor visitor, @NotNull CompilableCodec codec) throws GelException {
        Codec innerCodec;

        // context type control:
        // inner codec:
        try(var ignored = visitor.enterNewContext(v -> {
            if(v.isRealType) {
                return;
            }

            var innerType = TypeUtils.tryPullWrappingType(visitor.getContext().type);

            if(innerType == null) {
                innerType = codec.getInnerType();
            }

            v.type = visitor.getContext().isDynamicType()
                    ? visitor.getContext().type
                    : innerType;
        })) {
            innerCodec = visitor.visit(codec.getInnerCodec());
        }

        return visitor.visit(codec.compile(visitor.getContext().type, innerCodec));
    }

    public static Codec<?> visitComplexCodec(@NotNull TypeVisitor visitor, @NotNull ComplexCodec<?> codec) {
        codec.buildRuntimeCodecs();

        if(visitor.getContext().isDynamicType())
            return codec;

        return codec.getCodecFor(visitor.getContext().type);
    }

    public static Codec<?> visitRuntimeCodec(@NotNull TypeVisitor visitor, @NotNull RuntimeCodec<?> codec) {
        if(!visitor.getContext().type.equals(codec.getConvertingClass())) {
            return codec.getBroker().getCodecFor(visitor.getContext().type);
        }
        return codec;
    }

    private TypeResultContextFrame getContext() {
        return this.frames.peek();
    }

    private @NotNull FrameHandle enterNewContext(
            @NotNull Consumer<TypeResultContextFrame> func
    ) {
        var ctx = frames.empty() ? new TypeResultContextFrame(null, false) : frames.peek().clone();
        func.accept(ctx);
        frames.push(ctx);
        return this.handle;
    }

    private static final class TypeResultContextFrame implements Cloneable {
        public Class<?> type;
        public boolean isRealType;

        public boolean isDynamicType() {
            return type.equals(Object.class);
        }

        private TypeResultContextFrame(Class<?> type, boolean isRealType) {
            this.type = type;
            this.isRealType = isRealType;
        }

        public TypeResultContextFrame clone() {
            try {
                return (TypeResultContextFrame)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final class FrameHandle implements Closeable {
        private final Supplier<?> free;

        public FrameHandle(Supplier<?> free) {
            this.free = free;
        }

        @Override
        public void close() {
            this.free.get();
        }
    }

    @FunctionalInterface
    private interface TypeCodecVisitor {
        Codec<?> visit(TypeVisitor visitor, Codec<?> codec);
    }
}
