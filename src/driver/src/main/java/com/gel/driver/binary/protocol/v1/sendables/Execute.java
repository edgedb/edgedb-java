package com.gel.driver.binary.protocol.v1.sendables;

import com.gel.driver.Capabilities;
import com.gel.driver.binary.PacketWriter;
import com.gel.driver.binary.protocol.ClientMessageType;
import com.gel.driver.binary.protocol.Sendable;
import com.gel.driver.binary.protocol.common.Cardinality;
import com.gel.driver.binary.protocol.common.CompilationFlags;
import com.gel.driver.binary.protocol.common.IOFormat;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.NotNull;

import javax.naming.OperationNotSupportedException;
import java.util.EnumSet;
import java.util.UUID;

import static com.gel.driver.util.BinaryProtocolUtils.*;

public class Execute extends Sendable {
    private final EnumSet<Capabilities> capabilities;
    private final EnumSet<CompilationFlags> compilationFlags;
    private final long implicitLimit;
    private final IOFormat format;
    private final Cardinality cardinality;
    private final String query;
    private final UUID stateTypeDescriptorId;
    private final ByteBuf stateData;
    private final UUID inputTypeDescriptorId;
    private final UUID outputTypeDescriptorId;
    private final ByteBuf parameterData;

    public Execute(
            EnumSet<Capabilities> capabilities,
            EnumSet<CompilationFlags> compilationFlags,
            long implicitLimit,
            IOFormat format,
            Cardinality cardinality,
            String query,
            UUID stateTypeDescriptorId,
            ByteBuf stateData,
            UUID inputTypeDescriptorId,
            UUID outputTypeDescriptorId,
            ByteBuf parameterData
    ) {
        super(ClientMessageType.EXECUTE);
        this.capabilities = capabilities;
        this.compilationFlags = compilationFlags;
        this.implicitLimit = implicitLimit;
        this.format = format;
        this.cardinality = cardinality;
        this.query = query;
        this.stateTypeDescriptorId = stateTypeDescriptorId;
        this.stateData = stateData;
        this.inputTypeDescriptorId = inputTypeDescriptorId;
        this.outputTypeDescriptorId = outputTypeDescriptorId;
        this.parameterData = parameterData;
    }

    @Override
    public int getDataSize() {
        return
                LONG_SIZE +
                LONG_SIZE +
                LONG_SIZE +
                SHORT_SIZE +
                BYTE_SIZE +
                BYTE_SIZE +
                UUID_SIZE +
                UUID_SIZE +
                UUID_SIZE +
                sizeOf(this.query) +
                sizeOf(this.stateData) +
                sizeOf(this.parameterData);

    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected void buildPacket(@NotNull PacketWriter writer) throws OperationNotSupportedException {
        if(query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        writer.write((short)0); // annotations (zero of them)

        writer.writeEnumSet(capabilities, Long.TYPE);
        writer.writeEnumSet(compilationFlags, Long.TYPE);
        writer.write(implicitLimit);
        writer.write(format);
        writer.write(cardinality);
        writer.write(query);

        writer.write(stateTypeDescriptorId);
        writer.writeArray(stateData);

        writer.write(inputTypeDescriptorId);
        writer.write(outputTypeDescriptorId);

        writer.writeArray(parameterData);
    }
}
