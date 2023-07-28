package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.edgedb.driver.binary.protocol.common.descriptors.TypeOperation;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public class CompoundCodec extends CodecBase<Object> {
    private final TypeOperation operation;
    private final Codec<?>[] elements;

    public CompoundCodec(
            UUID id,
            @Nullable CodecMetadata metadata,
            TypeOperation operation,
            Codec<?>[] elements
    ) {
        super(id, metadata, Object.class);

        this.operation = operation;
        this.elements = elements;
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {
        throw new OperationNotSupportedException("No data wire format has been defined for this type of codec");
    }

    @Nullable
    @Override
    public Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        throw new OperationNotSupportedException("No data wire format has been defined for this type of codec");
    }
}
