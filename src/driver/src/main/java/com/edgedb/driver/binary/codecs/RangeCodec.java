package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.common.RangeFlags;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.EnumSet;
import java.util.UUID;

public final class RangeCodec<T> extends CodecBase<Range<T>> {
    private final Codec<T> innerCodec;

    @SuppressWarnings("unchecked")
    public RangeCodec(UUID id, @Nullable CodecMetadata metadata, Class<?> cls, Codec<?> innerCodec) {
        super(id, metadata, (Class<Range<T>>) cls);
        this.innerCodec = (Codec<T>) innerCodec;
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Range<T> value, CodecContext context) throws OperationNotSupportedException, GelException {
        if(value == null) {
            return;
        }

        EnumSet<RangeFlags> flags;

        if(value.isEmpty()) {
            flags = EnumSet.of(RangeFlags.EMPTY);
        } else {
            flags = EnumSet.noneOf(RangeFlags.class);

            if(value.doesIncludeLower()) {
                flags.add(RangeFlags.INCLUDE_LOWER_BOUNDS);
            }

            if(value.doesIncludeUpper()) {
                flags.add(RangeFlags.INCLUDE_UPPER_BOUNDS);
            }

            if(value.getLower() == null) {
                flags.add(RangeFlags.INFINITE_LOWER_BOUNDS);
            }

            if(value.getUpper() == null) {
                flags.add(RangeFlags.INFINITE_UPPER_BOUNDS);
            }
        }

        writer.writeEnumSet(flags, Byte.TYPE);

        if(value.getLower() != null) {
            writer.writeDelegateWithLength((v) -> innerCodec.serialize(v, value.getLower(), context));
        }

        if(value.getUpper() != null) {
            writer.writeDelegateWithLength((v) -> innerCodec.serialize(v, value.getUpper(), context));
        }
    }

    @Override
    public @Nullable Range<T> deserialize(@NotNull PacketReader reader, CodecContext context) throws GelException, OperationNotSupportedException {
        var flags = reader.readEnumSet(RangeFlags.class, Byte.TYPE);

        if(flags.contains(RangeFlags.EMPTY)) {
            return Range.empty();
        }

        T lowerBound = null, upperBound = null;

        if(!flags.contains(RangeFlags.INFINITE_LOWER_BOUNDS)) {
            try(var elementReader = reader.scopedSlice()) {
                lowerBound = innerCodec.deserialize(elementReader, context);
            }
        }

        if(!flags.contains(RangeFlags.INFINITE_UPPER_BOUNDS)) {
            try(var elementReader = reader.scopedSlice()) {
                upperBound = innerCodec.deserialize(elementReader, context);
            }
        }

        return Range.create(
                innerCodec.getConvertingClass(),
                lowerBound,
                upperBound,
                flags.contains(RangeFlags.INCLUDE_LOWER_BOUNDS),
                flags.contains(RangeFlags.INCLUDE_UPPER_BOUNDS));
    }
}
