package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.util.TemporalUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class LocalDateTimeCodec extends ScalarCodecBase<LocalDateTime> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000010B");

    public LocalDateTimeCodec() {
        super(ID, LocalDateTime.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable LocalDateTime value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(ChronoUnit.MICROS.between(TemporalUtils.EDGEDB_EPOC_LOCAL, value));
        }
    }

    @Override
    public @NotNull LocalDateTime deserialize(@NotNull PacketReader reader, CodecContext context) {
        return TemporalUtils.EDGEDB_EPOC_LOCAL.plus(reader.readInt64(), ChronoUnit.MICROS);
    }
}
