package com.edgedb.driver.binary.protocol.common.descriptors;

public final class CodecMetadata {
    public final String schemaName;
    public final boolean isSchemaDefined;
    public final CodecAncestor[] ancestors;

    public CodecMetadata(String schemaName, boolean isSchemaDefined) {
        this.schemaName = schemaName;
        this.isSchemaDefined = isSchemaDefined;
        this.ancestors = new CodecAncestor[0];
    }

    public CodecMetadata(String schemaName, boolean isSchemaDefined, CodecAncestor[] ancestors) {
        this.schemaName = schemaName;
        this.isSchemaDefined = isSchemaDefined;
        this.ancestors = ancestors;
    }
}
