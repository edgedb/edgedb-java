package com.gel.driver.binary.codecs;

import com.gel.driver.binary.PacketReader;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.common.descriptors.CodecMetadata;
import com.gel.driver.exceptions.GelException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.naming.OperationNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings({"rawtypes", "unchecked"})
public final class SparseObjectCodec extends CodecBase<Map<String, ?>> {
    private final Codec[] innerCodecs;
    private final @NotNull Map<String, Integer> propertyNamesMap;
    private final String @NotNull [] propertyNames;

    public SparseObjectCodec(UUID id, @Nullable CodecMetadata metadata, Codec[] innerCodecs, String @NotNull [] propertyNames) {
        super(id, metadata, (Class<Map<String,?>>) Map.of().getClass());
        this.innerCodecs = innerCodecs;

        this.propertyNames = propertyNames;
        this.propertyNamesMap = IntStream.range(0, propertyNames.length)
                .mapToObj((i) -> Map.entry(propertyNames[i], i))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public void serialize(@NotNull PacketWriter writer, @Nullable Map<String, ?> value, @NotNull CodecContext context) throws OperationNotSupportedException, GelException {
        if(value == null || value.isEmpty()) {
            writer.write(0);
            return;
        }

        writer.write(value.size());

        var visitor = context.getTypeVisitor();

        for(var element : value.entrySet()) {
            if(!propertyNamesMap.containsKey(element.getKey())) {
                continue;
            }

            var index = propertyNamesMap.get(element.getKey());

            writer.write(index);

            if(element.getValue() == null) {
                writer.write(-1);
                continue;
            }

            visitor.setTargetType(element.getValue().getClass());
            var codec = (Codec)visitor.visit(innerCodecs[index]);
            visitor.reset();

            writer.writeDelegateWithLength(v -> codec.serialize(v, element.getValue(), context));
        }
    }

    @Override
    public @NotNull Map<String, ?> deserialize(@NotNull PacketReader reader, CodecContext context) throws GelException, OperationNotSupportedException {
        var numElements = reader.readInt32();

        if(innerCodecs.length != numElements) {
            throw new GelException(String.format("Codecs mismatch for sparse object: expected %d codecs, got %d", innerCodecs.length, numElements));
        }

        var map = new HashMap<String, Object>(numElements);

        for(int i = 0; i != numElements; i++) {
            var index = reader.readInt32();
            var elementName = this.propertyNames[index];

            try(var elementReader = reader.scopedSlice()) {
                if(elementReader.isNoData) {
                    map.put(elementName, null);
                    continue;
                }

                map.put(elementName, innerCodecs[i].deserialize(elementReader, context));
            }
        }

        return map;
    }
}
