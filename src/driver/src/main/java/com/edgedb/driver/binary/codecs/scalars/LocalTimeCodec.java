package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public final class LocalTimeCodec extends ScalarCodecBase<LocalTime> {
    public LocalTimeCodec() {
        super(LocalTime.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable LocalTime value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(ChronoUnit.MICROS.between(LocalTime.MIDNIGHT, value));
        }
    }

    @Override
    public @NotNull LocalTime deserialize(@NotNull PacketReader reader, CodecContext context) {
        return LocalTime.MIDNIGHT.plus(reader.readInt64(), ChronoUnit.MICROS);
    }
}
