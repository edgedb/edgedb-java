package com.edgedb.driver.binary.packets.sendables;

import com.edgedb.driver.Capabilities;
import com.edgedb.driver.binary.PacketWriter;
import com.edgedb.driver.binary.packets.shared.Cardinality;
import com.edgedb.driver.binary.packets.shared.CompilationFlags;
import com.edgedb.driver.binary.packets.shared.IOFormat;
import com.edgedb.driver.binary.packets.ClientMessageType;
import com.edgedb.driver.util.BinaryProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;
import java.util.EnumSet;
import java.util.UUID;

public class Parse extends Sendable {
    private final EnumSet<Capabilities> capabilities;
    private final EnumSet<CompilationFlags> compilationFlags;
    private final IOFormat format;
    private final Cardinality cardinality;
    private final String query;
    private final long implicitLimit;
    private final UUID stateTypeDescriptorId;
    private final ByteBuf stateData;

    public Parse(
            EnumSet<Capabilities> capabilities,
            EnumSet<CompilationFlags> compilationFlags,
            IOFormat format,
            Cardinality cardinality,
            String query,
            long implicitLimit,
            UUID stateTypeDescriptorId,
            ByteBuf stateData
    ) {
        super(ClientMessageType.PARSE);
        this.capabilities = capabilities;
        this.compilationFlags = compilationFlags;
        this.format = format;
        this.cardinality = cardinality;
        this.query = query;
        this.implicitLimit = implicitLimit;
        this.stateTypeDescriptorId = stateTypeDescriptorId;
        this.stateData = stateData;
    }

    @Override
    public int getDataSize() {
        return
                BinaryProtocolUtils.SHORT_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.LONG_SIZE +
                BinaryProtocolUtils.BYTE_SIZE +
                BinaryProtocolUtils.BYTE_SIZE +
                BinaryProtocolUtils.sizeOf(query) +
                BinaryProtocolUtils.UUID_SIZE +
                BinaryProtocolUtils.sizeOf(stateData);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        if(query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        writer.write((short) 0); // annotations (zero of them)

        writer.writeEnumSet(capabilities, Long.TYPE);
        writer.writeEnumSet(compilationFlags, Long.TYPE);
        writer.write(implicitLimit);
        writer.write(format);
        writer.write(cardinality);
        writer.write(query);

        writer.write(stateTypeDescriptorId);
        writer.writeArray(stateData);
    }
}
