package com.edgedb.driver.binary.builders.types;

import com.edgedb.driver.annotations.EdgeDBType;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.codecs.ObjectCodec;
import com.edgedb.driver.binary.packets.receivable.Data;
import com.edgedb.driver.clients.EdgeDBBinaryClient;
import com.edgedb.driver.exceptions.EdgeDBException;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TypeBuilder {
    private static final ConcurrentMap<Class<?>, TypeDeserializerInfo<?>> deserializerInfo;


    static {
        deserializerInfo = new ConcurrentHashMap<>() {{
            put(Map.class, new TypeDeserializerInfo<>(Map.class, (e, v) -> e.flatten()));
            put(Object.class, new TypeDeserializerInfo<>(Object.class, (e, v) -> e.flatten()));
        }};
    }

    @SuppressWarnings("unchecked")
    public static <T> T buildObject(EdgeDBBinaryClient client, Class<T> type, ObjectCodec codec, Data data) throws OperationNotSupportedException, EdgeDBException {
        var info = getDeserializerInfo(type);

        if(info == null) {
            throw new OperationNotSupportedException("Cannot deserialize object data to " + type.getName());
        }

        if(!(codec instanceof ObjectCodec.TypeInitializedObjectCodec)) {
            codec = codec.getOrCreateTypeCodec(info);
        }


        return (T) Codec.deserializeFromBuffer(codec, data.payloadBuffer, client.getCodecContext());
    }

    @SuppressWarnings("unchecked")
    public static <T> @Nullable TypeDeserializerInfo<T> getDeserializerInfo(Class<T> cls) {
        if(!isValidObjectType(cls)) {
            return null;
        }

        var info = (TypeDeserializerInfo<T>) deserializerInfo.computeIfAbsent(cls, TypeDeserializerInfo::new);

        info.scanChildren();;

        return info;
    }

    public static boolean requiredImplicitTypeNames(Class<?> cls) {
        var info = getDeserializerInfo(cls);
        return info != null && info.requiresTypeNameIntrospection();
    }

    private static boolean isValidObjectType(Class<?> type) {
        if(deserializerInfo.containsKey(type)) {
            return true;
        }

        // TODO: make this smarter?
        return type.getAnnotation(EdgeDBType.class) != null;
    }
}
