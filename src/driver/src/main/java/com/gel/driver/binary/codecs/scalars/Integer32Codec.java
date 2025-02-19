package com.gel.driver.binary.codecs.scalars;

import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.codecs.CodecContext;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public final class Integer32Codec extends ScalarCodecBase<Integer> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000104");

    public Integer32Codec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Integer.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Integer value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.write(value);
        }
    }

    @Override
    public @NotNull Integer deserialize(@NotNull PacketReader reader, CodecContext context) {
        return reader.readInt32();
    }
}
