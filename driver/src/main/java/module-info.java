module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.datatypes;
    exports com.edgedb.driver.clients;
    exports com.edgedb.driver.exceptions;
    exports com.edgedb.driver.util;
    exports com.edgedb.driver.namingstrategies;
    exports com.edgedb.driver.annotations;
    exports com.edgedb.driver.state;

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

    opens com.edgedb.driver;

}
