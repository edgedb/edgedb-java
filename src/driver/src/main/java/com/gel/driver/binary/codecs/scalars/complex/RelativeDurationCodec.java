package com.gel.driver.binary.codecs.scalars.complex;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.codecs.complex.ComplexCodecConverter;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.datatypes.RelativeDuration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Duration;
import java.time.Period;
import java.util.UUID;

public final class RelativeDurationCodec extends ComplexScalarCodecBase<RelativeDuration> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000111");

    @SuppressWarnings("unchecked")
    public RelativeDurationCodec(@Nullable CodecMetadata metadata) {
        super(
                ID,
                metadata,
                RelativeDuration.class,
                new ComplexCodecConverter<>(
                        Period.class,
                        RelativeDuration::toPeriod,
                        RelativeDuration::new
                ),
                new ComplexCodecConverter<>(
                        Duration.class,
                        RelativeDuration::toDuration,
                        RelativeDuration::new
                )
        );
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable RelativeDuration value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value.getMicroseconds());
            writer.write(value.getDays());
            writer.write(value.getMonths());
        }
    }

    @Override
    public @NotNull RelativeDuration deserialize(@NotNull PacketReader reader, CodecContext context) {
        var micro = reader.readInt64();
        var days = reader.readInt32();
        var months = reader.readInt32();

        return new RelativeDuration(months, days, micro);
    }
}
