module com.gel.driver {
    exports com.gel.driver;
    exports com.gel.driver.datatypes;
    exports com.gel.driver.exceptions;
    exports com.gel.driver.namingstrategies;
    exports com.gel.driver.annotations;
    exports com.gel.driver.state;

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

    opens com.gel.driver;
}
