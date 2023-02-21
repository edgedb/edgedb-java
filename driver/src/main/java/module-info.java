module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.clients;
    exports com.edgedb.driver.exceptions;

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
    //requires io.netty.tcnative.classes.openssl;
    //requires io.netty.handler.ssl.ocsp;

    opens com.edgedb.driver;

    exports com.edgedb.driver.util;
    exports com.edgedb.driver.ssl;
}
