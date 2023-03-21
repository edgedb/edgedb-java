package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Period;

import static com.edgedb.driver.util.BinaryProtocolUtils.LONG_SIZE;

public final class DateDurationCodec extends ScalarCodecBase<Period> {
    public DateDurationCodec() {
        super(Period.class);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Period value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(0L);
            writer.write(value.getDays());
            writer.write((int)value.toTotalMonths()); // TODO: friendly error when overflowed
        }
    }

    @Override
    public Period deserialize(PacketReader reader, CodecContext context) {
        reader.skip(LONG_SIZE);
        var days = reader.readInt32();
        var months = reader.readInt32();

        return Period.of(0, months, days);
    }
}
