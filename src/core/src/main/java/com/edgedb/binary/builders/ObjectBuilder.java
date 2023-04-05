package com.edgedb.binary.builders;

import com.edgedb.binary.builders.types.TypeBuilder;
import com.edgedb.binary.codecs.Codec;
import com.edgedb.binary.codecs.ObjectCodec;
import com.edgedb.binary.codecs.visitors.TypeVisitor;
import com.edgedb.binary.packets.receivable.Data;
import com.edgedb.clients.EdgeDBBinaryClient;
import com.edgedb.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;

public final class ObjectBuilder {
    public static <T> T buildResult(EdgeDBBinaryClient client, Codec<?> codec, Data data, Class<T> cls) throws EdgeDBException, OperationNotSupportedException {
        var visitor = new TypeVisitor(client);
        visitor.setTargetType(cls);
        codec = visitor.visit(codec);

        if(codec instanceof ObjectCodec) {
            // TODO: type builder
            return TypeBuilder.buildObject(client, cls, (ObjectCodec)codec, data);
        }

        var value = Codec.deserializeFromBuffer(codec, data.payloadBuffer, client.getCodecContext());
        return convertTo(cls, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convertTo(Class<T> cls, Object value) throws EdgeDBException {
        try {
            if(value == null) {
                return null;
            }

            var valueType = value.getClass();

            if(cls.isAssignableFrom(valueType)) {
                return (T) value;
            }

            if(cls.isEnum() && value instanceof String) {
                return (T)cls.getMethod("valueOf", String.class).invoke(null, (String) value);
            }

            return (T)value;
        }
        catch (Exception x) {
            throw new EdgeDBException("Failed to convert type to specified result", x);
        }
    }


}
