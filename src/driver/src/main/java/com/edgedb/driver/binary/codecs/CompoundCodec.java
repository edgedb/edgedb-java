package com.edgedb.driver.binary.codecs;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.protocol.common.descriptors.TypeOperation;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.UUID;

public class CompoundCodec extends CodecBase<Object> {

    public CompoundCodec(
            UUID id,
            Class<Object> cls,
            TypeOperation operation,
            ) {
        super(id, cls);
    }

    @Override
    public void serialize(PacketWriter writer, @Nullable Object value, CodecContext context) throws OperationNotSupportedException, EdgeDBException {

    }

    @Nullable
    @Override
    public Object deserialize(PacketReader reader, CodecContext context) throws EdgeDBException, OperationNotSupportedException {
        return null;
    }
}
