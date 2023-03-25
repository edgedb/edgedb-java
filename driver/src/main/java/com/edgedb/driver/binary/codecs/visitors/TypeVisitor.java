package com.edgedb.driver.binary.codecs.visitors;

import com.edgedb.driver.binary.codecs.*;
import com.edgedb.driver.clients.EdgeDBBinaryClient;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Supplier;

@SuppressWarnings("rawtypes")
public final class TypeVisitor implements CodecVisitor {
    private static final Map<Class<? extends Codec>, TypeCodecVisitor> visitors;
    private final Stack<TypeResultContextFrame> frames;
    private final FrameHandle handle;
    private final EdgeDBBinaryClient client;

    static {
        visitors = new HashMap<>() {
            {
                put(ObjectCodec.class,     (v, c) -> visitObjectCodec(v, (ObjectCodec) c));
                put(CompilableCodec.class, (v, c) -> visitCompilableCodec(v, (CompilableCodec) c));
                put(ComplexCodec.class,    (v, c) -> visitComplexCodec(v, (ComplexCodec<?>) c));
                put(RuntimeCodec.class,    (v, c) -> visitRuntimeCodec(v, (RuntimeCodec<?>) c));
            }
        };
    }
    public TypeVisitor(EdgeDBBinaryClient client) {
        this.frames = new Stack<>();
        this.client = client;
        this.handle = new FrameHandle(this.frames::pop);
    }

    public void setTargetType(Class<?> type) {
        this.frames.push(new TypeResultContextFrame(type));
    }

    public void reset(){
        this.frames.clear();
    }

    @Override
    public Codec<?> visit(Codec<?> codec) {
        if (getContext().type.equals(Void.class)) {
            return codec;
        }

        if(visitors.containsKey(codec.getClass())) {
            var visitor = visitors.get(codec.getClass());
            return visitor.visit(this, codec);
        }

        return codec;
    }

    public static Codec<?> visitObjectCodec(TypeVisitor visitor, ObjectCodec codec) {
        return codec;
    }

    public static Codec<?> visitCompilableCodec(TypeVisitor visitor, CompilableCodec codec) {
        // visit the inner codec
        Codec compiledCodec;
        try(var handle = visitor.enterNewContext(codec.getInnerType())) {
            compiledCodec = codec.compile(visitor.getContext().type, visitor.visit(codec.getInnerCodec()));
        }

        return visitor.visit(compiledCodec);
    }

    public static Codec<?> visitComplexCodec(TypeVisitor visitor, ComplexCodec<?> codec) {
        return codec.getCodecFor(visitor.getContext().type);
    }

    public static Codec<?> visitRuntimeCodec(TypeVisitor visitor, RuntimeCodec<?> codec) {
        if(!visitor.getContext().type.equals(codec.getConvertingClass())) {
            return codec.getBroker().getCodecFor(visitor.getContext().type);
        }
        return codec;
    }

    private TypeResultContextFrame getContext() {
        return this.frames.peek();
    }

    private FrameHandle enterNewContext(
            Class<?> type
    ) {
        frames.push(new TypeResultContextFrame(type));
        return this.handle;
    }

    private static final class TypeResultContextFrame {
        public final Class<?> type;

        private TypeResultContextFrame(Class<?> type) {
            this.type = type;
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
