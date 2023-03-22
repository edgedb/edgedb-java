package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.common.RangeFlags;
import com.edgedb.driver.datatypes.Range;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

import java.util.EnumSet;

import static com.edgedb.driver.util.BinaryProtocolUtils.INT_SIZE;

public final class RangeCodec<T extends Number> extends CodecBase<Range<T>> {
    private final Codec<T> innerCodec;

    public RangeCodec(Class<Range<T>> cls, Codec<T> innerCodec) {
        super(cls);
        this.innerCodec = innerCodec;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Range<T> value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
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
    public @Nullable Range<T> deserialize(PacketReader reader, CodecContext context) throws EdgeDBException {
        var flags = reader.readEnumSet(RangeFlags.class, Byte.TYPE, RangeFlags::valueOf);

        if(flags.contains(RangeFlags.EMPTY)) {
            return Range.empty();
        }

        T lowerBound = null, upperBound = null;

        if(flags.contains(RangeFlags.INFINITE_LOWER_BOUNDS)) {
            reader.skip(INT_SIZE);
            lowerBound = innerCodec.deserialize(reader, context);
        }

        if(flags.contains(RangeFlags.INFINITE_UPPER_BOUNDS)) {
            reader.skip(INT_SIZE);
            upperBound = innerCodec.deserialize(reader, context);
        }

        return new Range<>(
                lowerBound,
                upperBound,
                flags.contains(RangeFlags.INCLUDE_LOWER_BOUNDS),
                flags.contains(RangeFlags.INCLUDE_UPPER_BOUNDS)
        );
    }
}
