package com.edgedb.binary.codecs.scalars.complex;

import com.edgedb.binary.PacketReader;
import com.edgedb.binary.PacketWriter;
import com.edgedb.binary.codecs.CodecContext;
import com.edgedb.binary.codecs.complex.ComplexCodecConverter;
import com.edgedb.datatypes.RelativeDuration;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Duration;
import java.time.Period;

public final class RelativeDurationCodec extends ComplexScalarCodecBase<RelativeDuration> {
    @SuppressWarnings("unchecked")
    public RelativeDurationCodec() {
        super(
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
    public void serialize(PacketWriter writer, @Nullable RelativeDuration value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value.getMicroseconds());
            writer.write(value.getDays());
            writer.write(value.getMonths());
        }
    }

    @Override
    public RelativeDuration deserialize(PacketReader reader, CodecContext context) {
        var micro = reader.readInt64();
        var days = reader.readInt32();
        var months = reader.readInt32();

        return new RelativeDuration(months, days, micro);
    }
}
