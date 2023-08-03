module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.datatypes;
    exports com.edgedb.driver.exceptions;
    exports com.edgedb.driver.namingstrategies;
    exports com.edgedb.driver.annotations;
    exports com.edgedb.driver.state;

    exports com.edgedb.driver.binary to com.edgedb.codegen;
    exports com.edgedb.driver.binary.codecs to com.edgedb.codegen;
    exports com.edgedb.driver.clients to com.edgedb.codegen;
    exports com.edgedb.driver.binary.protocol to com.edgedb.codegen;
    exports com.edgedb.driver.binary.duplexers to com.edgedb.codegen;
    exports com.edgedb.driver.util to com.edgedb.codegen;
    exports com.edgedb.driver.binary.protocol.common to com.edgedb.codegen;
    exports com.edgedb.driver.binary.codecs.common to com.edgedb.codegen;
    exports com.edgedb.driver.binary.codecs.scalars to com.edgedb.codegen;
    exports com.edgedb.driver.binary.codecs.complex to com.edgedb.codegen;
    exports com.edgedb.driver.binary.protocol.common.descriptors to com.edgedb.codegen;
    exports com.edgedb.driver.binary.builders to com.edgedb.codegen;
    exports com.edgedb.driver.binary.builders.types to com.edgedb.codegen;


    requires org.jetbrains.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.naming;
    requires org.slf4j;
    requires io.netty.common;
    requires io.netty.transport;
    requires io.netty.codec;
    requires io.netty.buffer;
    requires io.netty.handler;
    requires org.jooq.joou;
    requires org.reflections;
    requires java.net.http;

    opens com.edgedb.driver;
}
