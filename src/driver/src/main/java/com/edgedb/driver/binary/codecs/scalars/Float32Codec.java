package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class Float32Codec extends ScalarCodecBase<Float> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000106");
    public Float32Codec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Float.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Float value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Float deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readFloat();
    }
}
