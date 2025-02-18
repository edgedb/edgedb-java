package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.exceptions.GelException;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Period;
import java.util.UUID;

public final class DateDurationCodec extends ScalarCodecBase<Period> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000112");
    public DateDurationCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Period.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Period value, CodecContext context)
    throws OperationNotSupportedException, GelException {
        if(value != null) {
            writer.write(0L);
            writer.write(value.getDays());
            try {
                writer.write(Math.toIntExact(value.toTotalMonths()));
            } catch (ArithmeticException x) {
                throw new GelException("Value of total months cannot be greater than " + Integer.MAX_VALUE, x);
            }
        }
    }

    @Override
    public Period deserialize(@NotNull PacketReader reader, CodecContext context) {
        reader.skip(BinaryProtocolUtils.LONG_SIZE);
        var days = reader.readInt32();
        var months = reader.readInt32();

        return Period.of(0, months, days);
    }
}
