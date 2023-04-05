module com.edgedb.core {
    exports com.edgedb;
    exports com.edgedb.datatypes;
    exports com.edgedb.clients;
    exports com.edgedb.exceptions;
    exports com.edgedb.util;
    exports com.edgedb.namingstrategies;
    exports com.edgedb.annotations;
    exports com.edgedb.state;

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

    exports com.edgedb.abstractions to com.edgedb.driver;

    opens com.edgedb;
}
