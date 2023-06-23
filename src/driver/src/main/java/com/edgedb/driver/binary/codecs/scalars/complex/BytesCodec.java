package com.edgedb.driver.binary.codecs.scalars.complex;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.codecs.scalars.ScalarCodecBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;

public final class BytesCodec extends ScalarCodecBase<Byte[]> {
    public BytesCodec() {
        super(Byte[].class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, Byte @Nullable [] value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.writeArrayWithoutLength(toPrimitive(value));
        }
    }

    @Override
    public Byte @Nullable [] deserialize(@NotNull PacketReader reader, CodecContext context) {
        return toObject(reader.consumeByteArray());
    }

    private static byte[] toPrimitive(Byte @NotNull [] arr){
        var prim = new byte[arr.length];
        for(int i = 0; i != arr.length; i++) {
            if(arr[i] == null) {
                throw new NullPointerException("Byte inside of Byte[] cannot be null!");
            }

            prim[i] = arr[i];
        }
        return prim;
    }

    private static Byte @NotNull [] toObject(byte @NotNull [] prim) {
        var obj = new Byte[prim.length];
        for(int i = 0; i != prim.length; i++) {
            obj[i] = prim[i];
        }
        return obj;
    }
}
