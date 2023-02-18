module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.clients;
    exports com.edgedb.driver.exceptions;

    requires org.jetbrains.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.naming;
    requires org.slf4j;
    //requires tls.channel;

    opens com.edgedb.driver;
    exports com.edgedb.driver.util;
    exports com.edgedb.driver.ssl;
}
