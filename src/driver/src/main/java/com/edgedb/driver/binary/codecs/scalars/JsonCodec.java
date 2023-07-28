package com.edgedb.driver.binary.codecs.scalars;

import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.CodecContext;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.datatypes.Json;
import com.edgedb.driver.util.BinaryProtocolUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class JsonCodec extends ScalarCodecBase<Json> {
    public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-00000000010F");
    private static final byte JSON_FORMAT = (byte)0x01;

    public JsonCodec(@Nullable CodecMetadata metadata) {
        super(ID, metadata, Json.class);
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Json value, CodecContext context) throws OperationNotSupportedException {
        var data = (value != null ? value.getValue() : "").getBytes(StandardCharsets.UTF_8);

        writer.write(JSON_FORMAT);
        writer.writeArrayWithoutLength(data);
    }

    @Override
    public @Nullable Json deserialize(@NotNull PacketReader reader, CodecContext context) {
        reader.skip(BinaryProtocolUtils.BYTE_SIZE);

        var data = reader.consumeByteArray();

        if(data.length == 0) {
            return null;
        }

        return new Json(new String(data, StandardCharsets.UTF_8));
    }
}
