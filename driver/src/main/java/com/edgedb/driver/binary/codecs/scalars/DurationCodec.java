package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static com.edgedb.driver.util.BinaryProtocolUtils.LONG_SIZE;

public final class DurationCodec extends ScalarCodecBase<Duration> {
    public DurationCodec() {
        super(Duration.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Duration value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(Math.round(value.toNanos() / 1000d));

            // deprecated: days & months
            writer.write(0);
            writer.write(0);
        }
    }

    @Override
    public @Nullable Duration deserialize(PacketReader reader, CodecContext context) {
        var duration = Duration.of(reader.readInt64(), ChronoUnit.MICROS);

        // deprecated: days & months
        reader.skip(LONG_SIZE);

        return duration;
    }
}
