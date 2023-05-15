package com.edgedb.driver.binary.codecs.scalars.complex;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.codecs.complex.ComplexCodecConverter;
import com.edgedb.driver.util.TemporalUtils;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public final class DateTimeCodec extends ComplexScalarCodecBase<OffsetDateTime> {
    @SuppressWarnings("unchecked")
    public DateTimeCodec() {
        super(
                OffsetDateTime.class,
                new ComplexCodecConverter<>(
                        ZonedDateTime.class,
                        OffsetDateTime::toZonedDateTime,
                        ZonedDateTime::toOffsetDateTime
                )
        );
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable OffsetDateTime value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(TemporalUtils.toMicrosecondsSinceEpoc(value));
        }
    }

    @Override
    public @Nullable OffsetDateTime deserialize(PacketReader reader, CodecContext context) {
        return TemporalUtils.fromMicrosecondsSinceEpoc(reader.readInt64(), ZonedDateTime::toOffsetDateTime);
    }
}
