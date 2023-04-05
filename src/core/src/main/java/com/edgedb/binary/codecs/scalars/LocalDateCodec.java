package com.edgedb.binary.codecs.scalars;

import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.PacketReader;
import com.edgedb.binary.codecs.CodecContext;
import com.edgedb.util.TemporalUtils;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class LocalDateCodec extends ScalarCodecBase<LocalDate> {
    public LocalDateCodec() {
        super(LocalDate.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable LocalDate value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(ChronoUnit.DAYS.between(value, TemporalUtils.EDGEDB_EPOC));
        }
    }

    @Override
    public LocalDate deserialize(PacketReader reader, CodecContext context) {
        return TemporalUtils.EDGEDB_EPOC.plusDays(reader.readInt32()).toLocalDate();
    }
}
