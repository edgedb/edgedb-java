package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.util.StringsUtil;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UShort;

import javax.naming.OperationNotSupportedException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.joou.Unsigned.uint;
import static org.joou.Unsigned.ushort;

public final class BigIntCodec extends ScalarCodecBase<BigInteger> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000110");
    private static final BigInteger BASE = new BigInteger("10000");

    public BigIntCodec() {
        super(ID, BigInteger.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable BigInteger value, CodecContext context) throws OperationNotSupportedException {
        if(value == null) {
            return;
        }

        if(value.compareTo(BigInteger.ZERO) == 0) {
            writer.write(uint(0)); // nDigits & weight
            writer.write(ushort(0x0000)); // pos
            writer.write(ushort(0)); // reserved
            return;
        }

        var isPos = value.signum() == 1;
        var abs = value.abs();

        List<UShort> digits = new ArrayList<>();

        while(abs.compareTo(BigInteger.ZERO) != 0) {
            var mod = abs.mod(BASE);
            abs = abs.divide(BASE);
            digits.add((ushort(mod.intValue())));
        }

        writer.write(ushort(digits.size()));
        writer.write((short) (digits.size() - 1));
        writer.write(ushort(isPos ? 0x0000 : 0x4000));
        writer.write(ushort(0));

        for(int i = digits.size() - 1; i >= 0; i--) {
            writer.write(digits.get(i));
        }
    }

    @Override
    public @NotNull BigInteger deserialize(@NotNull PacketReader reader, CodecContext context) {
        var nDigits = reader.readUInt16().intValue();
        var weight = reader.readInt16();
        var isPos = reader.readUInt16().compareTo(ushort(0)) == 0;

        // reserved
        reader.skip(BinaryProtocolUtils.SHORT_SIZE);

        StringBuilder result = new StringBuilder(isPos ? "" : "-");

        int i = weight, d = 0;

        while(i >= 0) {
            if(i <= weight && d < nDigits) {
                var digit = Integer.toString(reader.readUInt16().intValue());
                result.append(d > 0 ? StringsUtil.padLeft(digit, '0', 4) : digit);
                d++;
            } else {
                result.append("0000");
            }
            i--;
        }

        return new BigInteger(result.toString());
    }
}
