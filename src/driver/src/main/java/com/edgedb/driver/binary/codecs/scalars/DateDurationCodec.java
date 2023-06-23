package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.exceptions.EdgeDBException;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.time.Period;

public final class DateDurationCodec extends ScalarCodecBase<Period> {
    public DateDurationCodec() {
        super(Period.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Period value, CodecContext context)
    throws OperationNotSupportedException, EdgeDBException {
        if(value != null) {
            writer.write(0L);
            writer.write(value.getDays());
            try {
                writer.write(Math.toIntExact(value.toTotalMonths()));
            } catch (ArithmeticException x) {
                throw new EdgeDBException("Value of total months cannot be greater than " + Integer.MAX_VALUE, x);
            }
        }
    }

    @Override
    public Period deserialize(@NotNull PacketReader reader, CodecContext context) {
        reader.skip(BinaryProtocolUtils.LONG_SIZE);
        var days = reader.readInt32();
        var months = reader.readInt32();

        return Period.of(0, months, days);
    }
}
