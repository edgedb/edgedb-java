package com.edgedb.driver.binary.protocol.v2.descriptors;

import com.edgedb.driver.binary.PacketReader;
import com.edgedb.driver.binary.codecs.Codec;
import com.edgedb.driver.binary.protocol.TypeDescriptor;
import com.edgedb.driver.binary.protocol.TypeDescriptorInfo;
import com.edgedb.driver.binary.protocol.common.descriptors.CodecMetadata;
import org.jetbrains.annotations.Nullable;
import org.joou.UShort;

import java.util.UUID;
import java.util.function.Function;

public class MultiRangeDescriptor implements TypeDescriptor, MetadataDescriptor {

    public final UUID id;
    public final String name;
    public final boolean isSchemaDefined;
    public final UShort[] ancestors;
    public final UShort type;

    public MultiRangeDescriptor(UUID id, PacketReader reader) {
        this.id = id;
        this.name = reader.readString();
        this.isSchemaDefined = reader.readBoolean();
        this.ancestors = reader.readArrayOf(UShort.class, PacketReader::readUInt16, UShort.class);
        this.type = reader.readUInt16();
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public @Nullable CodecMetadata getMetadata(Function<Integer, Codec<?>> getRelativeCodec, Function<Integer, TypeDescriptorInfo<?>> getRelativeDescriptor) {
        return new CodecMetadata(
                name,
                isSchemaDefined,
                MetadataDescriptor.constructAncestors(ancestors, getRelativeCodec, getRelativeDescriptor)
        );
    }
}
