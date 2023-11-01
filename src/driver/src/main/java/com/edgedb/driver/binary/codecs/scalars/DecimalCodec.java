package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.util.StringsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joou.UShort;

import javax.naming.OperationNotSupportedException;
import java.math.BigDecimal;
import java.util.UUID;

import static org.joou.Unsigned.ushort;

public final class DecimalCodec extends ScalarCodecBase<BigDecimal> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000108");
    public DecimalCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, BigDecimal.class);
    }

    @Override
    public void serialize(
            @NotNull PacketWriter writer,
            @Nullable BigDecimal value,
            CodecContext context
    ) throws OperationNotSupportedException {
        if(value == null) {
            return;
        }

        var str = value.toPlainString();
        var spl = str.split("\\.");
        var integral = spl[0].charAt(0) == '-' ? spl[0].substring(1) : spl[0];
        var frac = spl.length > 1 ? spl[1] : "";

        var sDigits =
                StringsUtil.padLeft(integral, '0', (int)Math.ceil(integral.length() / 4d) * 4) +
                StringsUtil.padRight(frac, '0', (int)Math.ceil(frac.length() / 4d) * 4);

        UShort[] digits = new UShort[sDigits.length() / 4];

        for (int i = 0; i < sDigits.length(); i += 4) {
            digits[i / 4] = ushort(Integer.parseInt(sDigits.substring(i, i+4)));
        }

        var nDigits = ushort(digits.length);
        var weight = (short)(Math.ceil(integral.length() / 4d) - 1);
        var sign = ushort(value.signum() == -1 ? 0x4000 : 0x0000);
        var dScale = (short)frac.length();

        writer.write(nDigits);
        writer.write(weight);
        writer.write(sign);
        writer.write(dScale);

        for (var digit : digits) {
            writer.write(digit);
        }
    }

    @Override
    public @NotNull BigDecimal deserialize(@NotNull PacketReader reader, CodecContext context) {
        var numDigits = reader.readUInt16().intValue();
        var weight = reader.readInt16();
        var isPos = reader.readUInt16().compareTo(ushort(0)) == 0;
        var displayScale = reader.readUInt16().intValue();

        StringBuilder value = new StringBuilder(isPos ? "" : "-");

        int d;

        if(weight < 0) {
            d = weight + 1;
            value.append("0");
        } else {
            for(d = 0; d <= weight; d++) {
                var digit = d < numDigits ? reader.readUInt16() : 0;
                var sDigit = Integer.toString(digit.intValue());
                if(d > 0) {
                    sDigit = StringsUtil.padLeft(sDigit, '0', 4);
                }
                value.append(sDigit);
            }
        }

        if(displayScale > 0) {
            // seems like the code from `BigDecimal` doesn't use the locale decimal point, parsing to and from must
            // be done with a literal '.'.
            value.append('.');

            var end = value.length() + displayScale;
            for(int i = 0; i < displayScale; d++, i += 4) {
                var digit = d >= 0 && d < numDigits ? reader.readUInt16().intValue() : 0;
                value.append(StringsUtil.padLeft(Integer.toString(digit), '0', 4));
            }

            value.delete(end, value.length());
        }

        return new BigDecimal(value.toString());
    }
}
