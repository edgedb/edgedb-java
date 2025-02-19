package com.gel.driver.binary.codecs.scalars;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class Float64Codec extends ScalarCodecBase<Double> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000107");
    public Float64Codec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Double.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Double value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Double deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readDouble();
    }
}
