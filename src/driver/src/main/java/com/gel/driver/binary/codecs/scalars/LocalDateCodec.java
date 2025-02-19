package com.gel.driver.binary.codecs.scalars;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.util.TemporalUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class LocalDateCodec extends ScalarCodecBase<LocalDate> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000010C");
    public LocalDateCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, LocalDate.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable LocalDate value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            var days = ChronoUnit.DAYS.between(TemporalUtils.EDGEDB_EPOC_LOCAL.toLocalDate(), value);

            if(days > Integer.MAX_VALUE || days < Integer.MIN_VALUE) {
                throw new IllegalArgumentException(String.format("value exceeds the day range of %d..%d", Integer.MIN_VALUE, Integer.MAX_VALUE));
            }

            writer.write((int)days);
        }
    }

    @Override
    public LocalDate deserialize(@NotNull PacketReader reader, CodecContext context) {
        return TemporalUtils.EDGEDB_EPOC_LOCAL.plusDays(reader.readInt32()).toLocalDate();
    }
}
