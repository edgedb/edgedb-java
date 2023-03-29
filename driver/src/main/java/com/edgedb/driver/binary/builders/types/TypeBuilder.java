package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.packets.receivable.Data;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;

import javax.naming.OperationNotSupportedException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TypeBuilder {
    private static final ConcurrentMap<Class<?>, TypeDeserializerInfo<?>> deserializerInfo;

    static {
        deserializerInfo = new ConcurrentHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static <T> T buildObject(EdgeDBBinaryClient client, Class<T> type, ObjectCodec codec, Data data) throws OperationNotSupportedException, EdgeDBException {
        if(!isValidObjectType(type)) {
            throw new OperationNotSupportedException("Cannot deserialize object data to " + type.getName());
        }

        var info = deserializerInfo.computeIfAbsent(type, TypeDeserializerInfo::new);

        codec.initialize(info);

        return (T)Codec.deserializeFromBuffer(codec, data.payloadBuffer, client.getCodecContext());
    }

    private static boolean isValidObjectType(Class<?> type) {
        if(deserializerInfo.containsKey(type)) {
            return true;
        }

        // TODO: check for valid strategy for construction
        return true;
    }

}
