module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.clients;
    exports com.edgedb.driver.exceptions;

    requires org.jetbrains.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.naming;
    requires org.slf4j;

    opens com.edgedb.driver;
}
