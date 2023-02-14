module com.edgedb.driver {
    exports com.edgedb.driver;
    exports com.edgedb.driver.clients;

    requires org.jetbrains.annotations;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires java.naming;
    requires org.slf4j;
}
