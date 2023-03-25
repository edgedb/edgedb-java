package com.edgedb.driver.binary.builders;

import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.codecs.visitors.TypeVisitor;
import com.edgedb.driver.binary.packets.receivable.Data;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;

public final class ObjectBuilder {
    public static <T> T buildResult(EdgeDBBinaryClient client, Codec<?> codec, Data data, Class<T> cls) throws EdgeDBException, OperationNotSupportedException {
        var visitor = new TypeVisitor(client);
        visitor.setTargetType(cls);
        codec = visitor.visit(codec);

        if(codec instanceof ObjectCodec) {
            // TODO: type builder
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
