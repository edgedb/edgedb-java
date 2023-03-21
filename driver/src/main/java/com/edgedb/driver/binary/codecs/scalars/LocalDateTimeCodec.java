package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.util.TemporalUtils;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class LocalDateTimeCodec extends ScalarCodecBase<LocalDateTime> {
    public LocalDateTimeCodec() {
        super(LocalDateTime.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable LocalDateTime value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(ChronoUnit.MICROS.between(value, TemporalUtils.EDGEDB_EPOC));
        }
    }

    @Override
    public LocalDateTime deserialize(PacketReader reader, CodecContext context) {
        return TemporalUtils.EDGEDB_EPOC.plus(reader.readInt64(), ChronoUnit.MICROS).toLocalDateTime();
    }
}
