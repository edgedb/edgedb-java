package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TextCodec extends ScalarCodecBase<String> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000101");

    public TextCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, String.class);
    }

    protected TextCodec(UUID id, @Nullable CodecMetadata metadata) {
        super(id, metadata, String.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable String value, CodecContext context) throws OperationNotSupportedException {
        if(value != null) {
            writer.writeArrayWithoutLength(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public @NotNull String deserialize(@NotNull PacketReader reader, CodecContext context) {
        return new String(reader.consumeByteArray(), StandardCharsets.UTF_8);
    }
}
