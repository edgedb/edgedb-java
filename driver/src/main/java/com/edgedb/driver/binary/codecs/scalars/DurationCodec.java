package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

public final class DurationCodec extends ScalarCodecBase<Duration> {
    public DurationCodec() {
        super(Duration.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Duration value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(Math.round(value.toNanos() / 1000d));
        }
    }

    @Override
    public @Nullable Duration deserialize(PacketReader reader, CodecContext context) {
        return Duration.of(reader.readInt64(), ChronoUnit.MICROS);
    }
}
